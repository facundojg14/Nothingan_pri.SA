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
 * Controlador de reservas para agentes y administradores.
 *
 * Permite consultar, filtrar, crear y anular reservas, así como gestionar
 * la entrega y devolución de vehículos.
 *
 * @author Equip4
 */
@Controller
// La misma pantalla sirve para agentes y administradores, cada uno con su prefijo.
@RequestMapping({"/web/agent/reserves", "/web/admin/reserves"})
@RequestScope
public class ReservaController {

    // ==================== ATRIBUTOS ====================

    // Servicios para gestionar reservas, vehículos, usuarios y correos.
    private final ReservaLogic reservaLogic;
    private final VehicleLogic vehicleLogic;
    private final UsuarisLogic usuarisLogic;
    private final EmailService emailService; // Servicio de envío de correos.

    // ==================== CONSTRUCTOR ====================

    /**
     * Inicializa los servicios necesarios para gestionar las reservas.
     */
    public ReservaController(ReservaLogic reservaLogic,
            VehicleLogic vehicleLogic,
            UsuarisLogic usuarisLogic,
            EmailService emailService) {
        this.reservaLogic = reservaLogic;
        this.vehicleLogic = vehicleLogic;
        this.usuarisLogic = usuarisLogic;
        this.emailService = emailService;
    }

    // ==================== CONSULTA DE RESERVAS ====================

    /**
     * Muestra todas las reservas aplicando los filtros seleccionados.
     * También permite cargar el detalle de una reserva.
     */
    @GetMapping("/all")
    public String findAll(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) Integer detall,
            Model model) {

        // Obtiene las reservas y aplica los filtros indicados.
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

        // Envía los resultados y los filtros a la vista.
        model.addAttribute("reserves", reserves);
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("codi", codi);
        model.addAttribute("matricula", matricula);
        model.addAttribute("data", data);

        // Carga el detalle de la reserva seleccionada, si existe.
        if (detall != null) {
            Reserva seleccionada = reservaLogic.getReservaById(detall);
            if (seleccionada != null) {
                model.addAttribute("reservaSeleccionada", seleccionada);
            }
        }

        return "reserves-list";
    }

    // ==================== CREACIÓN DE RESERVAS ====================

    /**
     * Muestra el formulario para crear una nueva reserva.
     */
    @GetMapping("/new")
    public String getForm(Model model) {
        // Prepara una reserva con vehículo y usuario vacíos.
        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());

        model.addAttribute("reserva", reserva);

        // Carga los datos necesarios para los desplegables.
        carregarDadesFormulari(model);

        return "nova-reserva-agent";
    }

    /**
     * Busca vehículos disponibles según las fechas y la categoría seleccionadas.
     */
    @GetMapping("/disponibles")
    public String buscarDisponibles(
            @RequestParam String dataInici,
            @RequestParam String dataFi,
            @RequestParam(required = false) String categoria,
            Model model) {

        LocalDateTime inici;
        LocalDateTime fi;

        // Convierte las fechas recibidas.
        try {
            inici = LocalDateTime.parse(dataInici);
            fi = LocalDateTime.parse(dataFi);
        } catch (DateTimeParseException e) {
            // Devuelve el formulario con un mensaje si las fechas no son válidas.
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

        // Comprueba que la fecha final sea posterior a la inicial.
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

        // Busca vehículos activos, disponibles y sin reservas en ese intervalo.
        List<Vehicle> disponibles = vehicleLogic.findAllVehicles().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActiu()) && v.getEstatVehicle() == EstatVehicle.Disponible)
                .filter(v -> reservaLogic.vehicleDisponible(v.getId(), inici, fi))
                .filter(v -> categoria == null || categoria.isBlank()
                || v.getCategoria().name().equals(categoria))
                .toList();

        // Prepara la reserva con las fechas seleccionadas.
        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());
        reserva.setDataInici(inici);
        reserva.setDataFi(fi);

        // Envía los vehículos disponibles y los datos del formulario a la vista.
        model.addAttribute("reserva", reserva);
        model.addAttribute("vehicles", disponibles);
        model.addAttribute("senseDisponibilitat", disponibles.isEmpty());
        model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);
        model.addAttribute("categoria", categoria);

        return "nova-reserva-agent";
    }

    /**
     * Guarda una nueva reserva después de comprobar el estado del vehículo.
     */
    @PostMapping("/save")
    public String save(@ModelAttribute Reserva reserva,
            HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Comprueba que el vehículo seleccionado siga activo y disponible.
            if (reserva.getVehicle() != null && reserva.getVehicle().getId() > 0) {
                Vehicle v = vehicleLogic.getVehicleById(reserva.getVehicle().getId());
                if (v != null && (!Boolean.TRUE.equals(v.getActiu()) || v.getEstatVehicle() != EstatVehicle.Disponible)) {
                    redirectAttributes.addFlashAttribute("error", "Aquest vehicle està donat de baixa i no es pot reservar.");
                    return "redirect:" + RutaBase.obtenir(session) + "/reserves/new";
                }
            }

            // Guarda la reserva y muestra el resultado.
            reservaLogic.saveReserva(reserva);
            redirectAttributes.addFlashAttribute("missatge", "Reserva creada correctament.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/new";
        }

        return "redirect:" + RutaBase.obtenir(session) + "/reserves/all";
    }

    // ==================== DETALLE DE RESERVAS ====================

    /**
     * Redirige al listado mostrando el detalle de la reserva seleccionada
     * y manteniendo los filtros aplicados.
     */
    @GetMapping("/detall/{id}")
    public String details(@PathVariable Integer id,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            HttpSession session, RedirectAttributes attributes) {

        // Añade el identificador de la reserva seleccionada.
        attributes.addAttribute("detall", id);

        // Conserva los filtros que estaban aplicados.
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

    // ==================== ANULACIÓN DE RESERVAS ====================

    /**
     * Anula una reserva y envía un correo al cliente con los datos de la devolución.
     */
    @PostMapping("/cancelar/{id}")
    public String cancel(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "") String motiu,
            @RequestParam(required = false, defaultValue = "100") double percentatgeRetorn,
            HttpSession session, RedirectAttributes redirectAttributes) {

        // Obtiene la reserva seleccionada.
        Reserva reserva = reservaLogic.getReservaById(id);

        if (reserva != null && reservaLogic.anularReserva(id)) {

            // Calcula el importe que se devolverá al cliente.
            BigDecimal preuTotal = reserva.getPreuLloguer() != null ? reserva.getPreuLloguer() : BigDecimal.ZERO;
            BigDecimal importRetornat = preuTotal.multiply(BigDecimal.valueOf(percentatgeRetorn / 100.0));

            // Envía el correo de anulación si la reserva tiene un usuario asociado.
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

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Carga los vehículos activos y disponibles y todos los usuarios
     * necesarios para el formulario de reservas.
     */
    private void carregarDadesFormulari(Model model) {
        List<Vehicle> vehiclesActius = vehicleLogic.findAllVehicles().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActiu()) && v.getEstatVehicle() == EstatVehicle.Disponible)
                .toList();
        model.addAttribute("vehicles", vehiclesActius);
        model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
    }

    // ==================== ENTREGA DE VEHÍCULOS ====================

    /**
     * Busca reservas pendientes de entrega aplicando los filtros seleccionados.
     */
    @GetMapping("/lliurar")
    public String cercarPerLliurar(
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String dataInici,
            Model model) {

        // Busca las reservas que cumplen los filtros.
        List<Reserva> reservesPendents = reservaLogic.cercarReservesPerLliurar(codi, dni, email, matricula, dataInici);

        // Envía los resultados y los filtros a la vista.
        model.addAttribute("reserves", reservesPendents);
        model.addAttribute("codi", codi);
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("matricula", matricula);
        model.addAttribute("dataInici", dataInici);

        return "lliurar-vehicle-cerca";
    }

    /**
     * Muestra el formulario para verificar y confirmar la entrega del vehículo.
     */
    @GetMapping("/lliurar/{id}")
    public String formulariLliurar(@PathVariable Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Obtiene la reserva y comprueba que esté confirmada.
        Reserva reserva = reservaLogic.getReservaById(id);

        if (reserva == null || reserva.getEstat() != cat.copernic.Projecte1.entity.Enum.EstatReserva.Confirmada) {
            redirectAttributes.addFlashAttribute("error", "La reserva no existeix o no està pendent de lliurament.");
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/lliurar";
        }

        // Envía la reserva y la hora actual a la vista.
        model.addAttribute("reserva", reserva);
        model.addAttribute("ara", LocalDateTime.now().withSecond(0).withNano(0));

        return "lliurar-vehicle-form";
    }

    /**
     * Procesa la entrega del vehículo y guarda la información y las fotografías.
     */
    @PostMapping("/lliurar/{id}")
    public String processarLliurament(
            @PathVariable Integer id,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraLliurament,
            @RequestParam String descripcioLliurament,
            @RequestParam(name = "fotos", required = false) org.springframework.web.multipart.MultipartFile[] fotos,
            HttpSession session, RedirectAttributes redirectAttributes) {

        try {
            // Confirma la entrega y gestiona los posibles errores.
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

    // ==================== DEVOLUCIÓN DE VEHÍCULOS ====================

    /**
     * Busca reservas pendientes de devolución aplicando los filtros seleccionados.
     */
    @GetMapping("/retornar")
    public String cercarPerRetornar(
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String dataInici,
            Model model) {

        // Busca las reservas que cumplen los filtros.
        List<Reserva> reserves = reservaLogic.cercarReservesPerRetornar(
                codi, dni, email, matricula, dataInici);

        // Envía los resultados y los filtros a la vista.
        model.addAttribute("reserves", reserves);
        model.addAttribute("codi", codi);
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("matricula", matricula);
        model.addAttribute("dataInici", dataInici);

        return "retornar-vehicle-cerca";
    }

    /**
     * Muestra el formulario de devolución si la reserva está entregada.
     */
    @GetMapping("/retornar/{id}")
    public String formulariRetornar(
            @PathVariable Integer id,
            Model model,
            HttpSession session, RedirectAttributes redirectAttributes) {

        // Obtiene la reserva y comprueba que esté pendiente de devolución.
        Reserva reserva = reservaLogic.getReservaById(id);

        if (reserva == null
                || reserva.getEstat() != cat.copernic.Projecte1.entity.Enum.EstatReserva.Lliurada) {
            redirectAttributes.addFlashAttribute(
                    "error", "La reserva no existeix o no està pendent de devolució.");
            return "redirect:" + RutaBase.obtenir(session) + "/reserves/retornar";
        }

        // Envía la reserva y la hora actual a la vista.
        model.addAttribute("reserva", reserva);
        model.addAttribute("ara", LocalDateTime.now().withSecond(0).withNano(0));

        return "retornar-vehicle-form";
    }

    /**
     * Procesa la devolución del vehículo y registra su estado y observaciones.
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
            // Confirma la devolución y actualiza la información de la reserva.
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