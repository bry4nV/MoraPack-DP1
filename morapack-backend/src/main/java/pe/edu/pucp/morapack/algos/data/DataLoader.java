package pe.edu.pucp.morapack.algos.data;

import pe.edu.pucp.morapack.model.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataLoader {
    private static final Random random = new Random(42); // Fixed seed for reproducible coordinates
    private static final Map<String, Country> countryCache = new HashMap<>();
    private static final Map<String, Continent> continentMap = Map.of(
        "America del Sur", Continent.AMERICA,
        "Europa", Continent.EUROPE,
        "Asia", Continent.ASIA
    );
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd,HH,mm");

    public static List<Airport> loadAirports(String filePath) throws IOException {
        List<Airport> airports = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int idCounter = 1;
            final String[] continentHolder = {""};
            
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                // Check if this is a continent line
                if (line.contains("America del Sur") || line.contains("Europa") || line.contains("Asia")) {
                    continentHolder[0] = line.trim();
                    continue;
                }
                
                // Skip header lines
                if (line.startsWith("*") || line.contains("GMT") || !line.matches(".*\\d+.*")) continue;
                
                // Extract line number and code first
                String[] initialParts = line.trim().split("\\s+", 3);
                if (initialParts.length < 3) continue;

                String lineNumber = initialParts[0];
                String code = initialParts[1];
                String remaining = initialParts[2];

                // Split the remaining parts, keeping multi-word city names intact
                String[] remainingParts = remaining.trim().split("\\s{2,}");
                if (remainingParts.length < 5) continue;

                String city = remainingParts[0].trim();
                String countryName = remainingParts[1].trim();
                String cityCode = remainingParts[2].trim();
                String gmtStr = remainingParts[3].trim();
                String capacityStr = remainingParts[4].trim();

                int gmt = Integer.parseInt(gmtStr);
                int capacity = Integer.parseInt(capacityStr);
                
                // Get latitude and longitude from the city code or generate random coordinates within continent boundaries
                double latitude = generateLatitudeForContinent(continentHolder[0]);
                double longitude = generateLongitudeForContinent(continentHolder[0]);
                
                // Create or get country
                Country country = countryCache.computeIfAbsent(countryName, k -> 
                    new Country(countryCache.size() + 1, k, continentMap.get(continentHolder[0])));
                
                airports.add(new Airport(idCounter++, code, city, city, country, capacity, gmt, latitude, longitude));
            }
        }
        return airports;
    }

    public static List<Flight> loadFlights(String filePath, Map<String, Airport> airportMap) throws IOException {
        List<Flight> flights = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        int idCounter = 1;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            br.readLine();
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                
                String originCode = parts[0];
                String destCode = parts[1];
                String departureTime = parts[2];
                String arrivalTime = parts[3];
                int capacity = Integer.parseInt(parts[4]);
                
                Airport origin = airportMap.get(originCode);
                Airport destination = airportMap.get(destCode);
                
                if (origin == null || destination == null) {
                    System.out.println("Warning: Could not find airport for flight: " + originCode + " -> " + destCode);
                    continue;
                }
                
                // Parse times
                LocalTime depTime = LocalTime.parse(departureTime, timeFormatter);
                LocalTime arrTime = LocalTime.parse(arrivalTime, timeFormatter);
                
                // Create flight using October 2025 dates to match orders
                LocalDateTime depDateTime = LocalDateTime.of(2025, 10, 1, depTime.getHour(), depTime.getMinute());
                LocalDateTime arrDateTime = LocalDateTime.of(2025, 10, 1, arrTime.getHour(), arrTime.getMinute());
                
                // If arrival is before departure, it means the flight arrives the next day
                if (arrDateTime.isBefore(depDateTime)) {
                    arrDateTime = arrDateTime.plusDays(1);
                }
                
                // Create flights for multiple days (October 1-31, 2025)
                for (int dayOffset = 0; dayOffset < 31; dayOffset++) {
                    LocalDateTime flightDepDateTime = depDateTime.plusDays(dayOffset);
                    LocalDateTime flightArrDateTime = arrDateTime.plusDays(dayOffset);
                    flights.add(new Flight(idCounter++, origin, destination, flightDepDateTime, flightArrDateTime, capacity, 1000.0));
                }
            }
        }
        return flights;
    }

    // Helper method to parse day-hour-minute format from order files
    public static LocalDateTime parseDayHourMinute(String day, String hour, String minute) {
        return LocalDateTime.of(2025, 10, Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(minute));
    }
    
    private static double generateLatitudeForContinent(String continent) {
        switch(continent) {
            case "America del Sur":
                return -23.0 + random.nextDouble() * 46; // -23 to 23 degrees
            case "Europa":
                return 35.0 + random.nextDouble() * 30;  // 35 to 65 degrees
            case "Asia":
                return 0.0 + random.nextDouble() * 65;   // 0 to 65 degrees
            default:
                return 0.0 + random.nextDouble() * 90;   // 0 to 90 degrees
        }
    }
    
    private static double generateLongitudeForContinent(String continent) {
        switch(continent) {
            case "America del Sur":
                return -80.0 + random.nextDouble() * 40; // -80 to -40 degrees
            case "Europa":
                return -10.0 + random.nextDouble() * 50; // -10 to 40 degrees
            case "Asia":
                return 60.0 + random.nextDouble() * 100; // 60 to 160 degrees
            default:
                return 0.0 + random.nextDouble() * 360;  // 0 to 360 degrees
        }
    }

    public static List<Order> loadOrders(String filePath, Map<String, Airport> airportMap) throws IOException {
        List<Order> orders = new ArrayList<>();
        int orderId = 1;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            br.readLine();
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;
                
                int day = Integer.parseInt(parts[0].trim());
                int hour = Integer.parseInt(parts[1].trim());
                int minute = Integer.parseInt(parts[2].trim());
                String destCode = parts[3].trim();
                int quantity = Integer.parseInt(parts[4].trim());
                String clientId = parts[5].trim();
                
                // Get destination airport
                Airport destination = airportMap.get(destCode);
                if (destination == null) {
                    System.out.println("Warning: Could not find airport for code: " + destCode);
                    continue;
                }
                
                // Por ahora asumimos que todos los pedidos salen de Lima (SPIM)
                Airport origin = airportMap.get("SPIM");
                if (origin == null) {
                    System.out.println("Error: Could not find Lima airport (SPIM)");
                    continue;
                }
                
                // Create order time based on day, hour, minute
                LocalDateTime orderTime = LocalDateTime.of(2025, 10, day, hour, minute);
                
                Order order = new Order(orderId++, quantity, origin, destination);
                // Set the order time explicitly
                order.setOrderTime(orderTime);
                orders.add(order);
            }
        }
        
        return orders;
    }
}