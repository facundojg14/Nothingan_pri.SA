package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Proves unitàries (JUnit5 + Mockito) de la lògica de vehicles (RN22).
 *
 * No necessita base de dades: els repositoris són simulats (@Mock).
 *
 * Què prova:
 * <ol>
 * <li>donarDeBaixaVehicle: no es pot donar de baixa un vehicle que té
 * reserves (RF41) i no es guarda res.</li>
 * <li>donarDeBaixaVehicle: un vehicle sense reserves es dona de baixa de
 * forma lògica (estat Baixa i actiu = false), no s'esborra.</li>
 * <li>existsByMatricula: detecta una matrícula que ja té un altre vehicle,
 * però no ho considera duplicat si s'està editant el mateix vehicle.</li>
 * </ol>
 *
 * @author Facundo
 */
@ExtendWith(MockitoExtension.class)
public class VehicleLogicTest {

    @Mock
    private VehicleRepository vehicleRepo;

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private ReservaLogic reservaLogic;

    @InjectMocks
    private VehicleLogic vehicleLogic;

    @Test
    @DisplayName("1. donarDeBaixaVehicle: no es pot donar de baixa un vehicle que té reserves (RF41)")
    void testDonarDeBaixa_NoEsPotSiTeReserves() {
        // GIVEN: El vehicle 1 té reserves
        when(reservaRepo.existsByVehicle_Id(1L)).thenReturn(true);

        // WHEN
        boolean resultat = vehicleLogic.donarDeBaixaVehicle(1L);

        // THEN: No es dona de baixa i no es guarda res
        assertFalse(resultat);
        verify(vehicleRepo, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("2. donarDeBaixaVehicle: un vehicle sense reserves queda de baixa i inactiu")
    void testDonarDeBaixa_SenseReservesQuedaDeBaixa() {
        // GIVEN: El vehicle 2 existeix i no té reserves
        Vehicle vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setEstatVehicle(EstatVehicle.Disponible);
        vehicle.setActiu(true);

        when(reservaRepo.existsByVehicle_Id(2L)).thenReturn(false);
        when(vehicleRepo.findById(2L)).thenReturn(Optional.of(vehicle));

        // WHEN
        boolean resultat = vehicleLogic.donarDeBaixaVehicle(2L);

        // THEN: Baixa lògica (no s'esborra, es canvia l'estat)
        assertTrue(resultat);
        assertEquals(EstatVehicle.Baixa, vehicle.getEstatVehicle());
        assertFalse(vehicle.getActiu());
        verify(vehicleRepo, times(1)).save(vehicle);
    }

    @Test
    @DisplayName("3. existsByMatricula: detecta una matrícula que ja té un altre vehicle")
    void testExistsByMatricula_DetectaDuplicada() {
        // GIVEN: Ja existeix un vehicle (id 5) amb la matrícula 1234ABC
        Vehicle existent = new Vehicle();
        existent.setId(5L);
        existent.setMatricula("1234ABC");

        when(vehicleRepo.findByMatriculaIgnoreCase("1234ABC")).thenReturn(Optional.of(existent));

        // WHEN & THEN: Un vehicle nou (id 0) amb la mateixa matrícula és duplicat,
        // però si editem el mateix vehicle (id 5) no ho és
        assertTrue(vehicleLogic.existsByMatricula("1234ABC", 0));
        assertFalse(vehicleLogic.existsByMatricula("1234ABC", 5));
    }
}
