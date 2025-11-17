package pe.edu.pucp.morapack.repository.simulation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.morapack.model.simulation.SimShipment;

import java.util.List;

/**
 * Repositorio JPA para la tabla shipment del esquema moraTravelSimulation.
 * Se usa exclusivamente para operaciones de simulación con datos históricos.
 */
@Repository
public interface SimShipmentRepository extends JpaRepository<SimShipment, Long> {

    /**
     * Busca todos los shipments de un pedido específico
     */
    @Query("SELECT s FROM SimShipment s WHERE s.orderId = :orderId")
    List<SimShipment> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Cuenta cuántos shipments tiene un pedido
     */
    @Query("SELECT COUNT(s) FROM SimShipment s WHERE s.orderId = :orderId")
    Long countByOrderId(@Param("orderId") Long orderId);
}
