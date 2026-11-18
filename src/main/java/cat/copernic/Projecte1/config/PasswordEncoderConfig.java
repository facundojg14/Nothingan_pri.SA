/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuració de l'encriptador de contrasenyes.
 *
 * Crea el BCryptPasswordEncoder que fan servir el login i UsuarisLogic per
 * guardar i comprovar les contrasenyes de forma segura (RN52).
 *
 * @author Equip4
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Crea l'encriptador BCrypt que fa servir tota l'aplicació.
     *
     * @return encriptador de contrasenyes BCrypt
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
