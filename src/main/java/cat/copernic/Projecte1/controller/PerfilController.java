package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web del perfil de l'agent i de l'administrador.
 *
 * Tots dos fan servir la mateixa pantalla (perfil-agent.html), cadascun amb
 * el seu prefix: {@code /web/agent/perfil} i {@code /web/admin/perfil}.
 *
 * @author Equip4
 */
@Controller
@RequestMapping({"/web/agent/perfil", "/web/admin/perfil"})
public class PerfilController {

    @Autowired
    private UsuarisLogic usuarisService;

    /**
     * Mostra el perfil de l'usuari que ha iniciat sessió.
     *
     * @param session sessió HTTP amb l'usuari loguejat
     * @param model model per passar les dades a la vista
     * @return plantilla "perfil-agent"
     */
    @GetMapping
    public String mostrarPerfil(HttpSession session, Model model) {
        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        // Carreguem les dades actualitzades des de la base de dades
        Usuaris usuariBD = usuarisService.getUsuariById(usuariLoguejat.getId());

        model.addAttribute("usuariLoguejat", usuariBD);
        model.addAttribute("usuari", usuariBD); // Necessari per al th:object="${usuari}"

        return "perfil-agent";
    }

    /**
     * Desa els canvis del perfil (nom i telèfon).
     *
     * @param usuariForm dades enviades des del formulari
     * @param session sessió HTTP amb l'usuari loguejat
     * @param redirectAttributes missatge d'èxit o d'error
     * @return redirecció al perfil
     */
    @PostMapping("/save")
    public String guardarPerfil(
            @ModelAttribute("usuari") Usuaris usuariForm,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        // Carreguem l'usuari real de la BD per no sobrescriure contrasenya, rol o estat
        Usuaris usuariBD = usuarisService.getUsuariById(usuariLoguejat.getId());

        if (usuariBD != null) {
            // Actualitzem els camps permesos (telèfon i/o nom)
            usuariBD.setTelefon(usuariForm.getTelefon());
            if (usuariForm.getNom() != null && !usuariForm.getNom().isBlank()) {
                usuariBD.setNom(usuariForm.getNom());
            }

            try {
                usuarisService.saveUsuari(usuariBD);
                // Actualitzem l'objecte a la sessió HTTP per mantenir la coherència
                session.setAttribute("usuariLoguejat", usuariBD);
                redirectAttributes.addFlashAttribute("missatge", "Perfil actualitzat amb èxit!");
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "No s'ha pogut carregar el perfil per actualitzar.");
        }

        return "redirect:" + RutaBase.obtenir(session) + "/perfil";
    }
}
