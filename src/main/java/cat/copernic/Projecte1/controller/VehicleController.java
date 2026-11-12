package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.logic.VehicleLogic;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.Duration;

/**
 * CONTROLADOR WEB DE VEHÍCULOS
 *
 * {@code @Controller}: Marca la clase como un controlador de Spring MVC (devuelve
 * nombres de vistas HTML).
 * {@code @RequestMapping("/web/vehicles")}: Define la ruta base HTTP para todas las
 * URLs de este controlador.
 * {@code @Scope("request")}: Spring crea una instancia nueva de este controlador por
 * cada petición HTTP.
 */
@Controller
@RequestMapping("/web/vehicles")
@Scope("request")
public class VehicleController {

    // Logger para registrar eventos o errores en la consola/logs del servidor
    private final Logger logger = LoggerFactory.getLogger(VehicleController.class);

    // @Autowired: Inyección automática de la capa de lógica/negocio de vehículos
    @Autowired
    private VehicleLogic vehicleLogic;

    /**
     * MÉTODOS Y ENDPOINTS DE LA APLICACIÓN
     */
    /**
     * {@code @GetMapping("/all")}: Atiende peticiones GET a /web/vehicles/all
     *
     * ¿QUÉ HACE?: Muestra la lista de vehículos aplicando filtros opcionales de
     * búsqueda. ¿QUÉ CAPTURA?: Parámetros opcionales de filtro y la interfaz
     * Model para pasar datos a la plantilla. ¿DE DÓNDE LO SACA?: De la
     * URL/Query parameters enviados desde el formulario GET de
     * "vehicles-list.html". ¿QUÉ DEVUELVE?: La plantilla HTML "vehicles-list".
     */
    @GetMapping("/all")
    public String findAll(
            @RequestParam(name = "matricula", required = false) String matricula,
            @RequestParam(name = "marca", required = false) String marca,
            @RequestParam(name = "model", required = false) String modelParam,
            @RequestParam(name = "anyFabricacio", required = false) Integer anyFabricacio,
            @RequestParam(name = "categoria", required = false) Categoria categoria,
            @RequestParam(name = "dataInici", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInici, // Parsea fecha ISO (ej. yyyy-MM-ddTHH:mm)
            @RequestParam(name = "dataFi", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFi,
            Model model) {

        // Limpieza de espacios en blanco en los campos de texto recibidos
        String matriculaClean = (matricula != null) ? matricula.trim() : null;
        String marcaClean = (marca != null) ? marca.trim() : null;
        String modelClean = (modelParam != null) ? modelParam.trim() : null;

        logger.debug("Getting all vehicles with filters - Matrícula: {}, Marca: {}, Model: {}, Any: {}, Categoria: {}, Inici: {}, Fi: {}",
                matriculaClean, marcaClean, modelClean, anyFabricacio, categoria, dataInici, dataFi);

        // Consulta a la lógica de negocio pasándole todos los criterios de filtro
        List<Vehicle> llista = vehicleLogic.findByFiltres(
                matriculaClean,
                marcaClean,
                modelClean,
                anyFabricacio,
                categoria,
                dataInici,
                dataFi
        );

        // Inyección de variables en el modelo para que la vista HTML pueda usarlas
        model.addAttribute("vehicle", llista);
        model.addAttribute("matricula", matriculaClean);
        model.addAttribute("marca", marcaClean);
        model.addAttribute("modelParam", modelClean);
        model.addAttribute("anyFabricacio", anyFabricacio);
        model.addAttribute("categoriaSel", categoria);
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);

        return "vehicles-list"; // Carga el archivo templates/vehicles-list.html
    }

    /**
     * {@code @GetMapping("/cercar")}: Atiende peticiones GET a /web/vehicles/cercar
     *
     * ¿QUÉ HACE?: Busca vehículos disponibles exclusivamente entre un rango de
     * fechas e calcula los días de alquiler. ¿QUÉ CAPTURA?: dataInici, dataFi y
     * Model. ¿DE DÓNDE LO SACA?: Parámetros GET enviados desde el formulario de
     * reserva (ej. index.html). ¿QUÉ DEVUELVE?: La plantilla HTML "index" (con
     * lista de coches disponibles o error).
     */
    @GetMapping("/cercar")
    public String cercarVehicles(
            @RequestParam("dataInici")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInici,
            @RequestParam("dataFi")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFi,
            Model model, HttpSession session) {

        Usuaris usuariLoguejat = (Usuaris) session.getAttribute("usuariLoguejat");

        if (!dataFi.isAfter(dataInici)) {
            model.addAttribute("error", "La data final ha de ser posterior a la inicial.");
            return determinarVistaSegonsRol(usuariLoguejat, model);
        }

        // Obtención de vehículos libres en ese intervalo
        List<Vehicle> vehicles = vehicleLogic.findVehiclesDisponibles(dataInici, dataFi);

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("dataInici", dataInici);
        model.addAttribute("dataFi", dataFi);

        // Cálculo de días enteros de alquiler basándose en los minutos de diferencia entre fechas
        long diesLloguer = (long) Math.ceil(
                Duration.between(dataInici, dataFi).toMinutes() / 1440.0
        );

        model.addAttribute("diesLloguer", diesLloguer);

        return determinarVistaSegonsRol(usuariLoguejat, model);
    }

    // Mètode auxiliar per determinar quina plantilla carregar segons el rol de la sessió
    private String determinarVistaSegonsRol(Usuaris usuariLoguejat, Model model) {
        if (usuariLoguejat != null) {
            model.addAttribute("usuariLoguejat", usuariLoguejat);

            rol rolUsuari = (usuariLoguejat.getRol() != null)
                    ? rol.valueOf(usuariLoguejat.getRol().toString())
                    : rol.CLIENT;

            switch (rolUsuari) {
                case CLIENT:
                    return "index-client"; // O "index-logged"
                case ADMIN:
                    // TO DO: RETURN LA VISTA PERTINENT D'ADMIN
                    return "index-admin";
                case AGENT:
                    // TO DO: RETURN LA VISTA PERTINENT D'AGENT
                    return "admin-clients";
                default:
                    return "index";
            }
        }

        // Si no hi ha cap sessió oberta, mostrem la pàgina pública general
        return "index";
    }

    /**
     * {@code @GetMapping("/new")}: Atiende peticiones GET a /web/vehicles/new
     *
     * ¿QUÉ HACE?: Prepara un objeto Vehicle vacío por defecto para rellenarlo
     * en el formulario de alta. ¿QUÉ CAPTURA?: El objeto Model de Spring MVC.
     * ¿DE DÓNDE LO SACA?: Inyección automática del framework al entrar en la
     * URL. ¿QUÉ DEVUELVE?: La plantilla HTML "edit-vehicle".
     */
    @GetMapping("/new")
    public String getForm(Model model) {
        if (!model.containsAttribute("vehicle")) {
            Vehicle p = new Vehicle(0, "", "", "", 0, Categoria.SUV, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "");
            model.addAttribute("vehicle", p);
        }
        return "edit-vehicle";
    }

    /**
     * {@code @PostMapping("/save")}: Atiende peticiones POST a /web/vehicles/save
     *
     * ¿QUÉ HACE?: Procesa la recepción del formulario para crear o guardar
     * cambios de un vehículo. ¿QUÉ CAPTURA?: El objeto Vehicle relleno y un
     * contenedor RedirectAttributes para mensajes flash. ¿DE DÓNDE LO SACA?:
     * Los datos del formulario enviado mediante POST desde "edit-vehicle.html".
     * ¿QUÉ DEVUELVE?: Redirección HTTP a la lista de vehículos o de nuevo al
     * formulario si hay errores.
     */
    @PostMapping("/save")
    public String save(@ModelAttribute("vehicle") Vehicle vehicle, RedirectAttributes redirectAttributes) {
        // Comprueba si ya existe otro vehículo registrado con la misma matrícula
        if (vehicleLogic.existsByMatricula(vehicle.getMatricula(), vehicle.getId())) {
            redirectAttributes.addFlashAttribute("error", "La matrícula '" + vehicle.getMatricula() + "' ja està registrada al sistema!");
            redirectAttributes.addFlashAttribute("vehicle", vehicle);
            return (vehicle.getId() > 0) ? "redirect:/web/vehicles/edit/" + vehicle.getId() : "redirect:/web/vehicles/new";
        }

        try {
            vehicleLogic.saveVehicle(vehicle); // Valida y persiste en BD
            redirectAttributes.addFlashAttribute("missatge", "Vehicle desat correctament!");
        } catch (IllegalArgumentException e) {
            // Captura errores de validación de negocio (preus, fiança, camps obligatoris)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("vehicle", vehicle);
            return (vehicle.getId() > 0) ? "redirect:/web/vehicles/edit/" + vehicle.getId() : "redirect:/web/vehicles/new";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Captura errores lanzados directamente por la BD (restricciones UNIQUE, etc.)
            logger.error("Error de duplicitat a la base de dades", e);
            redirectAttributes.addFlashAttribute("error", "Error: La matrícula introduïda ja existeix.");
            return (vehicle.getId() > 0) ? "redirect:/web/vehicles/edit/" + vehicle.getId() : "redirect:/web/vehicles/new";
        }

        return "redirect:/web/vehicles/all";
    }

    /**
     * {@code @GetMapping("/delete/{id}")}: Atiende GET a /web/vehicles/delete/{id}
     *
     * ¿QUÉ HACE?: Elimina el vehículo correspondiente a la ID especificada en
     * la URL. ¿QUÉ CAPTURA?: El parámetro {id} de la ruta codificado como Long
     * y RedirectAttributes. ¿DE DÓNDE LO SACA?: De la URL dinámica llamada
     * desde el botón de eliminación de la lista. ¿QUÉ DEVUELVE?: Redirección a
     * /web/vehicles/all.
     */
    @GetMapping("/delete/{id}")
    public String deleteById(@PathVariable("id") Long vehicleId, RedirectAttributes redirectAttributes) {
        logger.debug("Intentant donar de baixa el vehicle " + vehicleId);

        // 1. Intentar la baixa lògica (comprova si té reserves actives i canvia l'estat)
        boolean donatDeBaixa = vehicleLogic.donarDeBaixaVehicle(vehicleId);

        if (donatDeBaixa) {
            redirectAttributes.addFlashAttribute("missatge", "El vehicle s'ha donat de baixa correctament (ja no està disponible).");
        } else {
            // Si retora false és perquè té reserves actives o no existeix
            if (vehicleLogic.teReservesActives(vehicleId)) {
                redirectAttributes.addFlashAttribute("error",
                        "No es pot donar de baixa el vehicle perquè està assignat a una reserva activa.");
            } else {
                redirectAttributes.addFlashAttribute("error", "No s'ha trobat el vehicle.");
            }
        }

        return "redirect:/web/vehicles/all";
    }

    /**
     * {@code @GetMapping("/edit/{id}")}: Atiende GET a /web/vehicles/edit/{id}
     *
     * ¿QUÉ HACE?: Busca un vehículo por ID y lo pasa a la vista para editar sus
     * datos. ¿QUÉ CAPTURA?: El parámetro {id} de la ruta y la interfaz Model.
     * ¿DE DÓNDE LO SACA?: De la URL dinámica pulsada en la columna "Accions"
     * del listado. ¿QUÉ DEVUELVE?: La plantilla "edit-vehicle" o redirección al
     * listado si no se encuentra.
     */
    @GetMapping("/edit/{id}")
    public String editById(@PathVariable("id") Long vehicleId, Model model) {
        Vehicle vehicle = vehicleLogic.getVehicleById(vehicleId);
        if (vehicle == null) {
            return "redirect:/web/vehicles/all";
        }
        model.addAttribute("vehicle", vehicle);
        return "edit-vehicle";
    }

    /**
     * {@code @GetMapping("/eliminar/{id}")}: Eliminación física (borrado de BD). Solo
     * funciona si el vehículo nunca ha sido reservado.
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarDefinitivament(@PathVariable("id") Long vehicleId, RedirectAttributes redirectAttributes) {
        logger.debug("Intentant eliminar físicament el vehicle " + vehicleId);

        boolean eliminat = vehicleLogic.eliminarVehicleDefinitivament(vehicleId);

        if (eliminat) {
            redirectAttributes.addFlashAttribute("missatge", "El vehicle s'ha eliminat definitivament de la base de dades.");
        } else {
            if (vehicleLogic.teAlgunaReserva(vehicleId)) {
                redirectAttributes.addFlashAttribute("error",
                        "No es pot eliminar el vehicle perquè ja té reserves registrades al seu historial. Només es pot donar de baixa.");
            } else {
                redirectAttributes.addFlashAttribute("error", "No s'ha trobat el vehicle.");
            }
        }

        return "redirect:/web/vehicles/all";
    }

}
