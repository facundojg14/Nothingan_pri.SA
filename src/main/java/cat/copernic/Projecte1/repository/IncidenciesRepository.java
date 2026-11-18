package cat.copernic.Projecte1.repository;

import cat.copernic.Projecte1.entity.Incidencies;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Carolina
 */
public interface IncidenciesRepository extends JpaRepository<Incidencies, Long> {
    List<Incidencies> findByReserva_Id(Integer reservaId);
}
