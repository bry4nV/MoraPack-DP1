package pe.edu.pucp.morapack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
// --- AuthenticationProvider y DaoAuthenticationProvider YA NO SE IMPORTAN ---
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.edu.pucp.morapack.repository.daily.UserRepository;

@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    public ApplicationConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Le dice a Spring Security cómo encontrar un usuario.
     * Spring Boot lo usará para crear el AuthenticationProvider automáticamente.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /**
     * Define el encriptador de contraseñas.
     * Spring Boot lo usará para crear el AuthenticationProvider automáticamente.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // --- ¡MÉTODO 'authenticationProvider' ELIMINADO! ---
    // Ya no es necesario, Spring Boot lo hace por nosotros.
    

    /**
     * Expone el "Gestor de Autenticación" para que podamos
     * usarlo en nuestro endpoint de login más adelante.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}