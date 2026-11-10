package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;

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

    // Lógica de reservas inyectada para consultar la disponibilidad de fechas
    @Autowired
    private ReservaLogic reservaLogic;

    // Constructor explícito para inyección manual o de tests unitarios
    public VehicleLogic(VehicleRepository vehicleRepo) {
        this.vehicleRepo = vehicleRepo;
    }

    /**
     * MÉTODOS DE LA LÓGICA DE NEGOCIO
     */
    /**
     * Obte tots els vehicles de la base de dades.
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
            return vehicleRepo.findAll(); // Si no s'indica matrícula, retorna la llista completa com a base
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
        // Coincideix i NO és el mateix vehicle que estem editant (id != existing.getId)
        return existing.isPresent() && (id == 0 || existing.get().getId() != id);
    }

    /**
     * Filtratge combinat avançat (sense EstatVehicle)
     *
     * ¿QUÉ HACE?: Obtiene una lista base por matrícula y aplica filtros
     * sucesivos mediante Java Streams. ¿QUÉ CAPTURA?: Filtros de texto, año,
     * enum Categoria y rango de fechas. ¿DE DÓNDE LO SACA?: Parámetros pasados
     * desde el controlador. ¿QUÉ DEVUELVE?: Lista de vehículos procesada y
     * filtrada.
     */
    public List<Vehicle> findByFiltres(String matricula, String marca, String model,
            Integer anyFabricacio, Categoria categoria,
            LocalDateTime dataInici, LocalDateTime dataFi) {

        // 1. Obtiene candidatos por la matrícula
        List<Vehicle> base = findByMatricula(matricula);

        // Limpieza previa de textos a minúsculas
        String marcaClean = (marca != null && !marca.trim().isEmpty()) ? marca.trim().toLowerCase() : null;
        String modelClean = (model != null && !model.trim().isEmpty()) ? model.trim().toLowerCase() : null;
        boolean validarFechas = (dataInici != null && dataFi != null && dataFi.isAfter(dataInici));

        // 2. Filtra en memoria cada vehículo de la lista base
        return base.stream()
                // Filtro por marca (contiene el texto)
                .filter(v -> marcaClean == null || (v.getMarca() != null && v.getMarca().toLowerCase().contains(marcaClean)))
                // Filtro por modelo (contiene el texto)
                .filter(v -> modelClean == null || (v.getModel() != null && v.getModel().toLowerCase().contains(modelClean)))
                // Filtro por año de fabricación exacto
                .filter(v -> anyFabricacio == null || anyFabricacio <= 0 || anyFabricacio.equals(v.getAnyFabricacio()))
                // Filtro por categoría coincidente
                .filter(v -> categoria == null || (v.getCategoria() != null && v.getCategoria().name().equalsIgnoreCase(categoria.name())))
                // Filtro de disponibilidad: evalúa contra ReservaLogic si las fechas no chocan con reservas existentes
                .filter(v -> !validarFechas || reservaLogic.vehicleDisponible(v.getId(), dataInici, dataFi))
                .toList();
    }

    /**
     * Busca vehículos disponibles para las fechas seleccionadas.
     *
     * ¿QUÉ HACE?: Filtra en base de datos aquellos vehículos 'Disponibles' y
     * activos, comprobando luego la agenda de reservas. ¿QUÉ CAPTURA?:
     * dataInici y dataFi. ¿QUÉ DEVUELVE?: Lista de vehículos que se pueden
     * alquilar en esas fechas.
     */
    public List<Vehicle> findVehiclesDisponibles(
            LocalDateTime dataInici, LocalDateTime dataFi) {

        if (dataInici == null || dataFi == null || !dataFi.isAfter(dataInici)) {
            return List.of(); // Devuelve lista vacía si las fechas no son válidas
        }

        // Recupera de BD solo los marcados como EstatVehicle.Disponible y actiu = true
        return vehicleRepo.findByEstatVehicleAndActiuTrue(EstatVehicle.Disponible)
                .stream()
                // Comprueba que no tengan reservas reservadas/activadas en esas fechas
                .filter(v -> reservaLogic.vehicleDisponible(v.getId(), dataInici, dataFi))
                .toList();
    }

    // Comprovar si té reserves actives
    public boolean teReservesActives(Long vehicleId) {
        return vehicleRepo.tieneReservasActivas(vehicleId);
    }

// Donar de baixa el vehicle de forma segura
    public boolean donarDeBaixaVehicle(Long id) {
        // 1. Si té reserves actives, no es pot donar de baixa
        if (teReservesActives(id)) {
            return false;
        }

        // 2. Si no en té, procedim a la baixa lògica (o deshabilitar per a reserves)
        Vehicle vehicle = getVehicleById(id);
        if (vehicle != null) {
            vehicle.setActiu(false);
            vehicle.setEstatVehicle(cat.copernic.Projecte1.entity.Enum.EstatVehicle.Baixa);
            vehicle.setEstatVehicle(cat.copernic.Projecte1.entity.Enum.EstatVehicle.Desactivat);
            vehicle.setEstatVehicle(cat.copernic.Projecte1.entity.Enum.EstatVehicle.En_manteniment);// o l'estat que utilitzeu per a baixes
            vehicleRepo.save(vehicle);
            return true;
        }

        return false;
    }
}
