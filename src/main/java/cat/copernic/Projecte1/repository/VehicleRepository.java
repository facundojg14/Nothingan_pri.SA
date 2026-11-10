package cat.copernic.Projecte1.repository;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * REPOSITORIO DE VEHÍCULOS (CAPA DE ACCESO A DATOS - DAO)
 *
 * JpaRepository<Vehicle, Long>: Spring Data JPA genera automáticamente las
 * consultas SQL para operaciones CRUD sobre la entidad Vehicle (cuyo ID es tipo
 * Long).
 */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * SQL Generado: SELECT * FROM vehicle WHERE LOWER(matricula) LIKE
     * LOWER('%:matricula%') ¿QUÉ HACE?: Busca vehículos cuyo campo matrícula
     * contenga la cadena proporcionada (sin importar mayúsculas).
     */
    List<Vehicle> findByMatriculaContainingIgnoreCase(String matricula);

    /**
     * SQL Generado: SELECT * FROM vehicle WHERE LOWER(matricula) =
     * LOWER(:matricula) ¿QUÉ HACE?: Busca una coincidencia exacta de matrícula
     * ignorando mayúsculas. ¿QUÉ DEVUELVE?: Un Optional con el objeto o vacío
     * si no se encuentra.
     */
    Optional<Vehicle> findByMatriculaIgnoreCase(String matricula);

    /**
     * SQL Generado: SELECT * FROM vehicle WHERE estat_vehicle = :estatVehicle
     * AND actiu = true ¿QUÉ HACE?: Devuelve todos los vehículos que tienen un
     * estado determinado y están activos en el sistema.
     */
    List<Vehicle> findByEstatVehicleAndActiuTrue(EstatVehicle estatVehicle);

    /**
     * SQL Generado: Consulta combinada filtrando por matrícula (LIKE), marca
     * (LIKE), categoría y estado.
     */
    List<Vehicle> findByMatriculaContainingIgnoreCaseAndMarcaContainingIgnoreCaseAndCategoriaAndEstatVehicle(
            String matricula,
            String marca,
            Categoria categoria,
            EstatVehicle estatVehicle
    );
    
    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.vehicle.id = :vehicleId AND r.estat <> cat.copernic.Projecte1.entity.Enum.EstatReserva.ANULLADA")
    boolean tieneReservasActivas(@Param("vehicleId") Long vehicleId);
}
