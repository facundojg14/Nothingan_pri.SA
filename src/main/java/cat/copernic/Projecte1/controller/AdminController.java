package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.logic.ReservaLogic;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import java.math.BigDecimal;
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
 * Controlador web de les pantalles exclusives de l'administrador.
 *
 * Gestiona els agents (RF10-RF15), la validació de registres de clients
 * (RF82) i l'informe de reserves per client (RF91).
 *
 * La resta de pantalles (clients, vehicles, reserves i perfil) són les
 * mateixes que fa servir l'agent, amb el prefix /web/admin (veure
 * GestioClientsController, VehicleController, ReservaController i
 * PerfilController).
 *
 * @author Equip4
 */
@Controller
@RequestMapping("/web/admin")
public class AdminController {

    @Autowired
    private UsuarisLogic usuarisService;

    @Autowired
    private ReservaLogic reservaLogic;

    /**
     * Pàgina d'entrada de l'administrador: la llista de clients.
     *
     * @return redirecció a la llista de clients
     */
    @GetMapping({"", "/"})
    public String indexAdmin() {
        return "redirect:/web/admin/usuaris/all";
    }

    // ==========================================
    // SECCIÓ: VALIDAR REGISTRES (RF82)
    // ==========================================
    /**
     * Llista els registres de clients pendents de validar (RF82).
     *
     * @param model model per passar les dades a la vista
     * @return plantilla "validar-registres"
     */
    @GetMapping("/pendents")
    public String llistarPendents(Model model) {
        List<Usuaris> pendents = usuarisService.obtenirRegistresPendents();
        model.addAttribute("usuarisPendents", pendents);
        return "validar-registres";
    }

    /**
     * Valida o rebutja un registre pendent (RF82).
     *
     * @param id identificador de l'usuari pendent
     * @param nouEstat "VALIDAT" o "REBUTJAT"
     * @param redirectAttributes missatge d'èxit o d'error
     * @return redirecció a la llista de registres pendents
     */
    @PostMapping("/validar/{id}")
    public String validarRegistre(@PathVariable("id") Long id, @RequestParam("estat") String nouEstat,
            RedirectAttributes redirectAttributes) {
        // Només acceptem els dos estats possibles
        if (!"VALIDAT".equals(nouEstat) && !"REBUTJAT".equals(nouEstat)) {
            redirectAttributes.addFlashAttribute("error", "Estat no vàlid.");
            return "redirect:/web/admin/pendents";
        }

        try {
            usuarisService.canviarEstatRegistre(id, nouEstat);
            redirectAttributes.addFlashAttribute("missatge", "VALIDAT".equals(nouEstat)
                    ? "Registre validat. L'usuari ja és client i pot iniciar sessió."
                    : "Registre rebutjat.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/pendents";
    }

    // ==========================================
    // SECCIÓN: GESTIÓN DE AGENTES
    // ==========================================
    @GetMapping("/agents")
    public String findAllAgents(@RequestParam(value = "dni", required = false) String dni, Model model) {
        List<Usuaris> llista;

        if (dni != null && !dni.trim().isEmpty()) {
            llista = usuarisService.findByDni(dni.trim());
        } else {
            llista = usuarisService.findAllUsuaris();
        }

        List<Usuaris> nomesAgents = llista.stream()
                .filter(u -> u.getRol() != null && "AGENT".equalsIgnoreCase(u.getRol().toString()))
                .toList();

        model.addAttribute("agents", nomesAgents);
        model.addAttribute("dniFiltre", dni);

        return "admin-agent";
    }

    @GetMapping("/agents/new")
    public String newAgentAdmin(Model model) {
        Usuaris agent = new Usuaris();
        agent.setEstatRegistre("VALIDAT");
        agent.setPremium(false);

        model.addAttribute("agent", agent);
        model.addAttribute("modoLectura", false);
        return "admin-agent-edit";
    }

    @GetMapping("/agents/edit/{id}")
    public String editAgentAdmin(@PathVariable("id") Long id, Model model) {
        Usuaris agent = usuarisService.getUsuariById(id);

        if (agent == null) {
            return "redirect:/web/admin/agents";
        }

        model.addAttribute("agent", agent);
        model.addAttribute("modoLectura", false);
        return "admin-agent-edit";
    }

    @GetMapping("/agents/view/{id}")
    public String viewAgentAdmin(@PathVariable("id") Long id, Model model) {
        Usuaris agent = usuarisService.getUsuariById(id);

        if (agent == null) {
            return "redirect:/web/admin/agents";
        }

        model.addAttribute("agent", agent);
        model.addAttribute("modoLectura", true);
        return "admin-agent-edit";
    }

    @PostMapping("/agents/save")
    public String saveAgentAdmin(@ModelAttribute("agent") Usuaris agent, Model model, RedirectAttributes redirectAttributes) {
        try {
            agent.setRol(rol.AGENT);

            if (agent.getId() == null) {
                // Els agents creats directament per l'administrador ja queden validats.
                agent.setEstatRegistre("VALIDAT");
            } else {
                Usuaris usuariExistente = usuarisService.getUsuariById(agent.getId());
                if (usuariExistente != null) {
                    if (agent.getPassword() == null || agent.getPassword().isBlank()) {
                        agent.setPassword(usuariExistente.getPassword());
                    }
                    // El formulari d'edició no modifica l'estat del registre.
                    agent.setEstatRegistre(usuariExistente.getEstatRegistre());
                }
            }

            usuarisService.saveUsuari(agent);
            redirectAttributes.addFlashAttribute("missatge", "Agent desat correctament!");
            return "redirect:/web/admin/agents";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("agent", agent);
            model.addAttribute("modoLectura", false);
            return "admin-agent-edit";
        }
    }

    @GetMapping("/agents/delete/{id}")
    public String deleteAgentAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarisService.deleteUsuariById(id);
            redirectAttributes.addFlashAttribute("missatge", "Agent eliminat correctament.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/agents";
    }

    // ==========================================
    // SECCIÓ: INFORMES (RF91)
    // ==========================================
    @GetMapping({"/informes", "/informes/reserves"})
    public String mostrarInformeReserves(
            @RequestParam(value = "clientId", required = false) String clientIdStr,
            Model model) {

        Integer clientId = null;
        if (clientIdStr != null && !clientIdStr.trim().isEmpty()) {
            try {
                clientId = Integer.parseInt(clientIdStr.trim());
            } catch (NumberFormatException ignored) {
                clientId = null;
            }
        }

        List<Reserva> reserves = reservaLogic.obtenirInformeReserves(clientId);

        List<Usuaris> totsClients = usuarisService.findAllUsuaris().stream()
                .filter(u -> u.getRol() != null && "CLIENT".equalsIgnoreCase(u.getRol().toString()))
                .toList();

        BigDecimal importTotal = reserves.stream()
                .map(r -> r.getPreuLloguer() != null ? r.getPreuLloguer() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("reserves", reserves);
        model.addAttribute("clients", totsClients);
        model.addAttribute("clientIdSeleccionat", clientId);
        model.addAttribute("totalReserves", reserves.size());
        model.addAttribute("importTotal", importTotal);

        return "admin-informes";
    }
}
