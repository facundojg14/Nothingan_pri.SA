package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Incidencies;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.IncidenciesRepository;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import cat.copernic.Projecte1.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contiene la lógica de negocio relacionada con las reservas.
 * Gestiona la disponibilidad, creación, anulación, entrega y devolución
 * de vehículos, además de los precios, las fianzas y las consultas.
 *
 * @author Equip4
 */
@Service
public class ReservaLogic {

    // Repositorios utilizados para consultar y guardar reservas, vehículos y usuarios.
    private final ReservaRepository reservaRepo;
    private final VehicleRepository vehicleRepo;
    private final UsuarisRepository usuarisRepo;

    // Registra las operaciones realizadas sobre las reservas.
    private final Logger logger = LoggerFactory.getLogger(ReservaLogic.class);

    // Días mínimos de antelación necesarios para devolver el importe completo.
    @Value("${app.reserves.dies-antelacio-devolucio:2}")
    private int diesAntelacioDevolucio;

    // Servicio encargado de enviar correos electrónicos.
    @Autowired
    private EmailService emailService;

    // Repositorio para consultar las incidencias de una reserva.
    @Autowired
    private IncidenciesRepository incidenciesRepo;

    // Servicio que calcula y gestiona las fianzas.
    private final FiancaService fiancaService;

    // Inyección de los repositorios y servicios necesarios.
    public ReservaLogic(
            ReservaRepository reservaRepo,
            VehicleRepository vehicleRepo,
            UsuarisRepository usuarisRepo,
            FiancaService fiancaService) {
        this.reservaRepo = reservaRepo;
        this.vehicleRepo = vehicleRepo;
        this.usuarisRepo = usuarisRepo;
        this.fiancaService = fiancaService;
    }

    // ==================== CONSULTAS BÁSICAS ====================

    // Obtiene todas las reservas registradas.
    public List<Reserva> findAllReservas() {
        return reservaRepo.findAll();
    }

    // Busca una reserva por su ID. Devuelve null si no existe.
    public Reserva getReservaById(Integer id) {
        return reservaRepo.findById(id).orElse(null);
    }

    // Obtiene todas las reservas de un usuario.
    public List<Reserva> getReservesPerUsuari(Long id) {
        // Si no hay identificador, devuelve una lista vacía.
        if (id == null) {
            return List.of();
        }
        return reservaRepo.findByUsuariId(id);
    }

    // Comprueba si existe una reserva con el ID indicado.
    public boolean existsById(Integer id) {
        return reservaRepo.existsById(id);
    }

    // ==================== DISPONIBILIDAD ====================

    /**
     * Comprueba si un vehículo puede reservarse entre dos fechas.
     * Comprueba que exista, esté activo, disponible y que no tenga
     * otra reserva que coincida con el periodo solicitado.
     */
    public boolean vehicleDisponible(
            Long vehicleId, LocalDateTime dataInici, LocalDateTime dataFi) {

        // Comprueba que el identificador y las fechas sean válidos.
        if (vehicleId == null || vehicleId <= 0
                || dataInici == null || dataFi == null
                || !dataFi.isAfter(dataInici)) {
            return false;
        }

        // Busca el vehículo en la base de datos.
        Vehicle vehicle = vehicleRepo.findById(vehicleId).orElse(null);

        // Comprueba que exista, esté activo y tenga estado Disponible.
        if (vehicle == null
                || !Boolean.TRUE.equals(vehicle.getActiu())
                || vehicle.getEstatVehicle() != EstatVehicle.Disponible) {
            return false;
        }

        // Busca reservas del mismo vehículo que coincidan con las fechas.
        // Las reservas anuladas no bloquean la disponibilidad.
        return reservaRepo.findByVehicle_IdAndEstatNotAndDataIniciLessThanAndDataFiGreaterThan(
                vehicleId, EstatReserva.Anulada, dataFi, dataInici).isEmpty();
    }

    // ==================== PREPARACIÓN DE RESERVA ====================

    /**
     * Prepara los datos de una reserva antes de mostrar la confirmación
     * al cliente. Calcula el precio del alquiler y la fianza mediante FiancaService.
     */
    public void prepararReservaClient(Reserva reserva, Vehicle vehicle, Usuaris usuari, long diesLloguer) {
        if (reserva == null || vehicle == null) {
            throw new IllegalArgumentException("La reserva i el vehicle són obligatoris.");
        }

        // Calcula la fianza inicial según las condiciones del vehículo y del usuario.
        BigDecimal fiancaBase = (vehicle != null && usuari != null)
                ? fiancaService.calcularFiancaInicial(vehicle, usuari)
                : BigDecimal.ZERO;

        if (fiancaBase == null || fiancaBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La fiança del vehicle no és vàlida.");
        }

        // Obtiene la fianza final aplicando las condiciones del servicio de fianzas.
        BigDecimal fiancaFinal = (usuari != null)
                ? fiancaService.calcularFiancaInicial(vehicle, usuari)
                : BigDecimal.ZERO;

        // Obtiene el precio diario del vehículo.
        BigDecimal preuDia = vehicle.getPreuDia() != null
                ? vehicle.getPreuDia() : BigDecimal.ZERO;

        // Calcula el precio total multiplicando el precio diario por los días.
        BigDecimal subtotalLloguer = preuDia
                .multiply(BigDecimal.valueOf(diesLloguer))
                .setScale(2, RoundingMode.HALF_UP);

        // Asigna los datos calculados al objeto reserva.
        reserva.setVehicle(vehicle);
        reserva.setUsuari(usuari);
        reserva.setPreuLloguer(subtotalLloguer);
        reserva.setImportFianca(fiancaFinal.setScale(2, RoundingMode.HALF_UP));
    }

    // ==================== CREACIÓN DE RESERVAS (RF50) ====================

    /**
     * Valida y guarda una nueva reserva.
     * Comprueba el usuario, el vehículo y las fechas, calcula el precio
     * y la fianza, genera el código de reserva y la guarda como Confirmada.
     */
    @Transactional
    public Integer saveReserva(Reserva reserva) {
        // Comprueba que se haya recibido una reserva.
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no pot ser nul·la.");
        }

        // Este método solo permite crear reservas nuevas, no modificar existentes.
        if (reserva.getId() != null) {
            throw new IllegalArgumentException("No es poden modificar reserves existents.");
        }

        // Comprueba que las fechas existan y que la fecha final sea posterior.
        if (reserva.getDataInici() == null
                || reserva.getDataFi() == null
                || !reserva.getDataFi().isAfter(reserva.getDataInici())) {
            throw new IllegalArgumentException("La data final ha de ser posterior a la inicial.");
        }

        // Evita reservas cuya fecha de inicio sea anterior al día actual.
        if (reserva.getDataInici().toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La data d'inici de la reserva no pot ser anterior a avui.");
        }

        // Comprueba que se hayan seleccionado un vehículo y un usuario válidos.
        if (reserva.getVehicle() == null
                || reserva.getUsuari() == null
                || reserva.getVehicle().getId() <= 0
                || reserva.getUsuari().getId() == null) {
            throw new IllegalArgumentException("Cal seleccionar un vehicle i un usuari vàlids.");
        }

        // Comprueba también que la hora de inicio no haya pasado.
        if (reserva.getDataInici().isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
            throw new IllegalArgumentException(
                    "No es pot fer una reserva amb una data d'inici passada."
            );
        }

        // Recupera los datos completos del vehículo y del usuario desde la base de datos.
        Vehicle vehicle = vehicleRepo.findById(reserva.getVehicle().getId()).orElse(null);
        Usuaris usuari = usuarisRepo.findById(reserva.getUsuari().getId()).orElse(null);

        if (vehicle == null || usuari == null) {
            throw new IllegalArgumentException("El vehicle o l'usuari seleccionat no existeix.");
        }

        // Comprueba que el vehículo esté activo y disponible.
        if (!Boolean.TRUE.equals(vehicle.getActiu())
                || vehicle.getEstatVehicle() != EstatVehicle.Disponible) {
            throw new IllegalArgumentException("El vehicle no està disponible per reservar.");
        }

        // Solo los usuarios validados pueden realizar reservas.
        if (!"VALIDAT".equalsIgnoreCase(usuari.getEstatRegistre())) {
            throw new IllegalArgumentException("L'usuari ha d'estar validat per fer una reserva.");
        }

        // Comprueba que el vehículo tenga un precio diario válido.
        if (vehicle.getPreuDia() == null
                || vehicle.getPreuDia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El vehicle no té un preu diari vàlid.");
        }

        // Comprueba que la fianza estándar del vehículo sea válida.
        BigDecimal fiancaBase = vehicle.getFiancaEstandar();

        if (fiancaBase == null || fiancaBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La fiança del vehicle no és vàlida.");
        }

        // Calcula la duración del alquiler y redondea hacia arriba los días incompletos.
        Duration durada = Duration.between(reserva.getDataInici(), reserva.getDataFi());
        long dies = durada.toDays();
        if (!durada.minusDays(dies).isZero()) {
            dies++;
        }

        // Calcula el precio total del alquiler.
        BigDecimal preuTotal = vehicle.getPreuDia()
                .multiply(BigDecimal.valueOf(dies))
                .setScale(2, RoundingMode.HALF_UP);

        reserva.setPreuLloguer(preuTotal);

        // Calcula la fianza teniendo en cuenta las condiciones del usuario.
        BigDecimal fianca = fiancaService.calcularFiancaInicial(vehicle, usuari);
        reserva.setImportFianca(fianca);

        // Genera un código secuencial RES-001, RES-002, etc., si no se ha proporcionado uno.
        if (reserva.getCodiReserva() == null || reserva.getCodiReserva().isBlank()) {
            int ultimoNumero = reservaRepo.findAll().stream()
                    .map(Reserva::getCodiReserva)
                    // Solo tiene en cuenta los códigos que siguen el formato RES-número.
                    .filter(c -> c != null && c.matches("RES-\\d+"))
                    .mapToInt(c -> Integer.parseInt(c.substring(4)))
                    .max()
                    .orElse(0);

            reserva.setCodiReserva(String.format("RES-%03d", ultimoNumero + 1));
        } else if (reservaRepo.existsByCodiReserva(reserva.getCodiReserva())) {
            throw new IllegalArgumentException("Aquest codi de reserva ja existeix.");
        }

        // Comprueba que no exista otra reserva activa que coincida con las fechas.
        List<Reserva> reserves
                = reservaRepo.findByVehicle_Id(vehicle.getId());

        for (Reserva existent : reserves) {
            // Las reservas anuladas no impiden reservar el vehículo.
            if (existent.getEstat() == EstatReserva.Anulada) {
                continue;
            }

            // Hay solapamiento si una reserva empieza antes de que termine la otra
            // y termina después de que la otra haya comenzado.
            boolean solapades = reserva.getDataInici().isBefore(existent.getDataFi())
                    && reserva.getDataFi().isAfter(existent.getDataInici());

            if (solapades) {
                throw new IllegalArgumentException("El vehicle ja està reservat en aquestes dates.");
            }
        }

        // Asigna los datos definitivos y establece la reserva como Confirmada.
        reserva.setVehicle(vehicle);
        reserva.setUsuari(usuari);
        reserva.setDataReserva(LocalDateTime.now());
        reserva.setEstat(EstatReserva.Confirmada);

        // Guarda la reserva y registra la operación en el log.
        Reserva guardada = reservaRepo.save(reserva);
        logger.info("Reserva {} creada per a {} (vehicle {})",
                guardada.getCodiReserva(), usuari.getEmail(), vehicle.getMatricula());

        // Devuelve el ID generado por la base de datos.
        return guardada.getId();
    }

    // ==================== ANULACIÓN POR AGENTE O ADMINISTRADOR (RF55) ====================

    /**
     * Permite anular una reserva desde el panel del agente o administrador.
     * No permite anular reservas que ya estén entregadas o anuladas.
     */
    @Transactional
    public boolean anularReserva(Integer id) {
        // Busca la reserva por su identificador.
        Reserva reserva = getReservaById(id);

        // Comprueba que exista y que se encuentre en un estado anulable.
        if (reserva == null
                || reserva.getEstat() == EstatReserva.Lliurada
                || reserva.getEstat() == EstatReserva.Anulada) {
            return false;
        }

        // Cambia el estado y guarda la reserva.
        reserva.setEstat(EstatReserva.Anulada);
        reservaRepo.save(reserva);

        logger.info("Reserva {} anul·lada per un agent/admin", reserva.getCodiReserva());
        return true;
    }

    // ==================== INFORMES (RF91) ====================

    /**
     * Obtiene las reservas para los informes.
     * Si recibe un cliente, muestra solo sus reservas; si no, muestra todas.
     */
    public List<Reserva> obtenirInformeReserves(Integer clientId) {
        // Sin cliente, obtiene todas las reservas ordenadas por nombre y fecha.
        if (clientId == null) {
            return reservaRepo.findAllByOrderByUsuari_NomAscDataIniciDesc();
        }

        // Con cliente, obtiene únicamente sus reservas ordenadas por fecha.
        return reservaRepo.findByUsuari_IdOrderByDataIniciDesc(clientId.longValue());
    }

    // ==================== ANULACIÓN POR EL CLIENTE (RF55) ====================

    /**
     * Permite al cliente anular una reserva propia.
     * Comprueba la antelación, calcula el importe que debe devolverse
     * y envía un correo electrónico de confirmación.
     */
    @Transactional
    public BigDecimal anularReservaPerClient(Integer idReserva, Long idUsuariLoguejat) {
        // Busca la reserva y comprueba que pertenezca al usuario conectado.
        Reserva reserva = getReservaById(idReserva);

        if (reserva == null || !reserva.getUsuari().getId().equals(idUsuariLoguejat)) {
            throw new IllegalArgumentException("La reserva no existeix o no pertany a aquest usuari.");
        }

        // No permite anular reservas entregadas o que ya estén anuladas.
        if (reserva.getEstat() == EstatReserva.Lliurada || reserva.getEstat() == EstatReserva.Anulada) {
            throw new IllegalArgumentException("Aquesta reserva no es pot anul·lar.");
        }

        // Cambia el estado de la reserva a Anulada.
        reserva.setEstat(EstatReserva.Anulada);

        // Calcula cuántos días faltan para el inicio de la reserva.
        LocalDate avui = LocalDate.now();
        LocalDate dataIniciReserva = reserva.getDataInici().toLocalDate();

        BigDecimal importRetornat = BigDecimal.ZERO;

        // Si se cumple la antelación mínima configurada, devuelve el alquiler y la fianza.
        if (!avui.plusDays(diesAntelacioDevolucio).isAfter(dataIniciReserva)) {
            BigDecimal totalLloguer = reserva.getPreuLloguer() != null ? reserva.getPreuLloguer() : BigDecimal.ZERO;
            BigDecimal fianca = reserva.getImportFianca() != null ? reserva.getImportFianca() : BigDecimal.ZERO;
            importRetornat = totalLloguer.add(fianca);
        }

        // Guarda la reserva con el nuevo estado.
        reservaRepo.save(reserva);

        // Calcula el porcentaje de devolución para incluirlo en el correo.
        double percentatgeRetorn = (importRetornat != null && importRetornat.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;

        // Envía al cliente un correo con los datos de la anulación y el importe devuelto.
        emailService.enviarEmailAnullacio(
                reserva.getUsuari().getEmail(),
                reserva.getUsuari().getNom(),
                reserva.getCodiReserva(),
                percentatgeRetorn,
                importRetornat,
                null
        );

        logger.info("Reserva {} anul·lada pel client. Import retornat: {} €", reserva.getCodiReserva(), importRetornat);
        return importRetornat;
    }

    // ==================== FILTROS DE RESERVAS DEL CLIENTE ====================

    /**
     * Filtra las reservas de un cliente por código, estado y fecha de inicio.
     * Los tres filtros son opcionales.
     */
    public List<Reserva> filtrarReservesClient(Long usuariId, String codi, String estat, String data) {
        // Obtiene primero las reservas del usuario.
        List<Reserva> reserves = getReservesPerUsuari(usuariId);

        // Aplica los filtros indicados. Si un filtro está vacío, no se aplica.
        return reserves.stream()
                // Busca coincidencias parciales en el código.
                .filter(r -> codi == null || codi.isBlank()
                || (r.getCodiReserva() != null && r.getCodiReserva().toLowerCase().contains(codi.trim().toLowerCase())))
                // Compara el estado sin distinguir mayúsculas y minúsculas.
                .filter(r -> estat == null || estat.isBlank()
                || (r.getEstat() != null && r.getEstat().name().equalsIgnoreCase(estat.trim())))
                // Compara la fecha de inicio exacta.
                .filter(r -> data == null || data.isBlank()
                || (r.getDataInici() != null && r.getDataInici().toLocalDate().toString().equals(data.trim())))
                .toList();
    }

    // ==================== BÚSQUEDA DE RESERVAS PARA ENTREGA ====================

    /**
     * Busca reservas pendientes de entrega.
     * Solo muestra reservas Confirmadas y permite filtrar por código,
     * DNI, correo electrónico, matrícula y fecha de inicio.
     */
    public List<Reserva> cercarReservesPerLliurar(String codi, String dni, String email, String matricula, String dataInici) {
        return reservaRepo.findAll().stream()
                // Solo incluye reservas que todavía están Confirmadas.
                .filter(r -> r.getEstat() == EstatReserva.Confirmada)
                .filter(r -> codi == null || codi.isBlank()
                || (r.getCodiReserva() != null && r.getCodiReserva().toLowerCase().contains(codi.trim().toLowerCase())))
                .filter(r -> dni == null || dni.isBlank()
                || (r.getUsuari() != null && r.getUsuari().getDni() != null && r.getUsuari().getDni().toLowerCase().contains(dni.trim().toLowerCase())))
                .filter(r -> email == null || email.isBlank()
                || (r.getUsuari() != null && r.getUsuari().getEmail() != null && r.getUsuari().getEmail().toLowerCase().contains(email.trim().toLowerCase())))
                .filter(r -> matricula == null || matricula.isBlank()
                || (r.getVehicle() != null && r.getVehicle().getMatricula() != null && r.getVehicle().getMatricula().toLowerCase().contains(matricula.trim().toLowerCase())))
                .filter(r -> dataInici == null || dataInici.isBlank()
                || (r.getDataInici() != null && r.getDataInici().toLocalDate().toString().equals(dataInici.trim())))
                .toList();
    }

    // ==================== ENTREGA DEL VEHÍCULO (RF48) ====================

    /**
     * Confirma la entrega del vehículo al cliente.
     * Guarda la fecha, la descripción y las fotografías adjuntas,
     * y actualiza los estados de la reserva y del vehículo.
     */
    @Transactional
    public void confirmarLliuramentVehicle(Integer reservaId,
            LocalDateTime dataHoraLliurament,
            String descripcioLliurament,
            org.springframework.web.multipart.MultipartFile[] fotos) throws java.io.IOException {

        // Busca la reserva que se quiere entregar.
        Reserva reserva = getReservaById(reservaId);

        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no existeix.");
        }

        // Solo se pueden entregar reservas Confirmadas.
        if (reserva.getEstat() != EstatReserva.Confirmada) {
            throw new IllegalArgumentException("Només es poden lliurar reserves en estat Confirmada.");
        }

        // Comprueba que se haya indicado la fecha y hora de entrega.
        if (dataHoraLliurament == null) {
            throw new IllegalArgumentException("La data i hora de lliurament són obligatòries.");
        }

        // Comprueba que se haya escrito una descripción del estado del vehículo.
        if (descripcioLliurament == null || descripcioLliurament.isBlank()) {
            throw new IllegalArgumentException("Cal introduir una descripció de l'estat del vehicle.");
        }

        // Recupera el vehículo asociado y comprueba que siga activo.
        Vehicle vehicle = reserva.getVehicle();
        if (vehicle == null || !Boolean.TRUE.equals(vehicle.getActiu())) {
            throw new IllegalArgumentException("El vehicle associat no està actiu.");
        }

        // Guarda las fotografías adjuntas por el agente, si existen.
        if (fotos != null && fotos.length > 0) {
            // Define la carpeta donde se almacenarán las fotografías.
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads/lliuraments");

            // Crea la carpeta si todavía no existe.
            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir);
            }

            for (org.springframework.web.multipart.MultipartFile foto : fotos) {
                if (foto != null && !foto.isEmpty()) {
                    // Genera un nombre único para evitar que se sobrescriban fotografías.
                    String originalName = foto.getOriginalFilename() != null ? foto.getOriginalFilename() : "foto.jpg";
                    String safeName = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    java.nio.file.Path filePath = uploadDir.resolve(safeName);

                    // Copia el archivo a la carpeta de entregas.
                    java.nio.file.Files.copy(foto.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    // Guarda la ruta de la fotografía en la reserva.
                    reserva.getFotosLliurament().add("/uploads/lliuraments/" + safeName);
                }
            }
        }

        // Actualiza los datos de entrega y cambia el estado de la reserva.
        reserva.setDataHoraLliurament(dataHoraLliurament);
        reserva.setDescripcioLliurament(descripcioLliurament.trim());
        reserva.setEstat(EstatReserva.Lliurada);

        // Cambia el estado del vehículo a Lliurat.
        vehicle.setEstatVehicle(EstatVehicle.Lliurat);

        // Guarda los cambios en ambos repositorios.
        vehicleRepo.save(vehicle);
        reservaRepo.save(reserva);
    }

    // ==================== BÚSQUEDA DE RESERVAS PARA DEVOLUCIÓN ====================

    /**
     * Busca reservas pendientes de devolución.
     * Solo muestra reservas Lliuradas y permite aplicar los mismos filtros
     * de búsqueda que en el proceso de entrega.
     */
    public List<Reserva> cercarReservesPerRetornar(
            String codi, String dni, String email, String matricula, String dataInici) {

        return reservaRepo.findAll().stream()
                // Solo incluye reservas cuyo vehículo ya se ha entregado.
                .filter(r -> r.getEstat() == EstatReserva.Lliurada)
                .filter(r -> codi == null || codi.isBlank()
                || (r.getCodiReserva() != null && r.getCodiReserva().toLowerCase().contains(codi.trim().toLowerCase())))
                .filter(r -> dni == null || dni.isBlank()
                || (r.getUsuari() != null && r.getUsuari().getDni() != null && r.getUsuari().getDni().toLowerCase().contains(dni.trim().toLowerCase())))
                .filter(r -> email == null || email.isBlank()
                || (r.getUsuari() != null && r.getUsuari().getEmail() != null && r.getUsuari().getEmail().toLowerCase().contains(email.trim().toLowerCase())))
                .filter(r -> matricula == null || matricula.isBlank()
                || (r.getVehicle() != null && r.getVehicle().getMatricula() != null && r.getVehicle().getMatricula().toLowerCase().contains(matricula.trim().toLowerCase())))
                .filter(r -> dataInici == null || dataInici.isBlank()
                || (r.getDataInici() != null && r.getDataInici().toLocalDate().toString().equals(dataInici.trim())))
                .toList();
    }

    // ==================== DEVOLUCIÓN DEL VEHÍCULO ====================

    /**
     * Confirma la devolución del vehículo.
     * Comprueba los datos de devolución, delega el cálculo de la fianza
     * en FiancaService y actualiza el estado final del vehículo.
     */
    @Transactional
    public void confirmarRetornVehicle(
            Integer reservaId,
            LocalDateTime dataHoraRetorn,
            String observacions,
            boolean desperfectesImputables,
            boolean vehiculoAveriado) {

        // Busca la reserva que se quiere devolver.
        Reserva reserva = getReservaById(reservaId);

        // Solo se pueden devolver reservas que estén en estado Lliurada.
        if (reserva == null || reserva.getEstat() != EstatReserva.Lliurada) {
            throw new IllegalArgumentException("La reserva no existeix o no està pendent de devolució.");
        }

        // Comprueba que la fecha de devolución sea válida y no sea anterior a la entrega.
        if (dataHoraRetorn == null
                || reserva.getDataHoraLliurament() == null
                || dataHoraRetorn.isBefore(reserva.getDataHoraLliurament())) {
            throw new IllegalArgumentException("La data de devolució no és vàlida.");
        }

        // Las observaciones del agente son obligatorias.
        if (observacions == null || observacions.isBlank()) {
            throw new IllegalArgumentException("Cal introduir les observacions de la devolució.");
        }

        // Recupera el vehículo asociado a la reserva.
        Vehicle vehicle = reserva.getVehicle();

        if (vehicle == null) {
            throw new IllegalArgumentException("La reserva no té cap vehicle associat.");
        }

        // Delega en FiancaService el cálculo de la fianza, los retrasos,
        // los posibles desperfectos y los cargos adicionales.
        fiancaService.processarRetornVehicle(reserva, dataHoraRetorn, desperfectesImputables);

        // Guarda las observaciones y establece el estado operativo final del vehículo.
        reserva.setObservacions(observacions.trim());
        vehicle.setEstatVehicle(vehiculoAveriado
                ? EstatVehicle.En_manteniment
                : EstatVehicle.Disponible);

        // Guarda los cambios del vehículo y de la reserva.
        vehicleRepo.save(vehicle);
        reservaRepo.save(reserva);

        // Registra en el log los importes relacionados con la devolución de la fianza.
        logger.info("Reserva {} retornada. Fiança retinguda: {} €, retornada: {} €, càrrec extra: {} €",
                reserva.getCodiReserva(),
                reserva.getFiancaRetinguda(),
                reserva.getFiancaRetornada(),
                reserva.getCarrecExtraPendent());
    }

    // ==================== INCIDENCIAS ====================

    // Obtiene las incidencias asociadas a una reserva concreta.
    public List<Incidencies> getIncidenciesPerReserva(Integer reservaId) {
        // Si no se proporciona un ID, devuelve una lista vacía.
        if (reservaId == null) {
            return List.of();
        }

        // Busca las incidencias relacionadas con el ID de la reserva.
        return incidenciesRepo.findByReserva_Id(reservaId);
    }
}