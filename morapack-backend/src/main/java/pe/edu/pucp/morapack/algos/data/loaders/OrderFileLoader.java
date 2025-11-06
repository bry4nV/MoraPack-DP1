package pe.edu.pucp.morapack.algos.data.loaders;

import pe.edu.pucp.morapack.model.DynamicOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cargador de pedidos dinámicos programados desde archivo.
 * 
 * Formato del archivo:
 * # dynamic_orders_2025_12.txt
 * # Formato: DIA.HH:MM,ORIGEN,DESTINO,CANTIDAD,DEADLINE_HH
 * 01.14:30,SPIM,EBCI,250,48
 * 03.08:00,SEQM,LTFM,150,72
 * 
 * DIA: 01-07 (día de la semana simulada)
 * HH:MM: Hora de inyección del pedido
 * CANTIDAD: Número de productos
 * DEADLINE_HH: Deadline en horas (48 o 72)
 */
public class OrderFileLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderFileLoader.class);
    
    /**
     * Carga pedidos dinámicos programados desde un archivo.
     * 
     * @param filePath Ruta del archivo de pedidos
     * @param startDate Fecha de inicio de la simulación
     * @return Lista de pedidos programados
     */
    public static List<DynamicOrder> loadOrders(String filePath, LocalDate startDate) {
        List<DynamicOrder> orders = new ArrayList<>();
        
        logger.info("📁 Cargando pedidos dinámicos desde: {}", filePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Saltar líneas vacías y comentarios
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                try {
                    DynamicOrder order = parseOrderLine(line, startDate);
                    orders.add(order);
                    
                    logger.debug("✅ Línea {}: {}", lineNumber, order);
                    
                } catch (Exception e) {
                    logger.error("❌ Error en línea {}: {} - {}", lineNumber, line, e.getMessage());
                }
            }
            
            logger.info("✅ Cargados {} pedidos dinámicos programados", orders.size());
            
        } catch (IOException e) {
            logger.error("❌ Error leyendo archivo de pedidos: {}", e.getMessage());
        }
        
        return orders;
    }
    
    /**
     * Parsea una línea del archivo de pedidos dinámicos.
     * 
     * Formato: 01.14:30,SPIM,EBCI,250,48
     * 
     * @param line Línea a parsear
     * @param startDate Fecha de inicio de la simulación
     * @return Pedido dinámico programado
     */
    private static DynamicOrder parseOrderLine(String line, LocalDate startDate) {
        String[] parts = line.split(",");
        
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                "Formato inválido. Esperado: DIA.HH:MM,ORIGEN,DESTINO,CANTIDAD,DEADLINE_HH"
            );
        }
        
        // 1. Parsear momento de inyección (DIA.HH:MM)
        String[] timeParts = parts[0].split("\\.");
        if (timeParts.length != 2) {
            throw new IllegalArgumentException("Formato de tiempo inválido: " + parts[0]);
        }
        
        int dayOfWeek = Integer.parseInt(timeParts[0]);          // 01-07
        String injectTime = timeParts[1];                        // HH:MM
        
        // Validar día
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Día inválido: " + dayOfWeek + " (debe ser 01-07)");
        }
        
        // Calcular fecha de inyección
        LocalDate injectionDate = startDate.plusDays(dayOfWeek - 1);
        LocalTime injectionLocalTime = LocalTime.parse(injectTime);
        LocalDateTime injectionTime = LocalDateTime.of(injectionDate, injectionLocalTime);
        
        // 2. Parsear datos del pedido
        String origin = parts[1].trim();
        String destination = parts[2].trim();
        int quantity = Integer.parseInt(parts[3].trim());
        int deadlineHours = Integer.parseInt(parts[4].trim());
        
        // 3. Validaciones
        if (origin.isEmpty() || destination.isEmpty()) {
            throw new IllegalArgumentException("Origen y destino no pueden estar vacíos");
        }
        
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("Origen y destino deben ser diferentes");
        }
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser mayor a 0: " + quantity);
        }
        
        if (deadlineHours != 48 && deadlineHours != 72) {
            throw new IllegalArgumentException("Deadline debe ser 48 o 72 horas: " + deadlineHours);
        }
        
        // 4. Crear pedido dinámico
        String reason = String.format("Pedido urgente programado día %d", dayOfWeek);
        
        return new DynamicOrder(
            origin,
            destination,
            quantity,
            deadlineHours,
            injectionTime,
            reason
        );
    }
    
    /**
     * Valida que un archivo de pedidos exista y sea legible.
     */
    public static boolean validateFile(String filePath) {
        java.io.File file = new java.io.File(filePath);
        
        if (!file.exists()) {
            logger.error("❌ Archivo no encontrado: {}", filePath);
            return false;
        }
        
        if (!file.canRead()) {
            logger.error("❌ Archivo no legible: {}", filePath);
            return false;
        }
        
        logger.info("✅ Archivo de pedidos válido: {}", filePath);
        return true;
    }
    
    /**
     * Método auxiliar para crear un archivo de ejemplo.
     */
    public static String generateExampleFile() {
        return """
            # dynamic_orders_2025_12.txt
            # Formato: DIA.HH:MM,ORIGEN,DESTINO,CANTIDAD,DEADLINE_HH
            # DIA: 01-07 (día de la semana)
            # HH:MM: Hora de inyección
            # CANTIDAD: Número de productos
            # DEADLINE_HH: 48 o 72 horas
            
            # Ejemplo: Inyectar el día 1 a las 14:30 un pedido SPIM→EBCI de 250 unidades con 48h deadline
            01.14:30,SPIM,EBCI,250,48
            
            # Pedidos de prueba
            01.16:00,SBGR,LATI,180,72
            03.08:00,SEQM,LTFM,150,72
            03.20:30,SPIM,EBCI,320,48
            05.10:15,LTFM,SEQM,200,48
            05.16:45,SBGR,LATI,300,48
            07.09:00,SCLS,SPIM,175,72
            """;
    }
}

