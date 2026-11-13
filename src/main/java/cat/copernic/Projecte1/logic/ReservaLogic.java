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
 * Lògica de negoci de les reserves.
 *
 * Comprova la disponibilitat dels vehicles, calcula el preu del lloguer i la
 * fiança (amb el descompte Premium), crea i anul·la reserves, lliura el
 * vehicle al client i fa les cerques i informes de reserves.
 *
 * @author Equip4
 */
@Service
public class ReservaLogic {

    private final ReservaRepository reservaRepo;
    private final VehicleRepository vehicleRepo;
    private final UsuarisRepository usuarisRepo;

    // Logger per registrar les accions de reserves al fitxer de logs (RN04)
    private final Logger logger = LoggerFactory.getLogger(ReservaLogic.class);

    @Value("${app.reserves.dies-antelacio-devolucio:2}")
    private int diesAntelacioDevolucio;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private IncidenciesRepository incidenciesRepo;

    private final FiancaService fiancaService;

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

    public List<Reserva> findAllReservas() {
        return reservaRepo.findAll();
    }

    public Reserva getReservaById(Integer id) {
        return reservaRepo.findById(id).orElse(null);
    }

    /**
     * Obté totes les reserves d'un usuari.
     *
     * @param id identificador de l'usuari
     * @return llista de reserves (buida si l'id és null)
     */
    public List<Reserva> getReservesPerUsuari(Long id) {
        if (id == null) {
            return List.of();
        }
        return reservaRepo.findByUsuariId(id);
    }

    public boolean existsById(Integer id) {
        return reservaRepo.existsById(id);
    }

    /**
     * Comprova si un vehicle es pot reservar entre dues dates.
     *
     * Un vehicle està disponible si existeix, està actiu, el seu estat és
     * Disponible i no té cap altra reserva (no anul·lada) que es solapi amb
     * aquestes dates.
     *
     * @param vehicleId identificador del vehicle
     * @param dataInici data i hora d'inici del lloguer
     * @param dataFi data i hora de finalització del lloguer
     * @return true si el vehicle es pot reservar; false en cas contrari o si
     * les dades no són vàlides
     */
    public boolean vehicleDisponible(
            Long vehicleId, LocalDateTime dataInici, LocalDateTime dataFi) {

        if (vehicleId == null || vehicleId <= 0
                || dataInici == null || dataFi == null
                || !dataFi.isAfter(dataInici)) {
            return false;
        }

        Vehicle vehicle = vehicleRepo.findById(vehicleId).orElse(null);

        if (vehicle == null
                || !Boolean.TRUE.equals(vehicle.getActiu())
                || vehicle.getEstatVehicle() != EstatVehicle.Disponible) {
            return false;
        }

        return reservaRepo.findReservesSolapades(
                vehicleId, dataInici, dataFi, EstatReserva.Anulada).isEmpty();
    }

    /**
     * Prepara un objeto Reserva calculando precios, fianzas (aplicando el 25%
     * si es Premium) y duraciones para la vista de confirmación del cliente.
     */
    public void prepararReservaClient(Reserva reserva, Vehicle vehicle, Usuaris usuari, long diesLloguer) {
        if (reserva == null || vehicle == null) {
            throw new IllegalArgumentException("La reserva i el vehicle són obligatoris.");
        }

        // 1. Calcular fianza base 
        BigDecimal fiancaBase = (vehicle != null && usuari != null)
                ? fiancaService.calcularFiancaInicial(vehicle, usuari)
                : BigDecimal.ZERO;

        if (fiancaBase == null || fiancaBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La fiança del vehicle no és vàlida.");
        }

        // Aplica el descuento Premium del 25 % a la fianza.
        BigDecimal fiancaFinal = (usuari != null)
                ? fiancaService.calcularFiancaInicial(vehicle, usuari)
                : BigDecimal.ZERO;

        BigDecimal preuDia = vehicle.getPreuDia() != null
                ? vehicle.getPreuDia() : BigDecimal.ZERO;

        BigDecimal subtotalLloguer = preuDia
                .multiply(BigDecimal.valueOf(diesLloguer))
                .setScale(2, RoundingMode.HALF_UP);

        reserva.setVehicle(vehicle);
        reserva.setUsuari(usuari);
        reserva.setPreuLloguer(subtotalLloguer);
        reserva.setImportFianca(fiancaFinal.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Crea una reserva nova (RF50).
     *
     * Comprova que les dates siguin correctes, que el vehicle estigui
     * disponible i actiu i que el client estigui validat. Calcula el preu del
     * lloguer (preu per dia per nombre de dies) i la fiança estàndard del
     * vehicle, amb un 25% de descompte si el client és Premium. Genera un codi
     * de reserva únic i deixa la reserva en estat Confirmada.
     *
     * @param reserva reserva amb el vehicle, el client i les dates
     * @return identificador de la reserva creada
     * @throws IllegalArgumentException si alguna dada no és correcta, el
     * vehicle no està disponible o ja està reservat en aquestes dates
     */
    @Transactional
    public Integer saveReserva(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no pot ser nul·la.");
        }

        if (reserva.getId() != null) {
            throw new IllegalArgumentException("No es poden modificar reserves existents.");
        }

        if (reserva.getDataInici() == null
                || reserva.getDataFi() == null
                || !reserva.getDataFi().isAfter(reserva.getDataInici())) {
            throw new IllegalArgumentException("La data final ha de ser posterior a la inicial.");
        }

        if (reserva.getVehicle() == null
                || reserva.getUsuari() == null
                || reserva.getVehicle().getId() <= 0
                || reserva.getUsuari().getId() == null) {
            throw new IllegalArgumentException("Cal seleccionar un vehicle i un usuari vàlids.");
        }

        Vehicle vehicle = vehicleRepo.findById(reserva.getVehicle().getId()).orElse(null);
        Usuaris usuari = usuarisRepo.findById(reserva.getUsuari().getId()).orElse(null);

        if (vehicle == null || usuari == null) {
            throw new IllegalArgumentException("El vehicle o l'usuari seleccionat no existeix.");
        }

        if (!Boolean.TRUE.equals(vehicle.getActiu())
                || vehicle.getEstatVehicle() != EstatVehicle.Disponible) {
            throw new IllegalArgumentException("El vehicle no està disponible per reservar.");
        }

        if (!"VALIDAT".equalsIgnoreCase(usuari.getEstatRegistre())) {
            throw new IllegalArgumentException("L'usuari ha d'estar validat per fer una reserva.");
        }

        if (vehicle.getPreuDia() == null
                || vehicle.getPreuDia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El vehicle no té un preu diari vàlid.");
        }

        BigDecimal fiancaBase = vehicle.getFiancaEstandar();
        
        if (fiancaBase == null || fiancaBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La fiança del vehicle no és vàlida.");
        }

        Duration durada = Duration.between(reserva.getDataInici(), reserva.getDataFi());
        long dies = durada.toDays();
        if (!durada.minusDays(dies).isZero()) {
            dies++;
        }

        BigDecimal preuTotal = vehicle.getPreuDia()
                .multiply(BigDecimal.valueOf(dies))
                .setScale(2, RoundingMode.HALF_UP);

        reserva.setPreuLloguer(preuTotal);

        BigDecimal fianca = fiancaService.calcularFiancaInicial(vehicle, usuari);
        reserva.setImportFianca(fianca);

        if (reserva.getCodiReserva() == null || reserva.getCodiReserva().isBlank()) {
            String codi;
            do {
                codi = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            } while (reservaRepo.existsByCodiReserva(codi));
            reserva.setCodiReserva(codi);
        } else if (reservaRepo.existsByCodiReserva(reserva.getCodiReserva())) {
            throw new IllegalArgumentException("Aquest codi de reserva ja existeix.");
        }

        // Evita reservas que coincidan en fechas para el mismo vehículo.
        List<Reserva> reserves
                = reservaRepo.findByVehicle_Id(vehicle.getId());

        for (Reserva existent : reserves) {
            if (existent.getEstat() == EstatReserva.Anulada) {
                continue;
            }
            boolean solapades = reserva.getDataInici().isBefore(existent.getDataFi())
                    && reserva.getDataFi().isAfter(existent.getDataInici());
            if (solapades) {
                throw new IllegalArgumentException("El vehicle ja està reservat en aquestes dates.");
            }
        }

        reserva.setVehicle(vehicle);
        reserva.setUsuari(usuari);
        reserva.setDataReserva(LocalDateTime.now());
        reserva.setEstat(EstatReserva.Confirmada);

        Reserva guardada = reservaRepo.save(reserva);
        logger.info("Reserva {} creada per a {} (vehicle {})",
                guardada.getCodiReserva(), usuari.getEmail(), vehicle.getMatricula());
        return guardada.getId();
    }

    /**
     * Anul·la una reserva des del panell de l'agent o de l'administrador
     * (RF55).
     *
     * Només es pot anul·lar si la reserva existeix i no està Lliurada ni ja
     * Anul·lada. En anul·lar-la, el vehicle torna a quedar lliure per a
     * aquestes dates.
     *
     * @param id identificador de la reserva
     * @return true si s'ha anul·lat; false si no existeix o no es pot anul·lar
     */
    @Transactional
    public boolean anularReserva(Integer id) {
        Reserva reserva = getReservaById(id);
        if (reserva == null
                || reserva.getEstat() == EstatReserva.Lliurada
                || reserva.getEstat() == EstatReserva.Anulada) {
            return false;
        }

        reserva.setEstat(EstatReserva.Anulada);
        reservaRepo.save(reserva);
        logger.info("Reserva {} anul·lada per un agent/admin", reserva.getCodiReserva());
        return true;
    }

    /**
     * Obté les reserves per a l'informe de reserves per client (RF91),
     * ordenades per client i per data (de la més recent a la més antiga).
     *
     * @param clientId identificador del client, o null per obtenir les de tots
     * @return llista de reserves de l'informe
     */
    public List<Reserva> obtenirInformeReserves(Integer clientId) {
        return reservaRepo.findReservesPerInforme(clientId);
    }

    /**
     * Anul·la una reserva feta pel mateix client (RF55).
     *
     * Si l'anul·lació es fa amb l'antelació mínima (paràmetre del sistema
     * app.reserves.dies-antelacio-devolucio), es retorna tot l'import pagat
     * (lloguer + fiança). Si no, no es retorna res. S'envia un correu al
     * client.
     *
     * @param idReserva identificador de la reserva
     * @param idUsuariLoguejat identificador del client que fa la petició
     * @return import que es retorna al client (0 si no hi ha retorn)
     * @throws IllegalArgumentException si la reserva no existeix, no és del
     * client o ja està lliurada o anul·lada
     */
    @Transactional
    public BigDecimal anularReservaPerClient(Integer idReserva, Long idUsuariLoguejat) {
        Reserva reserva = getReservaById(idReserva);

        if (reserva == null || !reserva.getUsuari().getId().equals(idUsuariLoguejat)) {
            throw new IllegalArgumentException("La reserva no existeix o no pertany a aquest usuari.");
        }

        if (reserva.getEstat() == EstatReserva.Lliurada || reserva.getEstat() == EstatReserva.Anulada) {
            throw new IllegalArgumentException("Aquesta reserva no es pot anul·lar.");
        }

        // 1. Canviar l'estat de la reserva i alliberar el vehicle
        reserva.setEstat(EstatReserva.Anulada);

        // 2. Comprovar antelació per al retorn de diners
        LocalDate avui = LocalDate.now();
        LocalDate dataIniciReserva = reserva.getDataInici().toLocalDate();

        BigDecimal importRetornat = BigDecimal.ZERO;

        // Si queden >= diesAntelacioDevolucio entre avui i la data d'inici, es retorna l'import complet (lloguer + fiança)
        if (!avui.plusDays(diesAntelacioDevolucio).isAfter(dataIniciReserva)) {
            BigDecimal totalLloguer = reserva.getPreuLloguer() != null ? reserva.getPreuLloguer() : BigDecimal.ZERO;
            BigDecimal fianca = reserva.getImportFianca() != null ? reserva.getImportFianca() : BigDecimal.ZERO;
            importRetornat = totalLloguer.add(fianca);
        }

        // Guardar canvis
        reservaRepo.save(reserva);

        // 3. Enviar correu electrònic d'anul·lació (passant els 6 paràmetres)
        double percentatgeRetorn = (importRetornat != null && importRetornat.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;

        emailService.enviarEmailAnullacio(
                reserva.getUsuari().getEmail(),
                reserva.getUsuari().getNom(),
                reserva.getCodiReserva(),
                percentatgeRetorn, // % de retorn (100 o 0)
                importRetornat, // Import calculat en BigDecimal
                null // Motiu buit quan ho fa el client
        );

        logger.info("Reserva {} anul·lada pel client. Import retornat: {} €", reserva.getCodiReserva(), importRetornat);
        return importRetornat;
    }

    /**
     * Busca les reserves d'un client aplicant filtres opcionals.
     *
     * @param usuariId identificador del client
     * @param codi part del codi de reserva (opcional)
     * @param estat estat de la reserva, per exemple Confirmada (opcional)
     * @param data data d'inici en format AAAA-MM-DD (opcional)
     * @return reserves del client que compleixen tots els filtres indicats
     */
    public List<Reserva> filtrarReservesClient(Long usuariId, String codi, String estat, String data) {
        List<Reserva> reserves = getReservesPerUsuari(usuariId);

        return reserves.stream()
                // Filtre per Codi (parcial)
                .filter(r -> codi == null || codi.isBlank()
                || (r.getCodiReserva() != null && r.getCodiReserva().toLowerCase().contains(codi.trim().toLowerCase())))
                // Filtre per Estat (compara com a text sense importar majúscules/minúscules)
                .filter(r -> estat == null || estat.isBlank()
                || (r.getEstat() != null && r.getEstat().name().equalsIgnoreCase(estat.trim())))
                // Filtre per Data (coincidència exacta de la data d'inici YYYY-MM-DD)
                .filter(r -> data == null || data.isBlank()
                || (r.getDataInici() != null && r.getDataInici().toLocalDate().toString().equals(data.trim())))
                .toList();
    }

    /**
     * Cerca reserves pendents de lliurament (Estat = Confirmada) aplicant
     * filtres opcionals.
     */
    public List<Reserva> cercarReservesPerLliurar(String codi, String dni, String email, String matricula, String dataInici) {
        return reservaRepo.findAll().stream()
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

    /**
     * Confirma el lliurament del vehicle al client, guarda les fotos de
     * desperfectes (si n'hi ha) i actualitza l'estat de la reserva a Lliurada i
     * del vehicle a Lliurat.
     */
    @Transactional
    public void confirmarLliuramentVehicle(Integer reservaId,
            LocalDateTime dataHoraLliurament,
            String descripcioLliurament,
            org.springframework.web.multipart.MultipartFile[] fotos) throws java.io.IOException {

        Reserva reserva = getReservaById(reservaId);
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no existeix.");
        }

        if (reserva.getEstat() != EstatReserva.Confirmada) {
            throw new IllegalArgumentException("Només es poden lliurar reserves en estat Confirmada.");
        }

        if (dataHoraLliurament == null) {
            throw new IllegalArgumentException("La data i hora de lliurament són obligatòries.");
        }

        if (descripcioLliurament == null || descripcioLliurament.isBlank()) {
            throw new IllegalArgumentException("Cal introduir una descripció de l'estat del vehicle.");
        }

        Vehicle vehicle = reserva.getVehicle();
        if (vehicle == null || !Boolean.TRUE.equals(vehicle.getActiu())) {
            throw new IllegalArgumentException("El vehicle associat no està actiu.");
        }

        // Guardar les fotos si l'agent n'ha adjuntat
        if (fotos != null && fotos.length > 0) {
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads/lliuraments");
            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir);
            }

            for (org.springframework.web.multipart.MultipartFile foto : fotos) {
                if (foto != null && !foto.isEmpty()) {
                    String originalName = foto.getOriginalFilename() != null ? foto.getOriginalFilename() : "foto.jpg";
                    String safeName = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    java.nio.file.Path filePath = uploadDir.resolve(safeName);
                    java.nio.file.Files.copy(foto.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    reserva.getFotosLliurament().add("/uploads/lliuraments/" + safeName);
                }
            }
        }

        // Actualitzar dades de lliurament i estats
        reserva.setDataHoraLliurament(dataHoraLliurament);
        reserva.setDescripcioLliurament(descripcioLliurament.trim());
        reserva.setEstat(EstatReserva.Lliurada);

        vehicle.setEstatVehicle(EstatVehicle.Lliurat);
        vehicleRepo.save(vehicle);
        reservaRepo.save(reserva);
    }
    
    /**
     * Busca reservas pendientes de devolución aplicando filtros opcionales.
     */
    public List<Reserva> cercarReservesPerRetornar(
            String codi, String dni, String email, String matricula, String dataInici) {

        return reservaRepo.findAll().stream()
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
    
    /**
     * Confirma la devolución, calcula la fianza y actualiza el estado del vehículo.
     */
    @Transactional
    public void confirmarRetornVehicle(
            Integer reservaId,
            LocalDateTime dataHoraRetorn,
            String observacions,
            boolean desperfectesImputables,
            boolean vehiculoAveriado) {

        Reserva reserva = getReservaById(reservaId);

        if (reserva == null || reserva.getEstat() != EstatReserva.Lliurada) {
            throw new IllegalArgumentException("La reserva no existeix o no està pendent de devolució.");
        }

        if (dataHoraRetorn == null
                || reserva.getDataHoraLliurament() == null
                || dataHoraRetorn.isBefore(reserva.getDataHoraLliurament())) {
            throw new IllegalArgumentException("La data de devolució no és vàlida.");
        }

        if (observacions == null || observacions.isBlank()) {
            throw new IllegalArgumentException("Cal introduir les observacions de la devolució.");
        }

        Vehicle vehicle = reserva.getVehicle();

        if (vehicle == null) {
            throw new IllegalArgumentException("La reserva no té cap vehicle associat.");
        }

        // Deleguem el càlcul complet a FiancaService (hores de retard arrodonides cap amunt,
        // retenció del 100% si hi ha desperfectes imputables, càrrec extra i actualització d'estats)
        fiancaService.processarRetornVehicle(reserva, dataHoraRetorn, desperfectesImputables);

        // Afegim les observacions de l'agent i l'estat operatiu final del vehicle (Disponible o En_manteniment)
        reserva.setObservacions(observacions.trim());
        vehicle.setEstatVehicle(vehiculoAveriado
                ? EstatVehicle.En_manteniment
                : EstatVehicle.Disponible);

        vehicleRepo.save(vehicle);
        reservaRepo.save(reserva);

        logger.info("Reserva {} retornada. Fiança retinguda: {} €, retornada: {} €, càrrec extra: {} €",
                reserva.getCodiReserva(),
                reserva.getFiancaRetinguda(),
                reserva.getFiancaRetornada(),
                reserva.getCarrecExtraPendent());
        }

    /**
     * Obté la llista d'incidències associades a una reserva concreta.
     * @param reservaId ID de reserva
     * @return incidències associades a la reserva concreta.
     */
    public List<Incidencies> getIncidenciesPerReserva(Integer reservaId) {
        if (reservaId == null) {
            return List.of();
        }
        return incidenciesRepo.findByReserva_Id(reservaId);
    }
}