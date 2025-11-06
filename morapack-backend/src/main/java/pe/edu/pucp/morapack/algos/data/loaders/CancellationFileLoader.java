package pe.edu.pucp.morapack.algos.data.loaders;

import pe.edu.pucp.morapack.model.FlightCancellation;
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
 * Cargador de cancelaciones programadas desde archivo.
 * 
 * Formato del archivo:
 * # cancellations_2025_12.txt
 * # Formato: DIA.HH:MM,ORIGEN,DESTINO,HORA_VUELO
 * 01.06:00,SPIM,SEQM,03:34
 * 03.14:30,SBGR,SCLS,08:15
 * 
 * DIA: 01-07 (día de la semana simulada)
 * HH:MM: Hora de cancelación
 * HORA_VUELO: Hora programada del vuelo (HH:mm)
 */
public class CancellationFileLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CancellationFileLoader.class);
    
    /**
     * Carga cancelaciones programadas desde un archivo.
     * 
     * @param filePath Ruta del archivo de cancelaciones
     * @param startDate Fecha de inicio de la simulación
     * @return Lista de cancelaciones programadas
     */
    public static List<FlightCancellation> loadCancellations(String filePath, LocalDate startDate) {
        List<FlightCancellation> cancellations = new ArrayList<>();
        
        logger.info("📁 Cargando cancelaciones desde: {}", filePath);
        
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
                    FlightCancellation cancellation = parseCancellationLine(line, startDate);
                    cancellations.add(cancellation);
                    
                    logger.debug("✅ Línea {}: {}", lineNumber, cancellation);
                    
                } catch (Exception e) {
                    logger.error("❌ Error en línea {}: {} - {}", lineNumber, line, e.getMessage());
                }
            }
            
            logger.info("✅ Cargadas {} cancelaciones programadas", cancellations.size());
            
        } catch (IOException e) {
            logger.error("❌ Error leyendo archivo de cancelaciones: {}", e.getMessage());
        }
        
        return cancellations;
    }
    
    /**
     * Parsea una línea del archivo de cancelaciones.
     * 
     * Formato: 01.06:00,SPIM,SEQM,03:34
     * 
     * @param line Línea a parsear
     * @param startDate Fecha de inicio de la simulación
     * @return Cancelación programada
     */
    private static FlightCancellation parseCancellationLine(String line, LocalDate startDate) {
        String[] parts = line.split(",");
        
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                "Formato inválido. Esperado: DIA.HH:MM,ORIGEN,DESTINO,HORA_VUELO"
            );
        }
        
        // 1. Parsear momento de cancelación (DIA.HH:MM)
        String[] timeParts = parts[0].split("\\.");
        if (timeParts.length != 2) {
            throw new IllegalArgumentException("Formato de tiempo inválido: " + parts[0]);
        }
        
        int dayOfWeek = Integer.parseInt(timeParts[0]);          // 01-07
        String cancelTime = timeParts[1];                        // HH:MM
        
        // Validar día
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Día inválido: " + dayOfWeek + " (debe ser 01-07)");
        }
        
        // Calcular fecha de cancelación
        LocalDate cancellationDate = startDate.plusDays(dayOfWeek - 1);
        LocalTime cancellationLocalTime = LocalTime.parse(cancelTime);
        LocalDateTime cancellationTime = LocalDateTime.of(cancellationDate, cancellationLocalTime);
        
        // 2. Parsear datos del vuelo
        String origin = parts[1].trim();
        String destination = parts[2].trim();
        String flightTime = parts[3].trim();
        
        // 3. Crear cancelación
        String reason = String.format("Cancelación programada día %d", dayOfWeek);
        
        return new FlightCancellation(
            origin,
            destination,
            flightTime,
            cancellationTime,
            reason
        );
    }
    
    /**
     * Valida que un archivo de cancelaciones exista y sea legible.
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
        
        logger.info("✅ Archivo de cancelaciones válido: {}", filePath);
        return true;
    }
    
    /**
     * Método auxiliar para crear un archivo de ejemplo.
     */
    public static String generateExampleFile() {
        return """
            # cancellations_2025_12.txt
            # Formato: DIA.HH:MM,ORIGEN,DESTINO,HORA_VUELO
            # DIA: 01-07 (día de la semana)
            # HH:MM: Hora de cancelación
            # HORA_VUELO: Hora programada del vuelo
            
            # Ejemplo: Cancelar el día 1 a las 6:00 el vuelo SPIM→SEQM que sale a las 03:34
            01.06:00,SPIM,SEQM,03:34
            
            # Cancelaciones de prueba
            01.12:30,SBGR,SCLS,08:15
            03.14:30,SEQM,SPIM,10:30
            05.08:00,LTFM,EBCI,06:45
            07.16:00,SCLS,SBGR,14:20
            """;
    }
}

