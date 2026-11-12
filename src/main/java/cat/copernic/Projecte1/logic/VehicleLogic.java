package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * CAPA DE LÓGICA DE NEGOCIO (SERVICE) DE VEHÍCULOS
 *
 * @Service: Indica a Spring que esta clase almacena reglas de negocio y lógica
 * de aplicación.
 */
@Service
public class VehicleLogic {

    // Repositorio JPA inyectado para realizar operaciones en BD de Vehículos
    @Autowired
    private VehicleRepository vehicleRepo;

    // Logger per registrar les baixes de vehicles al fitxer de logs (RN04)
    private final Logger logger = LoggerFactory.getLogger(VehicleLogic.class);

    // Repositorio de reservas inyectado para comprobar reservas asociadas
    @Autowired
    private ReservaRepository reservaRepo;

    // Lógica de reservas inyectada para consultar la disponibilidad de fechas
    @Autowired
    private ReservaLogic reservaLogic;

    public VehicleLogic(VehicleRepository vehicleRepo, ReservaRepository reservaRepo, ReservaLogic reservaLogic) {
        this.vehicleRepo = vehicleRepo;
        this.reservaRepo = reservaRepo;
        this.reservaLogic = reservaLogic;
    }

    // Comprovar si el vehicle té reserves
    public boolean teReservesActives(Long vehicleId) {
        if (vehicleId == null) {
            return false;
        }
        // Utiliza el método que tenemos declarado en ReservaRepository
        return reservaRepo.existsByVehicle_Id(vehicleId);
    }

    // Método de baja lógica
    public boolean donarDeBaixaVehicle(Long id) {
        // 1. Si té reserves actives, no es pot donar de baixa
        if (teReservesActives(id)) {
            logger.warn("No es pot donar de baixa el vehicle {}: té reserves", id);
            return false;
        }

        // 2. Canvi d'estat a la base de dades (baixa lògica)
        Vehicle vehicle = getVehicleById(id);
        if (vehicle != null) {
            vehicle.setActiu(false);
            vehicle.setEstatVehicle(EstatVehicle.Baixa); // O EstatVehicle.NoDisponible si teniu aquest valor a l'Enum
            vehicleRepo.save(vehicle);
            logger.info("Vehicle {} donat de baixa", vehicle.getMatricula());
            return true;
        }

        return false;
    }

    /**
     * MÉTODOS DE LA LÓGICA DE NEGOCIO
     */
    /**
     * Obté tots els vehicles de la base de dades.
     *
     * @return List<Vehicle> Llista completa.
     */
    public List<Vehicle> findAllVehicles() {
        return vehicleRepo.findAll();
    }

    /**
     * Desa un vehicle (Crea un de nou o actualitza si ja té ID).
     *
     * @param vehicle Obres del vehicle a desar.
     * @return Long ID del vehicle desat.
     */
    public Long saveVehicle(Vehicle vehicle) {
        Vehicle ret = vehicleRepo.save(vehicle);
        return ret.getId();
    }

    /**
     * Elimina un vehicle de la base de dades a partir del seu ID.
     *
     * @param id Identificador únic del vehicle.
     */
    public void deleteVehicleById(Long id) {
        vehicleRepo.deleteById(id);
    }

    /**
     * Cerca un vehicle per la seva ID.
     *
     * @param id Identificador a cercar.
     * @return Vehicle trobat o null si no existeix.
     */
    public Vehicle getVehicleById(Long id) {
        return vehicleRepo.findById(id).orElse(null);
    }

    /**
     * Cerca vehicles que continguin una determinada matrícula.
     *
     * @param matricula Cadena de text amb la matrícula.
     * @return List<Vehicle> Llista de coincidències o tots si la cerca està
     * buida.
     */
    public List<Vehicle> findByMatricula(String matricula) {
        if (matricula == null || matricula.trim().isEmpty()) {
            return vehicleRepo.findAll();
        }
        return vehicleRepo.findByMatriculaContainingIgnoreCase(matricula.trim());
    }

    /**
     * Comprova si la matrícula ja existeix en un altre vehicle de la base de
     * dades.
     *
     * @param matricula Matrícula a verificar.
     * @param id ID del vehicle actual (0 si és alta nova).
     * @return true si la matrícula ja pertany a un altre vehicle diferent al
     * que s'edita.
     */
    public boolean existsByMatricula(String matricula, long id) {
        if (matricula == null || matricula.trim().isEmpty()) {
            return false;
        }
        Optional<Vehicle> existing = vehicleRepo.findByMatriculaIgnoreCase(matricula.trim());
        return existing.isPresent() && (id == 0 || existing.get().getId() != id);
    }

    /**
     * Filtratge combinat avançat
     */
    public List<Vehicle> findByFiltres(String matricula, String marca, String model,
            Integer anyFabricacio, Categoria categoria,
            LocalDateTime dataInici, LocalDateTime dataFi) {

        List<Vehicle> base = findByMatricula(matricula);

        String marcaClean = (marca != null && !marca.trim().isEmpty()) ? marca.trim().toLowerCase() : null;
        String modelClean = (model != null && !model.trim().isEmpty()) ? model.trim().toLowerCase() : null;
        boolean validarFechas = (dataInici != null && dataFi != null && dataFi.isAfter(dataInici));

        return base.stream()
                .filter(v -> marcaClean == null || (v.getMarca() != null && v.getMarca().toLowerCase().contains(marcaClean)))
                .filter(v -> modelClean == null || (v.getModel() != null && v.getModel().toLowerCase().contains(modelClean)))
                .filter(v -> anyFabricacio == null || anyFabricacio <= 0 || anyFabricacio.equals(v.getAnyFabricacio()))
                .filter(v -> categoria == null || (v.getCategoria() != null && v.getCategoria().name().equalsIgnoreCase(categoria.name())))
                .filter(v -> !validarFechas || reservaLogic.vehicleDisponible(v.getId(), dataInici, dataFi))
                .toList();
    }

    /**
     * Busca vehicles disponibles per a les dates seleccionades.
     */
    public List<Vehicle> findVehiclesDisponibles(
            LocalDateTime dataInici, LocalDateTime dataFi) {

        if (dataInici == null || dataFi == null || !dataFi.isAfter(dataInici)) {
            return List.of();
        }

        return vehicleRepo.findByEstatVehicleAndActiuTrue(EstatVehicle.Disponible)
                .stream()
                .filter(v -> reservaLogic.vehicleDisponible(v.getId(), dataInici, dataFi))
                .toList();
    }
}
