package cat.copernic.Projecte1.config;

import cat.copernic.Projecte1.controller.RutaBase;
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
 * Defineix quines rutes són públiques i quines només poden veure l'admin
 * ({@code /web/admin/**}), l'agent ({@code /web/agent/**}) o el client (RN53), el formulari de login amb usuari i contrasenya
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
                // Cada rol té el seu prefix. L'admin i l'agent fan servir les mateixes
                // pantalles (reserves, vehicles, clients, perfil), però cadascun amb la seva URL.
                // Només ADMIN: /web/admin/... (inclou agents, informes i validar registres)
                .requestMatchers("/web/admin/**").hasRole("ADMIN")
                // Només AGENT: /web/agent/...
                .requestMatchers("/web/agent/**").hasRole("AGENT")
                // Només CLIENT: zona privada del client
                .requestMatchers("/web/usuaris/client/**").hasRole("CLIENT")
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
                // Prefix de les URL internes segons el rol: /web/admin o /web/agent
                request.getSession().setAttribute(RutaBase.ATRIBUT_SESSIO, RutaBase.perRol(usuari.getRol()));
            });

            var authorities = authentication.getAuthorities();
            String redirectUrl = "/";

            for (var authority : authorities) {
                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                    redirectUrl = RutaBase.ADMIN + "/usuaris/all";
                    break;
                } else if (authority.getAuthority().equals("ROLE_AGENT")) {
                    redirectUrl = RutaBase.AGENT + "/usuaris/all";
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