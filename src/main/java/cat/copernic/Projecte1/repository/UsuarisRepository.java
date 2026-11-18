/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cat.copernic.Projecte1.repository;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Maxna
 */
public interface UsuarisRepository extends JpaRepository<Usuaris, Long> {

    //Optional ESTRICTO List PARCIAL
    // Método necesario para comprobar correos duplicados (RF09)
    // hace una llamada a la bbdd donde le pasa el atributo entre parentesis y busca las coincidencias
    //con los usuarios y devuelve una lista de usuarios con sus atributos y todo lo que contengan  
    Optional<Usuaris> findByEmail(String email);

    // Busqueda Dni parcial (sale dni exacto o equivalentes)
    // hace una llamada a la bbdd donde le pasa el atributo entre parentesis y busca las coincidencias
    //con los usuarios y devuelve un usuario con sus atributos y todo lo que contengan  
    List<Usuaris> findByDniContainingIgnoreCase(String dni);

    List<Usuaris> findByEmailContainingIgnoreCase(String email);

    List<Usuaris> findByEstatRegistre(String estatRegistre);

    List<Usuaris> findByRol(rol rol);

    Optional<Usuaris> findByTelefon(String telefon);

    Optional<Usuaris> findByDni(String dni);

    /**
     * SQL Generado: Consulta combinada filtrando por email, DNI, nombre y
     * teléfono (LIKE). Son campos obligatorios, nunca son null, así que un
     * filtro vacío ("") los encuentra todos. La nacionalidad y la población
     * pueden ser null y se filtran en UsuarisLogic.filtrarClients.
     */
    List<Usuaris> findByEmailContainingIgnoreCaseAndDniContainingIgnoreCaseAndNomContainingIgnoreCaseAndTelefonContaining(
            String email,
            String dni,
            String nom,
            String telefon
    );

}
