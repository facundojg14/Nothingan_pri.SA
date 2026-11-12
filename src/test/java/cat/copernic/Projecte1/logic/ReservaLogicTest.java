package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import cat.copernic.Projecte1.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Proves unitàries (JUnit5 + Mockito) de la lògica de reserves (RN22).
 *
 * @author Alejandro
 */
@ExtendWith(MockitoExtension.class)
public class ReservaLogicTest {

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private VehicleRepository vehicleRepo;

    @Mock
    private UsuarisRepository usuarisRepo;

    @InjectMocks
    private ReservaLogic reservaLogic;

    private final LocalDateTime inici = LocalDateTime.of(2027, 3, 1, 10, 0);
    private final LocalDateTime fi = LocalDateTime.of(2027, 3, 5, 10, 0);

    @Test
    @DisplayName("1. saveReserva: no deixa crear una reserva si la data final és anterior a la inicial")
    void testSaveReserva_DataFiAnteriorDonaError() {
        // GIVEN: Una reserva amb les dates al revés
        Reserva reserva = new Reserva();
        reserva.setDataInici(fi);
        reserva.setDataFi(inici);

        // WHEN & THEN: Ha de llançar IllegalArgumentException i no guardar res
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservaLogic.saveReserva(reserva)
        );

        assertTrue(exception.getMessage().contains("data final"));
        verify(reservaRepo, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("2. vehicleDisponible: un vehicle desactivat no està disponible per reservar")
    void testVehicleDisponible_VehicleDesactivatNoDisponible() {
        // GIVEN: El vehicle existeix però no està actiu
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setActiu(false);

        when(vehicleRepo.findById(1L)).thenReturn(Optional.of(vehicle));

        // WHEN
        boolean disponible = reservaLogic.vehicleDisponible(1L, inici, fi);

        // THEN: No està disponible i ni tan sols es consulten les reserves
        assertFalse(disponible);
        verify(reservaRepo, never()).findReservesSolapades(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("3. anularReservaPerClient: un client no pot anul·lar la reserva d'un altre client")
    void testAnularReservaPerClient_ReservaDUnAltreClient() {
        // GIVEN: La reserva 7 pertany a l'usuari 2
        Usuaris propietari = new Usuaris();
        propietari.setId(2L);

        Reserva reserva = new Reserva();
        reserva.setId(7);
        reserva.setUsuari(propietari);
        reserva.setEstat(EstatReserva.Confirmada);

        when(reservaRepo.findById(7)).thenReturn(Optional.of(reserva));

        // WHEN & THEN: L'usuari 1 la intenta anul·lar i no ho pot fer
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaLogic.anularReservaPerClient(7, 1L)
        );

        assertEquals(EstatReserva.Confirmada, reserva.getEstat());
        verify(reservaRepo, never()).save(any(Reserva.class));
    }
}
