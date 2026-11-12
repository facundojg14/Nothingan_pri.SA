package cat.copernic.Projecte1.config;

import cat.copernic.Projecte1.repository.UsuarisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarisRepository usuarisRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                // 1. Rutes PÚBLIQUES
                .requestMatchers(
                        "/", 
                        "/home", 
                        "/css/**", 
                        "/js/**", 
                        "/img/**", 
                        "/uploads/**", 
                        "/web/usuaris/registre", 
                        "/web/usuaris/login",
                        "/web/vehicles/**",                  // Permite ver y buscar vehículos públicamente
                        "/web/usuaris/client/reserves/nova"  // <- CORREGIDO: Permite ver el formulario de nueva reserva sin estar logueado
                ).permitAll()
                
                // 2. Rutes protegides per Rol
                .requestMatchers("/web/admin/**", "/web/usuaris/pendents").hasRole("ADMIN")
                .requestMatchers("/web/usuaris/client/**").hasRole("CLIENT")
                
                // 3. Qualsevol altra sol·licitud requereix estar autenticat
                .anyRequest().authenticated()
                )
                .formLogin(form -> form
                .loginPage("/web/usuaris/login")
                .loginProcessingUrl("/web/usuaris/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(customSuccessHandler())
                .failureUrl("/web/usuaris/login?error=true")
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

            response.sendRedirect(redirectUrl);
        };
    }
}