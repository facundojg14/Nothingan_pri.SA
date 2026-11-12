package cat.copernic.Projecte1.config;

import cat.copernic.Projecte1.repository.UsuarisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Configuració de seguretat de l'aplicació (Spring Security).
 *
 * Defineix quines rutes són públiques i quines només poden veure l'admin,
 * l'agent o el client (RN53), el formulari de login amb usuari i contrasenya
 * (RN51) i què passa quan el login és correcte o falla (RF80).
 *
 * @author Equip4
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Logger per registrar els intents de login al fitxer de logs (RN04)
    private final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private UsuarisRepository usuarisRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                // 1. Rutes PÚBLIQUES (qualsevol persona, sense login)
                .requestMatchers(
                        "/",
                        "/home",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/web/usuaris/registre",
                        "/web/usuaris/login",
                        "/web/vehicles/cercar",              // Cerca pública de vehicles disponibles (pàgina d'inici)
                        "/web/usuaris/client/reserves/nova"  // Permet veure el formulari de nova reserva sense estar loguejat
                ).permitAll()

                // 2. Rutes protegides per Rol
                // Només ADMIN: panell d'administració i validar registres (RF82)
                .requestMatchers("/web/admin/**", "/web/usuaris/pendents", "/web/usuaris/validar/**").hasRole("ADMIN")
                // Només CLIENT: zona privada del client
                .requestMatchers("/web/usuaris/client/**").hasRole("CLIENT")
                // AGENT o ADMIN: gestió de reserves, vehicles i clients
                .requestMatchers(
                        "/web/reserves/**",
                        "/web/vehicles/**",
                        "/web/usuaris/all",
                        "/web/usuaris/new",
                        "/web/usuaris/edit/**",
                        "/web/usuaris/save",
                        "/web/usuaris/delete/**"
                ).hasAnyRole("AGENT", "ADMIN")
                // Les imatges pujades (DNI, llicències, fotos) són dades personals (RN53)
                .requestMatchers("/uploads/**").hasAnyRole("AGENT", "ADMIN")

                // 3. Qualsevol altra sol·licitud requereix estar autenticat
                .anyRequest().authenticated()
                )
                .formLogin(form -> form
                .loginPage("/web/usuaris/login")
                .loginProcessingUrl("/web/usuaris/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(customSuccessHandler())
                // Decideix el missatge segons el motiu (compte no actiu o credencials incorrectes)
                .failureHandler(customFailureHandler())
                .permitAll()
                )
                .logout(logout -> logout
                .logoutUrl("/web/usuaris/logout")
                .logoutSuccessUrl("/web/usuaris/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
                );

        return http.build();
    }

    // Handler personalitzat quan el login falla:
    // - Si el compte està desactivat (registre PENDENT, REBUTJAT o BAIXA) -> ?pendent
    // - Si l'email o la contrasenya són incorrectes -> ?error
    @Bean
    public AuthenticationFailureHandler customFailureHandler() {
        return (request, response, exception) -> {
            // Email que s'ha intentat fer servir (mai es guarda la contrasenya al log)
            String email = request.getParameter("email");

            if (exception instanceof DisabledException) {
                logger.warn("Login denegat, compte no actiu: {}", email);
                response.sendRedirect("/web/usuaris/login?pendent");
            } else {
                logger.warn("Login fallit: {}", email);
                response.sendRedirect("/web/usuaris/login?error");
            }
        };
    }

    // Handler personalitzat per decidir la ruta d'entrada segons el Rol
    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            String email = authentication.getName();

            usuarisRepository.findByEmail(email).ifPresent(usuari -> {
                request.getSession().setAttribute("usuariLoguejat", usuari);
            });

            var authorities = authentication.getAuthorities();
            String redirectUrl = "/";

            for (var authority : authorities) {
                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                    redirectUrl = "/web/admin/usuaris";
                    break;
                } else if (authority.getAuthority().equals("ROLE_AGENT")) {
                    redirectUrl = "/web/usuaris/all";
                    break;
                } else if (authority.getAuthority().equals("ROLE_CLIENT")) {
                    redirectUrl = "/web/usuaris/client/home";
                    break;
                }
            }

            // Només guardem el rol (ROLE_ADMIN, ROLE_AGENT o ROLE_CLIENT)
            String rol = authorities.stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("-");
            logger.info("Login correcte: {} ({})", email, rol);
            response.sendRedirect(redirectUrl);
        };
    }
}