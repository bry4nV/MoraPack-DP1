package pe.edu.pucp.morapack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Desactivamos CSRF (necesario para APIs REST)
            .csrf(csrf -> csrf.disable()) 
            
            // 2. Damos permiso a TODAS las peticiones (temporal)
            .authorizeHttpRequests(authz -> authz
                // .requestMatchers("/api/auth/**").permitAll() // (Pronto será así)
                // .anyRequest().authenticated()               // (Pronto será así)

                // Por ahora, para que tu app vuelva a funcionar:
                .requestMatchers("/**").permitAll() 
            );

        return http.build();
    }
}