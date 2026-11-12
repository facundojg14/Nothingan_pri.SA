package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 *
 * @author Carolina
 */

@ExtendWith(MockitoExtension.class)
public class UsuarisLogicTest {
    
    @Mock
    private UsuarisRepository usuariRepo;

    @InjectMocks
    private UsuarisLogic usuarisLogic;

    private Usuaris nouClient;
    
    public UsuarisLogicTest() {
    }
    
    @BeforeEach
    void setUp() {
        nouClient = new Usuaris();
        nouClient.setNom("Laura Gómez");
        nouClient.setEmail("laura@example.com");
        nouClient.setPassword("123456");
        nouClient.setDni("12345678Z");
        nouClient.setTelefon("600112233");
        nouClient.setDataCaducitatDni(LocalDate.now().plusYears(2));
        nouClient.setDataCaducitatLlicencia(LocalDate.now().plusYears(5));
    }

    @Test
    @DisplayName("1. registrarClientWeb: Registra un client establint estat PENDENT, rol CLIENT i premium a false")
    void testRegistrarClientWeb_Exito() {
        // GIVEN: No existe otro usuario con el mismo email/DNI/teléfono en la BD
        when(usuariRepo.findByEmail("laura@example.com")).thenReturn(Optional.empty());
        when(usuariRepo.findByDni("12345678Z")).thenReturn(Optional.empty());
        when(usuariRepo.findByTelefon("600112233")).thenReturn(Optional.empty());

        // Simulamos la persistencia devolviendo el usuario con ID asignado = 5L
        when(usuariRepo.save(any(Usuaris.class))).thenAnswer(invocation -> {
            Usuaris u = invocation.getArgument(0);
            u.setId(5L);
            return u;
        });

        // WHEN: Ejecutamos el registro público del cliente web (RF81)
        Long idGenerat = usuarisLogic.registrarClientWeb(nouClient);

        // THEN: Verificamos valores asignados por defecto y persistencia
        assertNotNull(idGenerat);
        assertEquals(5L, idGenerat);
        assertEquals("PENDENT", nouClient.getEstatRegistre());
        assertEquals(rol.CLIENT, nouClient.getRol());
        assertFalse(nouClient.getPremium());

        verify(usuariRepo, times(1)).save(nouClient);
    }

    @Test
    @DisplayName("2. autenticarIValidarEstat: Denega l'accés si l'usuari té l'estat registre PENDENT")
    void testAutenticarIValidarEstat_ErrorEnEstatPendent() {
        // GIVEN: El cliente existe con la contraseña correcta pero su registro sigue PENDENT
        nouClient.setId(10L);
        nouClient.setEstatRegistre("PENDENT");

        when(usuariRepo.findByEmail("laura@example.com")).thenReturn(Optional.of(nouClient));

        // WHEN & THEN: Al intentar autenticarse debe lanzar IllegalStateException
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> usuarisLogic.autenticarIValidarEstat("laura@example.com", "123456")
        );

        assertTrue(exception.getMessage().contains("pendent de revisió"));
        verify(usuariRepo, times(1)).findByEmail("laura@example.com");
    }
}