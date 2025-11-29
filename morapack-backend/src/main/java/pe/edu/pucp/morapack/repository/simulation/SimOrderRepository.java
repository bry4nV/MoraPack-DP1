package pe.edu.pucp.morapack.repository.simulation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.morapack.model.simulation.SimOrder;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for accessing simulation order data from moraTravelSimulation schema.
 * Supports both WEEKLY (date range) and COLLAPSE (all orders) scenarios.
 */
@Repository
public interface SimOrderRepository extends JpaRepository<SimOrder, Long> {

    /**
     * For WEEKLY scenario: Get orders within a specific date range.
     */
    @Query("SELECT o FROM SimOrder o WHERE " +
           "o.orderDate >= :startDate AND o.orderDate < :endDate " +
           "ORDER BY o.orderDate, o.orderTime")
    List<SimOrder> findOrdersInDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * For COLLAPSE scenario: Get ALL orders from a starting date onwards.
     */
    @Query("SELECT o FROM SimOrder o WHERE " +
           "o.orderDate >= :startDate " +
           "ORDER BY o.orderDate, o.orderTime")
    List<SimOrder> findAllOrdersFromDate(
        @Param("startDate") LocalDate startDate
    );

    /**
     * Alias for findOrdersInDateRange (used by DatabaseDataProvider).
     */
    @Query("SELECT o FROM SimOrder o WHERE " +
           "o.orderDate >= :startDate AND o.orderDate < :endDate " +
           "ORDER BY o.orderDate, o.orderTime")
    List<SimOrder> findOrdersBetweenDates(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get pending orders (status = 'PENDING').
     */
    @Query("SELECT o FROM SimOrder o WHERE " +
           "o.status = 'PENDING' " +
           "ORDER BY o.orderDate, o.orderTime")
    List<SimOrder> findPendingOrders();

    /**
     * Get the maximum order date in the database.
     */
    @Query("SELECT MAX(o.orderDate) FROM SimOrder o")
    LocalDate findMaxOrderDate();

    /**
     * Get the minimum order date in the database.
     */
    @Query("SELECT MIN(o.orderDate) FROM SimOrder o")
    LocalDate findMinOrderDate();
}
