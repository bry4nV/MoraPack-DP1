package pe.edu.pucp.morapack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.morapack.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Security usará este método para encontrar al usuario
    // por su "username" (que en nuestro caso es el email)
    Optional<User> findByEmail(String email);
    
}