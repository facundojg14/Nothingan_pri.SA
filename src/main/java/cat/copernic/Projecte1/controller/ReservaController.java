package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.logic.EmailService;
import cat.copernic.Projecte1.logic.ReservaLogic;
import cat.copernic.Projecte1.logic.UsuarisLogic;
import cat.copernic.Projecte1.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web de les reserves (agent i administrador).
 *
 * Permet llistar i filtrar reserves (RF52, RF54), consultar-ne el detall
 * (RF53), buscar vehicles disponibles, crear reserves (RF50), anul·lar-les
 * (RF55), i lliurar (RF49) i retornar (RF48) el vehicle.
 *
 * La fan servir l'agent ({@code /web/agent/reserves}) i l'administrador
 * ({@code /web/admin/reserves}).
 *
 * @author Equip4
 */
@Controller
// La mateixa pantalla serveix per a l'agent i per a l'administrador (cada un amb el seu prefix)
@RequestMapping({"/web/agent/reserves", "/web/admin/reserves"})
@RequestScope
public class ReservaController {

    private final ReservaLogic reservaLogic;
    private final VehicleLogic vehicleLogic;
    private final UsuarisLogic usuarisLogic;
    private final EmailService emailService; // <- Injectem el servei de correu

    public ReservaController(ReservaLogic reservaLogic,
            VehicleLogic vehicleLogic,
            UsuarisLogic usuarisLogic,
            EmailService emailService) { // <- S'afegeix al constructor
        this.reservaLogic = reservaLogic;
        this.vehicleLogic = vehicleLogic;
        this.usuarisLogic = usuarisLogic;
        this.emailService = emailService;
    }

    @GetMapping("/all")
    public String findAll(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) Integer detall,
            Model model) {

        List<Reserva> reserves = reservaLogic.findAllReservas().stream()
                .filter(r -> dni == null || dni.isBlank()
                || (r.getUsuari() != null && r.getUsuari().getDni() != null && r.getUsuari().getDni().contains(dni)))
                .filter(r -> email == null || email.isBlank()
                || (r.getUsuari() != null && r.getUsuari().getEmail() != null
                && r.getUsuari().getEmail().toLowerCase().contains(email.toLowerCase())))
                .filter(r -> codi == null || codi.isBlank()
                || (r.getCodiReserva() != null && r.getCodiReserva().contains(codi)))
                .filter(r -> matricula == null || matricula.isBlank()
                || (r.getVehicle() != null && r.getVehicle().getMatricula() != null && r.getVehicle().getMatricula().contains(matricula)))
                .filter(r -> data == null || data.isBlank()
                || (r.getDataReserva() != null
                && r.getDataReserva().toLocalDate().toString().equals(data)))
                .toList();

        model.addAttribute("reserves", reserves);
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("codi", codi);
        model.addAttribute("matricula", matricula);
        model.addAttribute("data", data);

        if (detall != null) {
            Reserva seleccionada = reservaLogic.getReservaById(detall);
            if (seleccionada != null) {
                model.addAttribute("reservaSeleccionada", seleccionada);
            }
        }

        return "reserves-list";
    }

    @GetMapping("/new")
    public String getForm(Model model) {
        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());

        model.addAttribute("reserva", reserva);

        carregarDadesFormulari(model);

        return "nova-reserva-agent";
    }

    @GetMapping("/disponibles")
    public String buscarDisponibles(
            @RequestParam String dataInici,
            @RequestParam String dataFi,
            @RequestParam(required = false) String categoria,
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
            model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
            model.addAttribute("dataInici", dataInici);
            model.addAttribute("dataFi", dataFi);
            model.addAttribute("categoria", categoria);

            return "nova-reserva-agent";
        }

        if (!fi.isAfter(inici)) {
            model.addAttribute("error",
                    "La data final ha de ser posterior a la data inicial.");

            Reserva reserva = new Reserva();
            reserva.setVehicle(new Vehicle());
            reserva.setUsuari(new Usuaris());

            model.addAttribute("reserva", reserva);
            model.addAttribute("vehicles", List.of());
            model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
            model.addAttribute("dataInici", dataInici);
            model.addAttribute("dataFi", dataFi);
            model.addAttribute("categoria", categoria);

            return "nova-reserva-agent";
        }

        // MODIFICACIÓ: Només es mostren vehicles ACTIUS i DISPONIBLES que no estiguin reservats en aquestes dates
        List<Vehicle> disponibles = vehicleLogic.findAllVehicles().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActiu()) && v.getEstatVehicle() == EstatVehicle.Disponible)
                .filter(v -> reservaLogic.vehicleDisponible(v.getId(), inici, fi))
                .filter(v -> categoria == null || categoria.isBlank()
                || v.getCategoria().name().equals(categoria))
                .toList();

        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());
        reserva.setDataInici(inici);
        reserva.setDataFi(fi);

        model.addAttribute("reserva", reserva);
        model.addAttribute("vehicles", disponibles);
        model.addAttribute("senseDisponibilitat", disponibles.isEmpty());
        model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);
        model.addAttribute("categoria", categoria);

        return "nova-reserva-agent";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Reserva reserva,
            HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Validació extra: Evitar reservar un vehicle no actiu o no disponible
            if (reserva.getVehicle() != null && reserva.getVehicle().getId() > 0) {
                Vehicle v = vehicleLogic.getVehicleById(reserva.getVehicle().getId());
                if (v != null && (!Boolean.TRUE.equals(v.getActiu()) || v.getEstatVehicle() != EstatVehicle.Disponible)) {
                    redirectAttributes.addFlashAttribute("error", "Aquest vehicle està donat de baixa i no es pot reservar.");
                    return "redirect:" + RutaBase.obtenir(session) + "/reserves/new";
                }
            }

            reservaLogic.saveReserva(reserva);
            redirectAttributes.addFlashAttribute("missatge", "Reserva creada correctament.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/new";
        }

        return "redirect:" + RutaBase.obtenir(session) + "/reserves/all";
    }

    @GetMapping("/detall/{id}")
    public String details(@PathVariable Integer id,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            HttpSession session, RedirectAttributes attributes) {

        attributes.addAttribute("detall", id);

        if (dni != null && !dni.isBlank()) {
            attributes.addAttribute("dni", dni);
        }
        if (email != null && !email.isBlank()) {
            attributes.addAttribute("email", email);
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

        return "redirect:" + RutaBase.obtenir(session) + "/reserves/all";
    }

    @PostMapping("/cancelar/{id}")
    public String cancel(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "") String motiu,
            @RequestParam(required = false, defaultValue = "100") double percentatgeRetorn,
            HttpSession session, RedirectAttributes redirectAttributes) {

        Reserva reserva = reservaLogic.getReservaById(id);

        if (reserva != null && reservaLogic.anularReserva(id)) {

            BigDecimal preuTotal = reserva.getPreuLloguer() != null ? reserva.getPreuLloguer() : BigDecimal.ZERO;
            BigDecimal importRetornat = preuTotal.multiply(BigDecimal.valueOf(percentatgeRetorn / 100.0));

            // Crida a la instància emailService (en minúscula)
            if (reserva.getUsuari() != null) {
                emailService.enviarEmailAnullacio(
                        reserva.getUsuari().getEmail(),
                        reserva.getUsuari().getNom(),
                        reserva.getCodiReserva(),
                        percentatgeRetorn,
                        importRetornat,
                        motiu
                );
            }

            redirectAttributes.addFlashAttribute("missatge", "Reserva anul·lada i email enviat al client.");
        } else {
            redirectAttributes.addFlashAttribute("error", "No s'ha pogut anul·lar la reserva.");
        }

        return "redirect:" + RutaBase.obtenir(session) + "/reserves/all";
    }

    // Mètode privat afegit per carregar dades dels desplegables al formulari
    private void carregarDadesFormulari(Model model) {
        List<Vehicle> vehiclesActius = vehicleLogic.findAllVehicles().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActiu()) && v.getEstatVehicle() == EstatVehicle.Disponible)
                .toList();
        model.addAttribute("vehicles", vehiclesActius);
        model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
    }

    /**
     * Pantalla de cerca amb filtres per seleccionar la reserva a lliurar.
     */
    @GetMapping("/lliurar")
    public String cercarPerLliurar(
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String dataInici,
            Model model) {

        List<Reserva> reservesPendents = reservaLogic.cercarReservesPerLliurar(codi, dni, email, matricula, dataInici);

        model.addAttribute("reserves", reservesPendents);
        model.addAttribute("codi", codi);
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("matricula", matricula);
        model.addAttribute("dataInici", dataInici);

        return "lliurar-vehicle-cerca";
    }

    /**
     * Formulari de verificació visual, descripció, fotos i confirmació del
     * lliurament.
     */
    @GetMapping("/lliurar/{id}")
    public String formulariLliurar(@PathVariable Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Reserva reserva = reservaLogic.getReservaById(id);

        if (reserva == null || reserva.getEstat() != cat.copernic.Projecte1.entity.Enum.EstatReserva.Confirmada) {
            redirectAttributes.addFlashAttribute("error", "La reserva no existeix o no està pendent de lliurament.");
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/lliurar";
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("ara", LocalDateTime.now().withSecond(0).withNano(0));

        return "lliurar-vehicle-form";
    }

    /**
     * Processa el formulari de lliurament amb fotos opcionals.
     */
    @PostMapping("/lliurar/{id}")
    public String processarLliurament(
            @PathVariable Integer id,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraLliurament,
            @RequestParam String descripcioLliurament,
            @RequestParam(name = "fotos", required = false) org.springframework.web.multipart.MultipartFile[] fotos,
            HttpSession session, RedirectAttributes redirectAttributes) {

        try {
            reservaLogic.confirmarLliuramentVehicle(id, dataHoraLliurament, descripcioLliurament, fotos);
            redirectAttributes.addFlashAttribute("missatge", "Vehicle lliurat correctament al client!");
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/all";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/lliurar/" + id;
        } catch (java.io.IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error en desar les fotografies del lliurament.");
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/lliurar/" + id;
        }
    }
    
    /**
     * Busca reservas pendientes de devolución aplicando filtros.
     */
    @GetMapping("/retornar")
    public String cercarPerRetornar(
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String dataInici,
            Model model) {

        List<Reserva> reserves = reservaLogic.cercarReservesPerRetornar(
                codi, dni, email, matricula, dataInici);

        model.addAttribute("reserves", reserves);
        model.addAttribute("codi", codi);
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("matricula", matricula);
        model.addAttribute("dataInici", dataInici);

        return "retornar-vehicle-cerca";
    }

    /**
     * Muestra el formulario de devolución de una reserva.
     */
    @GetMapping("/retornar/{id}")
    public String formulariRetornar(
            @PathVariable Integer id,
            Model model,
            HttpSession session, RedirectAttributes redirectAttributes) {

        Reserva reserva = reservaLogic.getReservaById(id);

        if (reserva == null
                || reserva.getEstat() != cat.copernic.Projecte1.entity.Enum.EstatReserva.Lliurada) {
            redirectAttributes.addFlashAttribute(
                    "error", "La reserva no existeix o no està pendent de devolució.");
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/retornar";
        }

        model.addAttribute("reserva", reserva);
        model.addAttribute("ara", LocalDateTime.now().withSecond(0).withNano(0));

        return "retornar-vehicle-form";
    }
    
    /**
     * Procesa la devolución del vehículo.
     */
    @PostMapping("/retornar/{id}")
    public String processarRetorn(
            @PathVariable Integer id,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dataHoraRetorn,
            @RequestParam String observacions,
            @RequestParam(defaultValue = "false") boolean desperfectesImputables,
            @RequestParam(defaultValue = "false") boolean vehiculoAveriado,
            HttpSession session, RedirectAttributes redirectAttributes) {

        try {
            reservaLogic.confirmarRetornVehicle(
                    id, dataHoraRetorn, observacions,
                    desperfectesImputables, vehiculoAveriado);

            redirectAttributes.addFlashAttribute(
                    "missatge", "Vehicle retornat correctament.");

            return "redirect:" + RutaBase.obtenir(session) + "/reserves/all";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/retornar/" + id;
        }
    }
}