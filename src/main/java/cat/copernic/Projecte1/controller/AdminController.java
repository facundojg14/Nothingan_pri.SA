package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.logic.EmailService;
import cat.copernic.Projecte1.logic.ReservaLogic;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import cat.copernic.Projecte1.logic.VehicleLogic;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Controller
@RequestMapping("/web/admin")
public class AdminController {

    // Logger per registrar els errors inesperats al fitxer de logs (RN04)
    private final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UsuarisLogic usuarisService;

    @Autowired
    private VehicleLogic vehicleLogic;

    @Autowired
    private ReservaLogic reservaLogic;

    @Autowired
    private EmailService emailService; // <- Injectat EmailService

    @GetMapping({"", "/"})
    public String indexAdmin() {
        return "redirect:/web/admin/usuaris";
    }

    // ==========================================
    // SECCIÓN: GESTIÓN DE CLIENTES / USUARIOS
    // ==========================================
    @GetMapping("/usuaris")
    public String admin(@RequestParam(value = "dni", required = false) String dni, Model model) {
        List<Usuaris> llista;
        if (dni != null && !dni.trim().isEmpty()) {
            llista = usuarisService.findByDni(dni.trim());
        } else {
            llista = usuarisService.findAllUsuaris();
        }

        List<Usuaris> nomesClients = llista.stream()
                .filter(u -> u.getRol() != null && "CLIENT".equalsIgnoreCase(u.getRol().toString()))
                .toList();

        model.addAttribute("usuaris", nomesClients);
        model.addAttribute("dniFiltre", dni);

        return "admin-clients";
    }

    @GetMapping("/view/{id}")
    public String viewClientAdmin(@PathVariable("id") Long id, Model model) {
        Usuaris usuari = usuarisService.getUsuariById(id);
        model.addAttribute("usuari", usuari);
        model.addAttribute("modoLectura", true);
        return "admin-edit-clients";
    }

    @GetMapping("/edit/{id}")
    public String editClientAdmin(@PathVariable("id") Long id, Model model) {
        Usuaris usuari = usuarisService.getUsuariById(id);
        model.addAttribute("usuari", usuari);
        model.addAttribute("modoLectura", false);
        return "admin-edit-clients";
    }

    @GetMapping("/new")
    public String newClientAdmin(Model model) {
        Usuaris u = new Usuaris();
        u.setEstatRegistre("PENDENT");
        u.setPremium(false);

        model.addAttribute("usuari", u);
        model.addAttribute("modoLectura", false);
        return "admin-edit-clients";
    }

    @GetMapping("/delete/{id}")
    public String deleteClientAdmin(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarisService.deleteUsuariById(id);
            redirectAttributes.addFlashAttribute("missatge", "Client esborrat correctament.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/usuaris";
    }

    @PostMapping("/save")
    public String saveClientAdmin(@ModelAttribute("usuari") Usuaris usuari, Model model) {
        try {
            if (usuari.getId() != null && (usuari.getPassword() == null || usuari.getPassword().isBlank())) {
                Usuaris usuariExistente = usuarisService.getUsuariById(usuari.getId());
                if (usuariExistente != null) {
                    usuari.setPassword(usuariExistente.getPassword());
                }
            }

            usuarisService.saveUsuari(usuari);
            return "redirect:/web/admin/usuaris";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuari", usuari);
            model.addAttribute("modoLectura", false);
            return "admin-edit-clients";
        }
    }

    // ==========================================
    // SECCIÓN: GESTIÓN DE VEHÍCULOS
    // ==========================================
    @GetMapping("/vehicles")
    public String findAllVehicles(@RequestParam(name = "matricula", required = false) String matricula, Model model) {
        List<Vehicle> llista = vehicleLogic.findByMatricula(matricula);

        model.addAttribute("vehicles", llista);
        model.addAttribute("matricula", matricula);

        return "admin-vehicles";
    }

    @GetMapping("/vehicles/new")
    public String getFormVehicle(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCategoria(Categoria.SUV);
        vehicle.setEstatVehicle(EstatVehicle.Disponible);
        vehicle.setPreuDia(BigDecimal.ZERO);

        model.addAttribute("vehicle", vehicle);
        return "admin-edit-vehicle";
    }

    @PostMapping("/vehicles/save")
    public String saveVehicle(@ModelAttribute("vehicle") Vehicle vehicle, Model model, RedirectAttributes redirectAttributes) {
        if (vehicleLogic.existsByMatricula(vehicle.getMatricula(), vehicle.getId())) {
            model.addAttribute("error", "La matrícula '" + vehicle.getMatricula() + "' ja està registrada al sistema!");
            model.addAttribute("vehicle", vehicle);
            return "admin-edit-vehicle";
        }

        try {
            if (vehicle.getEstatVehicle() == null) {
                vehicle.setEstatVehicle(EstatVehicle.Disponible);
            }
            vehicleLogic.saveVehicle(vehicle);
            redirectAttributes.addFlashAttribute("missatge", "Vehicle desat correctament!");
            return "redirect:/web/admin/vehicles";

        } catch (Exception e) {
            logger.error("Error en desar el vehicle {}", vehicle.getMatricula(), e);
            model.addAttribute("error", "Error en desar el vehicle: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            return "admin-edit-vehicle";
        }
    }

    @GetMapping("/vehicles/edit/{id}")
    public String editVehicleById(@PathVariable("id") Long vehicleId, Model model) {
        Vehicle vehicle = vehicleLogic.getVehicleById(vehicleId);

        if (vehicle == null) {
            return "redirect:/web/admin/vehicles";
        }

        model.addAttribute("vehicle", vehicle);
        return "admin-edit-vehicle";
    }

    @GetMapping("/vehicles/delete/{id}")
    public String deleteVehicleById(@PathVariable("id") Long vehicleId, RedirectAttributes redirectAttributes) {
        try {
            vehicleLogic.deleteVehicleById(vehicleId);
            redirectAttributes.addFlashAttribute("missatge", "Vehicle eliminat correctament.");
        } catch (Exception e) {
            logger.error("Error en eliminar el vehicle {}", vehicleId, e);
            redirectAttributes.addFlashAttribute("error", "No es pot eliminar el vehicle perquè té reserves associades.");
        }

        return "redirect:/web/admin/vehicles";
    }

    // ==========================================
    // SECCIÓN: GESTIÓN DE RESERVAS
    // ==========================================
    @GetMapping("/reserves")
    public String findAllReserves(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) Integer detall,
            Model model) {

        List<Reserva> reserves = reservaLogic.findAllReservas().stream()
                .filter(r -> dni == null || dni.isBlank()
                || (r.getUsuari() != null && r.getUsuari().getDni() != null && r.getUsuari().getDni().contains(dni)))
                .filter(r -> codi == null || codi.isBlank()
                || (r.getCodiReserva() != null && r.getCodiReserva().contains(codi)))
                .filter(r -> matricula == null || matricula.isBlank()
                || (r.getVehicle() != null && r.getVehicle().getMatricula() != null && r.getVehicle().getMatricula().contains(matricula)))
                .filter(r -> data == null || data.isBlank()
                || (r.getDataReserva() != null && r.getDataReserva().toLocalDate().toString().equals(data)))
                .toList();

        model.addAttribute("reserves", reserves);
        model.addAttribute("dni", dni);
        model.addAttribute("codi", codi);
        model.addAttribute("matricula", matricula);
        model.addAttribute("data", data);

        if (detall != null) {
            Reserva seleccionada = reservaLogic.getReservaById(detall);
            if (seleccionada != null) {
                model.addAttribute("reservaSeleccionada", seleccionada);
            }
        }

        return "admin-reserves";
    }

    @GetMapping("/reserves/new")
    public String getFormReserva(Model model) {
        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());

        model.addAttribute("reserva", reserva);
        carregarDadesFormulari(model);

        return "admin-edit-reserva";
    }

    @GetMapping("/reserves/disponibles")
    public String buscarDisponibles(
            @RequestParam String dataInici,
            @RequestParam String dataFi,
            Model model) {

        LocalDateTime inici;
        LocalDateTime fi;

        try {
            inici = LocalDateTime.parse(dataInici);
            fi = LocalDateTime.parse(dataFi);
        } catch (DateTimeParseException e) {
            model.addAttribute("error", "Les dates no són vàlides.");

            Reserva reserva = new Reserva();
            reserva.setVehicle(new Vehicle());
            reserva.setUsuari(new Usuaris());

            model.addAttribute("reserva", reserva);
            model.addAttribute("vehicles", List.of());
            model.addAttribute("usuaris", usuarisService.findAllUsuaris());
            model.addAttribute("dataInici", dataInici);
            model.addAttribute("dataFi", dataFi);

            return "admin-edit-reserva";
        }

        if (!fi.isAfter(inici)) {
            model.addAttribute("error", "La data final ha de ser posterior a la data inicial.");

            Reserva reserva = new Reserva();
            reserva.setVehicle(new Vehicle());
            reserva.setUsuari(new Usuaris());

            model.addAttribute("reserva", reserva);
            model.addAttribute("vehicles", List.of());
            model.addAttribute("usuaris", usuarisService.findAllUsuaris());
            model.addAttribute("dataInici", dataInici);
            model.addAttribute("dataFi", dataFi);

            return "admin-edit-reserva";
        }

        List<Vehicle> disponibles = vehicleLogic.findAllVehicles().stream()
                .filter(v -> reservaLogic.vehicleDisponible(v.getId(), inici, fi))
                .toList();

        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());
        reserva.setDataInici(inici);
        reserva.setDataFi(fi);

        model.addAttribute("reserva", reserva);
        model.addAttribute("vehicles", disponibles);
        model.addAttribute("senseDisponibilitat", disponibles.isEmpty());
        model.addAttribute("usuaris", usuarisService.findAllUsuaris());
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);

        return "admin-edit-reserva";
    }

    @PostMapping("/reserves/save")
    public String saveReserva(@ModelAttribute Reserva reserva, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (reserva.getVehicle() != null && reserva.getVehicle().getId() > 0) {
                Vehicle v = vehicleLogic.getVehicleById(reserva.getVehicle().getId());
                reserva.setVehicle(v);
            }
            if (reserva.getUsuari() != null && reserva.getUsuari().getId() != null) {
                Usuaris u = usuarisService.getUsuariById(reserva.getUsuari().getId());
                reserva.setUsuari(u);
            }

            reservaLogic.saveReserva(reserva);
            redirectAttributes.addFlashAttribute("missatge", "Reserva creada correctament.");
            return "redirect:/web/admin/reserves";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reserva", reserva);
            carregarDadesFormulari(model);
            return "admin-edit-reserva";
        } catch (Exception e) {
            logger.error("Error en desar la reserva", e);
            model.addAttribute("error", "S'ha produït un error en desar la reserva: " + e.getMessage());
            model.addAttribute("reserva", reserva);
            carregarDadesFormulari(model);
            return "admin-edit-reserva";
        }
    }

    @GetMapping("/reserves/detall/{id}")
    public String detailsReserva(
            @PathVariable Integer id,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            RedirectAttributes attributes) {

        attributes.addAttribute("detall", id);

        if (dni != null && !dni.isBlank()) {
            attributes.addAttribute("dni", dni);
        }
        if (codi != null && !codi.isBlank()) {
            attributes.addAttribute("codi", codi);
        }
        if (matricula != null && !matricula.isBlank()) {
            attributes.addAttribute("matricula", matricula);
        }
        if (data != null && !data.isBlank()) {
            attributes.addAttribute("data", data);
        }

        return "redirect:/web/admin/reserves";
    }

    // ACTUALITZAT: Mètode d'anul·lació d'administrador integrat amb EmailService
    @PostMapping("/reserves/cancelar/{id}")
    public String cancelReserva(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "") String motiu,
            @RequestParam(required = false, defaultValue = "100") double percentatgeRetorn,
            RedirectAttributes redirectAttributes) {

        Reserva reserva = reservaLogic.getReservaById(id);

        if (reserva != null && reservaLogic.anularReserva(id)) {

            BigDecimal preuTotal = reserva.getPreuLloguer() != null ? reserva.getPreuLloguer() : BigDecimal.ZERO;
            BigDecimal importRetornat = preuTotal.multiply(BigDecimal.valueOf(percentatgeRetorn / 100.0));

            if (reserva.getUsuari() != null && reserva.getUsuari().getEmail() != null) {
                emailService.enviarEmailAnullacio(
                    reserva.getUsuari().getEmail(),
                    reserva.getUsuari().getNom(),
                    reserva.getCodiReserva(),
                    percentatgeRetorn,
                    importRetornat,
                    motiu
                );
            }

            redirectAttributes.addFlashAttribute("missatge", "Reserva anul·lada per l'administrador i email enviat al client.");
        } else {
            redirectAttributes.addFlashAttribute("error", "No s'ha pogut anul·lar la reserva.");
        }

        return "redirect:/web/admin/reserves";
    }

    private void carregarDadesFormulari(Model model) {
        model.addAttribute("vehicles", vehicleLogic.findAllVehicles());
        model.addAttribute("usuaris", usuarisService.findAllUsuaris());
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

            if (agent.getId() != null && (agent.getPassword() == null || agent.getPassword().isBlank())) {
                Usuaris usuariExistente = usuarisService.getUsuariById(agent.getId());
                if (usuariExistente != null) {
                    agent.setPassword(usuariExistente.getPassword());
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
    
    // ==========================================
    // SECCIÓN: PERFIL DE ADMINISTRADOR
    // ==========================================
    
    @GetMapping("/perfil")
    public String mostrarPerfilAdmin(jakarta.servlet.http.HttpSession session, Model model) {
        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        Usuaris usuariBD = usuarisService.getUsuariById(usuariLoguejat.getId());
        
        model.addAttribute("usuariLoguejat", usuariBD);
        model.addAttribute("usuari", usuariBD);
        
        return "perfil-admin";
    }

    @PostMapping("/perfil/save")
    public String guardarPerfilAdmin(
            @ModelAttribute("usuari") Usuaris usuariForm, 
            jakarta.servlet.http.HttpSession session, 
            RedirectAttributes redirectAttributes) {
            
        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");
        if (usuariLoguejat == null) {
            return "redirect:/web/usuaris/login";
        }

        Usuaris usuariBD = usuarisService.getUsuariById(usuariLoguejat.getId());

        if (usuariBD != null) {
            usuariBD.setTelefon(usuariForm.getTelefon());
            if (usuariForm.getNom() != null && !usuariForm.getNom().isBlank()) {
                usuariBD.setNom(usuariForm.getNom());
            }

            usuarisService.saveUsuari(usuariBD);
            session.setAttribute("usuariLoguejat", usuariBD);

            redirectAttributes.addFlashAttribute("missatge", "Perfil actualitzat amb èxit!");
        } else {
            redirectAttributes.addFlashAttribute("error", "No s'ha pogut carregar el perfil per actualitzar.");
        }

        return "redirect:/web/admin/perfil";
    }
}