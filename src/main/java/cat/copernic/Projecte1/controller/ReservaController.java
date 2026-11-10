package cat.copernic.Projecte1.controller;

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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * Controlador de reservas de agentes.
 * Permite listar y filtrar reservas, consultar detalles,
 * buscar vehículos disponibles, crear reservas y anularlas.
 */
@Controller
@RequestMapping("/web/reserves")
@RequestScope
public class ReservaController {
    
    // Clases que contienen las operaciones para gestionar los datos.
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

    // Muestra las reservas aplicando los filtros seleccionados.
    @GetMapping("/all")
    public String findAll(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) Integer detall,
            Model model) {
        
        // Obtenemos todas las reservas y filtramos según los datos introducidos.
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
        
        // Enviamos las reservas y los filtros a la página.
        model.addAttribute("reserves", reserves);
        model.addAttribute("dni", dni);
        model.addAttribute("email", email);
        model.addAttribute("codi", codi);
        model.addAttribute("matricula", matricula);
        model.addAttribute("data", data);
        
        // Si se ha seleccionado una reserva, también enviamos sus detalles.
        if (detall != null) {
            Reserva seleccionada = reservaLogic.getReservaById(detall);
            if (seleccionada != null) {
                model.addAttribute("reservaSeleccionada", seleccionada);
            }
        }

        return "reserves-list";
    }

    // Muestra el formulario para crear una reserva.
    @GetMapping("/new")
    public String getForm(Model model) {
        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());

        model.addAttribute("reserva", reserva);
        
        // Cargamos los vehículos y usuarios de los desplegables.
        carregarDadesFormulari(model);

        return "nova-reserva-agent";
    }

    // Busca vehículos disponibles por fechas y categoría.
    @GetMapping("/disponibles")
    public String buscarDisponibles(
            @RequestParam String dataInici,
            @RequestParam String dataFi,
            @RequestParam(required = false) String categoria,
            Model model) {

        LocalDateTime inici;
        LocalDateTime fi;
        
        // Convertimos las fechas al formato que utiliza Java.
        try {
            inici = LocalDateTime.parse(dataInici);
            fi = LocalDateTime.parse(dataFi);
        } catch (DateTimeParseException e) {
            
            // Si las fechas no son válidas, mostramos un error.
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
        
        // Comprobamos que la fecha final sea posterior a la inicial.
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
        
        // Filtra los vehículos disponibles por fechas y categoría.
        List<Vehicle> disponibles = vehicleLogic.findAllVehicles().stream()
                .filter(v -> reservaLogic.vehicleDisponible(
                        v.getId(), inici, fi))
                .filter(v -> categoria == null || categoria.isBlank()
                        || v.getCategoria().name().equals(categoria))
                .toList();
        
        // Preparamos la reserva con las fechas seleccionadas.
        Reserva reserva = new Reserva();
        reserva.setVehicle(new Vehicle());
        reserva.setUsuari(new Usuaris());
        reserva.setDataInici(inici);
        reserva.setDataFi(fi);

        // Enviamos los resultados a la página.
        model.addAttribute("reserva", reserva);
        model.addAttribute("vehicles", disponibles);
        model.addAttribute("senseDisponibilitat", disponibles.isEmpty());
        model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);
        model.addAttribute("categoria", categoria);

        return "nova-reserva-agent";
    }

    // Recibe y guarda los datos del formulario.
    @PostMapping("/save")
    public String save(@ModelAttribute Reserva reserva,
            RedirectAttributes redirectAttributes) {
        try {
            reservaLogic.saveReserva(reserva);
            
            // Mensaje que se muestra si la reserva se crea correctamente.
            redirectAttributes.addFlashAttribute(
                    "missatge", "Reserva creada correctament.");
        } catch (IllegalArgumentException e) {
            
            // Si hay un error, volvemos al formulario con el mensaje.
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/reserves/new";
        }

        return "redirect:/web/reserves/all";
    }

    // Selecciona una reserva para mostrar sus detalles sin perder los filtros.
    @GetMapping("/detall/{id}")
    public String details(@PathVariable Integer id,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String codi,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String data,
            RedirectAttributes attributes) {

        attributes.addAttribute("detall", id);

        // Conservamos los filtros que ya había aplicado el usuario.
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

        return "redirect:/web/reserves/all";
    }

    @PostMapping("/cancelar/{id}")
    public String cancel(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "") String motiu,
            @RequestParam(required = false, defaultValue = "100") double percentatgeRetorn,
            RedirectAttributes redirectAttributes) {

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
        
        return "redirect:/web/reserves/all";
    }

    // Mètode privat afegit per carregar dades dels desplegables al formulari
    private void carregarDadesFormulari(Model model) {
        model.addAttribute("vehicles", vehicleLogic.findAllVehicles());
        model.addAttribute("usuaris", usuarisLogic.findAllUsuaris());
    }
}