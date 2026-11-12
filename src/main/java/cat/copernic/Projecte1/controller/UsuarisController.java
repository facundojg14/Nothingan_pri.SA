package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.logic.FitxersLogic;
import cat.copernic.Projecte1.logic.ReservaLogic;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import cat.copernic.Projecte1.logic.VehicleLogic;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/usuaris")
public class UsuarisController {

    @Autowired
    private UsuarisLogic usuarisService;

    @Autowired
    private ReservaLogic reservaLogic;

    @Autowired
    private VehicleLogic vehicleLogic;

    @Autowired
    private FitxersLogic fitxersLogic;

    // --- LLISTAT GENERAL I FILTRATGE MÚLTIPLE (RF26 / CERCA EMAIL) ---
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

    // --- CREAR Y EDITAR USUARIO (ADMIN/AGENT) ---
    @GetMapping("/new")
    public String getForm(Model model) {

        Usuaris u = new Usuaris();
        u.setEstatRegistre("PENDENT");
        u.setPremium(false);

        model.addAttribute("usuari", u);
        return "edit-usuari";
    }

    @GetMapping("/edit/{id}")
    public String editById(@PathVariable("id") Long usuariId, Model model) {
        Usuaris usuari = usuarisService.getUsuariById(usuariId);

        if (usuari == null) {
            return "redirect:/web/usuaris/all";
        }

        model.addAttribute("usuari", usuari);
        return "edit-usuari";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("usuari") Usuaris usuari, Model model) {
        try {
            if (usuari.getId() != null) {
                usuarisService.actualitzarClientPerAgent(usuari);
            } else {
                usuarisService.saveUsuari(usuari);
            }
            return "redirect:/web/usuaris/all";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuari", usuari);
            return "edit-usuari";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteById(@PathVariable("id") Long usuariId, RedirectAttributes redirectAttributes) {
        try {
            usuarisService.deleteUsuariById(usuariId);
            redirectAttributes.addFlashAttribute("missatge", "Client esborrat correctament.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/usuaris/all";
    }

    // --- AUTENTICACIÓ (LOGIN) ---
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    // Tancar sessió
    @GetMapping("/logout")
    public String tancarSession(HttpSession session) {
        session.invalidate(); //Destrueix la sessió
        return "redirect:/";
    }

    // --- REGISTRE WEB (PUBLIC - RF81) ---
    @GetMapping("/registre")
    public String mostrarRegistre(Model model) {
        model.addAttribute("usuari", new Usuaris());
        return "registre";
    }

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

    // --- VALIDAR REGISTRES (ADMINISTRADOR - RF82) ---
    @GetMapping("/pendents")
    public String llistarPendents(Model model) {
        List<Usuaris> pendents = usuarisService.obtenirRegistresPendents();
        model.addAttribute("usuarisPendents", pendents);
        return "validar-registres";
    }

    @PostMapping("/validar/{id}")
    public String validarRegistre(@PathVariable("id") Long id, @RequestParam("estat") String nouEstat,
            RedirectAttributes redirectAttributes) {
        // Només acceptem els dos estats possibles
        if (!"VALIDAT".equals(nouEstat) && !"REBUTJAT".equals(nouEstat)) {
            redirectAttributes.addFlashAttribute("error", "Estat no vàlid.");
            return "redirect:/web/usuaris/pendents";
        }

        try {
            usuarisService.canviarEstatRegistre(id, nouEstat);
            redirectAttributes.addFlashAttribute("missatge", "VALIDAT".equals(nouEstat)
                    ? "Registre validat. L'usuari ja és client i pot iniciar sessió."
                    : "Registre rebutjat.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/usuaris/pendents";
    }
// ==========================================
    // SECCIÓ: PERFIL D'AGENT / USUARI LOGUEJAT
    // ==========================================

    // 1. MOSTRAR EL PERFIL DE L'AGENT LOGUEJAT
    @GetMapping("/perfil")
    public String mostrarPerfilAgent(HttpSession session, Model model) {
        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        // Carreguem les dades actualitzades des de la base de dades
        Usuaris usuariBD = usuarisService.getUsuariById(usuariLoguejat.getId());

        model.addAttribute("usuariLoguejat", usuariBD);
        model.addAttribute("usuari", usuariBD); // Necessari per al th:object="${usuari}" a perfil.html

        return "perfil-agent"; // Renderitza la plantilla perfil.html
    }

    // 2. DESAR ELS CANVIS DEL PERFIL
    @PostMapping("/perfil/save")
    public String guardarPerfilAgent(
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

            // Guardem a la BD
            usuarisService.saveUsuari(usuariBD);

            // Actualitzem l'objecte a la sessió HTTP per mantenir la coherència
            session.setAttribute("usuariLoguejat", usuariBD);

            redirectAttributes.addFlashAttribute("missatge", "Perfil actualitzat amb èxit!");
        } else {
            redirectAttributes.addFlashAttribute("error", "No s'ha pogut carregar el perfil per actualitzar.");
        }

        return "redirect:/web/usuaris/perfil";
    }
}
