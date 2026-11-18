package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.logic.FitxersLogic;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador web de les pantalles públiques dels usuaris.
 *
 * Gestiona el login i el logout (RF80) i el registre públic de clients
 * (RF81). La gestió de clients és a GestioClientsController, el perfil a
 * PerfilController i la validació de registres a AdminController.
 *
 * @author Equip4
 */
@Controller
@RequestMapping("/web/usuaris")
public class UsuarisController {

    @Autowired
    private UsuarisLogic usuarisService;

    @Autowired
    private FitxersLogic fitxersLogic;

    // --- AUTENTICACIÓ (LOGIN) ---
    /**
     * Mostra la pantalla de login. El formulari el processa Spring Security
     * (veure SecurityConfig).
     *
     * @return plantilla "login"
     */
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    /**
     * Tanca la sessió de l'usuari.
     *
     * @param session sessió HTTP a destruir
     * @return redirecció a la pàgina principal
     */
    @GetMapping("/logout")
    public String tancarSession(HttpSession session) {
        session.invalidate(); //Destrueix la sessió
        return "redirect:/";
    }

    // --- REGISTRE WEB (PUBLIC - RF81) ---
    /**
     * Mostra el formulari de registre públic.
     *
     * @param model model per passar un usuari buit al formulari
     * @return plantilla "registre"
     */
    @GetMapping("/registre")
    public String mostrarRegistre(Model model) {
        model.addAttribute("usuari", new Usuaris());
        return "registre";
    }

    /**
     * Processa el registre d'un client nou (RF81). Guarda les imatges del DNI
     * i de la llicència i deixa el registre pendent de validar.
     *
     * @param usuari dades del client
     * @param fitxerDni imatge del document d'identitat
     * @param fitxerLlicencia imatge de la llicència de conduir
     * @param model model per tornar a mostrar el formulari si hi ha errors
     * @return redirecció al login o el formulari amb el missatge d'error
     */
    @PostMapping("/registre")
    public String processarRegistre(@ModelAttribute("usuari") Usuaris usuari,
            @RequestParam("fitxerDni") MultipartFile fitxerDni,
            @RequestParam("fitxerLlicencia") MultipartFile fitxerLlicencia,
            Model model) {
        try {
            // Guardem les imatges del DNI i de la llicència a la carpeta uploads
            String prefix = usuari.getDni() != null ? usuari.getDni() : "client";
            usuari.setImatgedni(fitxersLogic.guardarFitxer(fitxerDni, "dni_" + prefix));
            usuari.setImatgeLlicencia(fitxersLogic.guardarFitxer(fitxerLlicencia, "llicencia_" + prefix));

            usuarisService.registrarClientWeb(usuari);
            return "redirect:/web/usuaris/login?registrePendent=true";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuari", usuari);
            return "registre";
        }
    }
}
