package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web de la gestió de clients (RF20-RF26).
 *
 * El fan servir l'agent ({@code /web/agent/usuaris}) i l'administrador
 * ({@code /web/admin/usuaris}): la pantalla és la mateixa per als dos.
 *
 * @author Equip4
 */
@Controller
@RequestMapping({"/web/agent/usuaris", "/web/admin/usuaris"})
public class GestioClientsController {

    @Autowired
    private UsuarisLogic usuarisService;

    /**
     * Llista els clients i permet filtrar-los per email o per diversos
     * criteris alhora (RF23, RF25, RF26).
     *
     * @param email part de l'email
     * @param nacionalitat part de la nacionalitat
     * @param poblacio part de la població
     * @param dni part del DNI
     * @param nom part del nom
     * @param telefon part del telèfon
     * @param model model per passar les dades a la vista
     * @return plantilla "usuaris-list"
     */
    @GetMapping("/all")
    public String findAll(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nacionalitat,
            @RequestParam(required = false) String poblacio,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String telefon,
            Model model) {

        List<Usuaris> llista;

        // Si ve una cerca directa per email, busquem per email
        if (email != null && !email.trim().isEmpty()) {
            llista = usuarisService.findByEmail(email.trim());
        } else {
            // Si no, executem la consulta multi-filtre
            llista = usuarisService.filtrarClients(email, nacionalitat, poblacio, dni, nom, telefon);
        }

        // Filtrem per mantenir només els usuaris amb rol CLIENT
        List<Usuaris> nomesClients = llista.stream()
                .filter(u -> u.getRol() == rol.CLIENT)
                .toList();

        model.addAttribute("usuaris", nomesClients);
        model.addAttribute("emailFiltre", email);

        return "usuaris-list";
    }

    /**
     * Mostra el formulari buit per donar d'alta un client (RF20).
     *
     * @param model model per passar les dades a la vista
     * @return plantilla "edit-usuari"
     */
    @GetMapping("/new")
    public String getForm(Model model) {
        Usuaris u = new Usuaris();
        u.setEstatRegistre("PENDENT");
        u.setPremium(false);

        model.addAttribute("usuari", u);
        return "edit-usuari";
    }

    /**
     * Mostra el formulari per modificar un client (RF22).
     *
     * @param usuariId identificador del client
     * @param model model per passar les dades a la vista
     * @param session sessió HTTP (per saber la ruta base)
     * @return plantilla "edit-usuari" o la llista si no existeix
     */
    @GetMapping("/edit/{id}")
    public String editById(@PathVariable("id") Long usuariId, Model model, HttpSession session) {
        Usuaris usuari = usuarisService.getUsuariById(usuariId);

        if (usuari == null) {
            return "redirect:" + RutaBase.obtenir(session) + "/usuaris/all";
        }

        model.addAttribute("usuari", usuari);
        return "edit-usuari";
    }

    /**
     * Mostra tots els detalls d'un client en mode lectura (RF24).
     *
     * @param usuariId identificador del client
     * @param model model per passar les dades a la vista
     * @param session sessió HTTP (per saber la ruta base)
     * @return plantilla "edit-usuari" en mode lectura o la llista si no existeix
     */
    @GetMapping("/view/{id}")
    public String viewById(@PathVariable("id") Long usuariId, Model model, HttpSession session) {
        Usuaris usuari = usuarisService.getUsuariById(usuariId);

        if (usuari == null) {
            return "redirect:" + RutaBase.obtenir(session) + "/usuaris/all";
        }

        model.addAttribute("usuari", usuari);
        model.addAttribute("modoLectura", true);
        return "edit-usuari";
    }

    /**
     * Desa un client nou o les modificacions d'un client existent.
     *
     * @param usuari dades del client enviades des del formulari
     * @param model model per tornar a mostrar el formulari si hi ha errors
     * @param session sessió HTTP (per saber la ruta base)
     * @return redirecció a la llista o el formulari amb el missatge d'error
     */
    @PostMapping("/save")
    public String save(@ModelAttribute("usuari") Usuaris usuari, Model model, HttpSession session) {
        try {
            if (usuari.getId() != null) {
                usuarisService.actualitzarClientPerAgent(usuari);
            } else {
                usuarisService.saveUsuari(usuari);
            }
            return "redirect:" + RutaBase.obtenir(session) + "/usuaris/all";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuari", usuari);
            return "edit-usuari";
        }
    }

    /**
     * Esborra un client (RF21). No es pot si té reserves associades.
     *
     * @param usuariId identificador del client
     * @param session sessió HTTP (per saber la ruta base)
     * @param redirectAttributes missatge d'èxit o d'error per a la llista
     * @return redirecció a la llista de clients
     */
    @GetMapping("/delete/{id}")
    public String deleteById(@PathVariable("id") Long usuariId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            usuarisService.deleteUsuariById(usuariId);
            redirectAttributes.addFlashAttribute("missatge", "Client esborrat correctament.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + RutaBase.obtenir(session) + "/usuaris/all";
    }
}
