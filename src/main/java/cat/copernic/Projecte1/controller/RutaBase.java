package cat.copernic.Projecte1.controller;

import cat.copernic.Projecte1.entity.Enum.rol;
import cat.copernic.Projecte1.entity.Usuaris;
import jakarta.servlet.http.HttpSession;

/**
 * Ruta base de les pantalles internes segons el rol de l'usuari.
 *
 * L'administrador i l'agent fan servir les mateixes pantalles (reserves,
 * vehicles, clients, perfil...), però cadascun amb el seu prefix d'URL:
 * <ul>
 * <li>Administrador: {@code /web/admin/...}</li>
 * <li>Agent: {@code /web/agent/...}</li>
 * </ul>
 * La ruta base es guarda a la sessió quan l'usuari fa login (veure
 * SecurityConfig) amb el nom {@code rutaBase}. Les plantilles la fan servir
 * als enllaços i els controladors a les redireccions.
 *
 * @author Equip4
 */
public final class RutaBase {

    /** Nom de l'atribut de sessió on es guarda la ruta base. */
    public static final String ATRIBUT_SESSIO = "rutaBase";

    /** Ruta base de l'administrador. */
    public static final String ADMIN = "/web/admin";

    /** Ruta base de l'agent. */
    public static final String AGENT = "/web/agent";

    private RutaBase() {
    }

    /**
     * Retorna la ruta base de l'usuari que ha iniciat sessió.
     *
     * Si per algun motiu no està guardada a la sessió, la calcula a partir del
     * rol de l'usuari i la guarda.
     *
     * @param session sessió HTTP de l'usuari
     * @return "/web/admin" si és administrador, "/web/agent" en cas contrari
     */
    public static String obtenir(HttpSession session) {
        String ruta = (String) session.getAttribute(ATRIBUT_SESSIO);
        if (ruta == null) {
            Usuaris usuari = (Usuaris) session.getAttribute("usuariLoguejat");
            ruta = (usuari != null && usuari.getRol() == rol.ADMIN) ? ADMIN : AGENT;
            session.setAttribute(ATRIBUT_SESSIO, ruta);
        }
        return ruta;
    }

    /**
     * Retorna la ruta base que correspon a un rol.
     *
     * @param rolUsuari rol de l'usuari
     * @return "/web/admin" si és administrador, "/web/agent" en cas contrari
     */
    public static String perRol(rol rolUsuari) {
        return rolUsuari == rol.ADMIN ? ADMIN : AGENT;
    }
}
