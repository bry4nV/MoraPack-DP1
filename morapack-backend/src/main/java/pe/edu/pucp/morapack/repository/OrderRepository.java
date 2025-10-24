package pe.edu.pucp.morapack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.morapack.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // add query methods if needed
}
