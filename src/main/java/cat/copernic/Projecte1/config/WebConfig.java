package cat.copernic.Projecte1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuració web.
 *
 * Fa que els fitxers guardats a la carpeta "uploads" del projecte es puguin
 * veure des del navegador a l'adreça /uploads/nom_del_fitxer.
 *
 * @author Equip4
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
