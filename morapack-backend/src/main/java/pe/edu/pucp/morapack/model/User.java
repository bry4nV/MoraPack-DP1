package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
// Usamos comillas "`user`" porque "user" es una palabra reservada en SQL
@Table(name = "`user`") 
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "second_last_name")
    private String secondLastName;

    // Este será nuestro "username" para el login
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    // Mapeamos el Enum
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    // --- Constructores ---
    public User() {}
    
    // --- Getters y Setters (puedes autogenerarlos) ---
    // (Omitidos por brevedad, pero asegúrate de tenerlos para 
    //  id, firstName, lastName, secondLastName, email, password, y role)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getSecondLastName() { return secondLastName; }
    public void setSecondLastName(String secondLastName) { this.secondLastName = secondLastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    // NOTA: No incluyas un 'getAuthorities' o 'getUsername' aquí,
    // los implementaremos de UserDetails abajo.
    
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    

    // --- MÉTODOS DE UserDetails (El "Contrato" de Spring Security) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Le dice a Spring que el rol de este usuario es (ej: "ROLE_ADMIN")
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        // Le dice a Spring cuál es la contraseña
        return this.password;
    }

    @Override
    public String getUsername() {
        // Le dice a Spring cuál es el "username" (usamos el email)
        return this.email;
    }

    // --- Métodos de estado de la cuenta ---
    // Como tu tabla no tiene estas columnas, simplemente
    // devolvemos 'true' para indicar que las cuentas siempre están activas.

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}