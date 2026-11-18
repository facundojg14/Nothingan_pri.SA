/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cat.copernic.Projecte1.logic;

import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Servei que fa servir Spring Security per carregar l'usuari que intenta fer
 * login (RF80). Busca l'usuari per email i li dona el rol que té a la BD.
 *
 * @author Equip4
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarisRepository usuarisRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuaris usuari = usuarisRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuari no trobat: " + email));

        String nomRol = (usuari.getRol() != null) ? usuari.getRol().name() : "CLIENT";

        // Només poden entrar els usuaris validats. Si el registre està PENDENT,
        // REBUTJAT o de BAIXA, el compte queda desactivat i Spring Security
        // no el deixa entrar (veure SecurityConfig -> failureHandler).
        String estat = usuari.getEstatRegistre();
        boolean actiu = !("PENDENT".equalsIgnoreCase(estat)
                || "REBUTJAT".equalsIgnoreCase(estat)
                || "BAIXA".equalsIgnoreCase(estat));

        return new User(
                usuari.getEmail(),
                usuari.getPassword(),
                actiu, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + nomRol))
        );
    }
}