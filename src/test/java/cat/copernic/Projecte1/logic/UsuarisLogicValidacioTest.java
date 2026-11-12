package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Proves unitàries (JUnit5 + Mockito) de les validacions d'usuaris i de
 * l'encriptació de contrasenyes (RN20, RN52 i RN22).
 *
 * No necessita base de dades: el repositori i l'encriptador són simulats
 * (@Mock).
 *
 * Què prova:
 * <ol>
 * <li>validarCamposUnicos: un DNI amb lletres entre els números no és
 * vàlid.</li>
 * <li>registrarClientWeb: no es pot registrar un client sense contrasenya i
 * no es guarda res a la BD.</li>
 * <li>saveUsuari: la contrasenya es guarda encriptada amb BCrypt (no en text
 * pla).</li>
 * <li>saveUsuari: una contrasenya que ja està encriptada no es torna a
 * encriptar (ex: quan s'edita el perfil).</li>
 * </ol>
 *
 * @author Denis
 */
@ExtendWith(MockitoExtension.class)
public class UsuarisLogicValidacioTest {

    @Mock
    private UsuarisRepository usuariRepo;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarisLogic usuarisLogic;

    private Usuaris usuari;

    @BeforeEach
    void setUp() {
        usuari = new Usuaris();
        usuari.setNom("Marc Puig");
        usuari.setEmail("marc@example.com");
        usuari.setPassword("1234");
        usuari.setDni("12345678Z");
        usuari.setTelefon("611223344");
        usuari.setDataCaducitatDni(LocalDate.now().plusYears(2));
    }

    @Test
    @DisplayName("1. validarCamposUnicos: un DNI amb lletres al mig no és vàlid")
    void testValidarDni_FormatIncorrecte() {
        // GIVEN: DNI amb una lletra entre els números
        usuari.setDni("1234X678Z");

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarisLogic.validarCamposUnicos(usuari)
        );

        assertTrue(exception.getMessage().contains("DNI"));
    }

    @Test
    @DisplayName("2. registrarClientWeb: no es pot registrar un client sense contrasenya")
    void testRegistrarClientWeb_SenseContrasenya() {
        // GIVEN: El client no ha posat contrasenya
        usuari.setPassword("");

        // WHEN & THEN: Error i no es guarda a la BD
        assertThrows(
                IllegalArgumentException.class,
                () -> usuarisLogic.registrarClientWeb(usuari)
        );

        verify(usuariRepo, never()).save(any(Usuaris.class));
    }

    @Test
    @DisplayName("3. saveUsuari: la contrasenya es guarda encriptada amb BCrypt")
    void testSaveUsuari_EncriptaLaContrasenya() {
        // GIVEN: L'encriptador retorna un hash de BCrypt
        when(passwordEncoder.encode("1234")).thenReturn("$2a$10$hashDeProva");
        when(usuariRepo.save(any(Usuaris.class))).thenAnswer(invocation -> {
            Usuaris u = invocation.getArgument(0);
            u.setId(3L);
            return u;
        });

        // WHEN
        usuarisLogic.saveUsuari(usuari);

        // THEN: La contrasenya ja no està en text pla
        assertEquals("$2a$10$hashDeProva", usuari.getPassword());
        verify(passwordEncoder, times(1)).encode("1234");
    }

    @Test
    @DisplayName("4. saveUsuari: una contrasenya ja encriptada no es torna a encriptar")
    void testSaveUsuari_NoEncriptaDuesVegades() {
        // GIVEN: L'usuari ja té la contrasenya encriptada (ex: editar perfil)
        usuari.setPassword("$2a$10$jaEncriptada");
        when(usuariRepo.save(any(Usuaris.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        usuarisLogic.saveUsuari(usuari);

        // THEN: Es manté igual
        assertEquals("$2a$10$jaEncriptada", usuari.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }
}
