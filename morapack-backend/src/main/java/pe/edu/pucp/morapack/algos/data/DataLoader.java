package pe.edu.pucp.morapack.algos.data;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.model.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.LocalDate;
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

    public static List<PlannerAirport> loadAirports(String filePath) throws IOException {
        List<PlannerAirport> airports = new ArrayList<>();
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
                
                airports.add(new PlannerAirport(idCounter++, code, city, city, country, capacity, gmt, latitude, longitude));
            }
        }
        return airports;
    }

    /**
     * Load flight templates from CSV (without specific dates, just times).
     * Flight templates represent recurring flights that happen daily.
     * 
     * CSV format: Origen,Destino,HoraOrigen,HoraDestino,Capacidad
     * Example: SKBO,SEQM,03:34,05:21,0300
     */
    public static List<pe.edu.pucp.morapack.algos.scheduler.FlightTemplate> loadFlightTemplates(String filePath, Map<String, PlannerAirport> airportMap) throws IOException {
        List<pe.edu.pucp.morapack.algos.scheduler.FlightTemplate> templates = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        int idCounter = 1;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            br.readLine();
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                
                String originCode = parts[0].trim();
                String destCode = parts[1].trim();
                String departureTime = parts[2].trim();
                String arrivalTime = parts[3].trim();
                int capacity = Integer.parseInt(parts[4].trim());
                
                PlannerAirport origin = airportMap.get(originCode);
                PlannerAirport destination = airportMap.get(destCode);
                
                if (origin == null || destination == null) {
                    System.out.println("   ⚠️  Unknown airport in flight: " + originCode + " -> " + destCode);
                    continue;
                }
                
                // Parse times (local times of each airport)
                LocalTime depTime = LocalTime.parse(departureTime, timeFormatter);
                LocalTime arrTime = LocalTime.parse(arrivalTime, timeFormatter);
                
                // Create flight template
                pe.edu.pucp.morapack.algos.scheduler.FlightTemplate template = 
                    new pe.edu.pucp.morapack.algos.scheduler.FlightTemplate(
                        idCounter++, origin, destination, depTime, arrTime, capacity, 1000.0
                    );
                
                templates.add(template);
            }
        }
        
        System.out.println("   ✅ Loaded " + templates.size() + " flight templates");
        return templates;
    }
    
    /**
     * Load flights for a specific time period using flight templates.
     * Generates actual flights with specific dates from templates.
     * 
     * @param filePath Path to flights CSV
     * @param airportMap Map of airport codes to PlannerAirport objects
     * @param year Year to generate flights for
     * @param month Month to generate flights for
     * @param daysToGenerate Number of days to generate flights for
     * @return List of PlannerFlight objects with specific dates
     */
    public static List<PlannerFlight> loadFlights(String filePath, Map<String, PlannerAirport> airportMap, int year, int month, int daysToGenerate) throws IOException {
        System.out.println("   📋 Loading flights from: " + filePath);
        System.out.println("   📅 Generating flights for: " + year + "-" + String.format("%02d", month) + " (" + daysToGenerate + " days)");
        
        // Load flight templates
        List<pe.edu.pucp.morapack.algos.scheduler.FlightTemplate> templates = loadFlightTemplates(filePath, airportMap);
        
        // Use FlightExpander to generate actual flights
        pe.edu.pucp.morapack.algos.scheduler.FlightExpander expander = 
            new pe.edu.pucp.morapack.algos.scheduler.FlightExpander();
        
        List<PlannerFlight> flights = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, month, 1);
        
        for (int day = 0; day < daysToGenerate; day++) {
            LocalDate currentDate = startDate.plusDays(day);
            
            for (pe.edu.pucp.morapack.algos.scheduler.FlightTemplate template : templates) {
                PlannerFlight flight = expander.expandForDate(template, currentDate);
                flights.add(flight);
            }
        }
        
        System.out.println("   ✅ Generated " + flights.size() + " flights (" + templates.size() + " templates × " + daysToGenerate + " days)");
        return flights;
    }
    
    /**
     * Legacy method for backwards compatibility.
     * Defaults to generating flights for October 2025 (31 days).
     * @deprecated Use {@link #loadFlights(String, Map, int, int, int)} instead
     */
    @Deprecated
    public static List<PlannerFlight> loadFlights(String filePath, Map<String, PlannerAirport> airportMap) throws IOException {
        return loadFlights(filePath, airportMap, 2025, 10, 31);
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

    /**
     * Load orders from CSV with relative dates (dd-hh-mm format).
     * 
     * CSV format: dd,hh,mm,dest,###,IdClien
     * Example: 04,16,22,EDDI,344,6084676
     * 
     * @param filePath Path to orders CSV file
     * @param airportMap Map of airport codes to PlannerAirport objects
     * @param year Year for simulation (e.g., 2025)
     * @param month Month for simulation (1-12)
     * @return List of PlannerOrder objects
     */
    public static List<PlannerOrder> loadOrders(String filePath, Map<String, PlannerAirport> airportMap, int year, int month) throws IOException {
        List<PlannerOrder> orders = new ArrayList<>();
        int orderId = 1;
        int skippedHubs = 0;
        int skippedInvalid = 0;
        
        // Main hubs (production centers) - orders to these are local deliveries
        Set<String> mainHubs = Set.of("SPIM", "EBCI", "UBBB");
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            String header = br.readLine();
            System.out.println("   📋 Loading orders from: " + filePath);
            System.out.println("   📅 Reference period: " + year + "-" + String.format("%02d", month));
            
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");
                if (parts.length < 6) {
                    skippedInvalid++;
                    continue;
                }
                
                try {
                    int day = Integer.parseInt(parts[0].trim());
                    int hour = Integer.parseInt(parts[1].trim());
                    int minute = Integer.parseInt(parts[2].trim());
                    String destCode = parts[3].trim();
                    int quantity = Integer.parseInt(parts[4].trim());
                    String clientId = parts[5].trim();
                    
                    // ⬇️ FILTER: Exclude orders to main hubs (local delivery, no air transport needed)
                    if (mainHubs.contains(destCode)) {
                        skippedHubs++;
                        continue;
                    }
                    
                    // Get destination airport
                    PlannerAirport destination = airportMap.get(destCode);
                    if (destination == null) {
                        System.out.println("   ⚠️  Line " + lineNumber + ": Unknown airport code: " + destCode);
                        skippedInvalid++;
                        continue;
                    }
                    
                    // Determine origin based on destination continent and proximity
                    PlannerAirport origin = determineOptimalOrigin(destination, airportMap);
                    if (origin == null) {
                        System.out.println("   ⚠️  Line " + lineNumber + ": Could not determine origin for: " + destCode);
                        skippedInvalid++;
                        continue;
                    }
                    
                    // Convert relative date (dd-hh-mm) to absolute timestamp
                    LocalDateTime orderTime = LocalDateTime.of(year, month, day, hour, minute);
                    
                    // Create order
                    PlannerOrder order = new PlannerOrder(orderId++, quantity, origin, destination);
                    order.setOrderTime(orderTime);
                    order.setClientId(clientId);
                    orders.add(order);
                    
                } catch (Exception e) {
                    System.out.println("   ⚠️  Line " + lineNumber + " parsing error: " + e.getMessage());
                    skippedInvalid++;
                }
            }
        }
        
        System.out.println("   ✅ Successfully loaded " + orders.size() + " orders");
        if (skippedHubs > 0) {
            System.out.println("   ⏭️  Skipped " + skippedHubs + " orders to main hubs (local delivery)");
        }
        if (skippedInvalid > 0) {
            System.out.println("   ⚠️  Skipped " + skippedInvalid + " invalid/malformed orders");
        }
        
        return orders;
    }
    
    /**
     * Load orders from CSV with absolute timestamps (ISO 8601 format).
     * 
     * CSV format: timestamp,destination,quantity,clientId
     * Example: 2025-12-04T16:22:00,EDDI,344,6084676
     * 
     * @param filePath Path to orders CSV file
     * @param airportMap Map of airport codes to PlannerAirport objects
     * @return List of PlannerOrder objects
     */
    public static List<PlannerOrder> loadOrdersWithAbsoluteDates(String filePath, Map<String, PlannerAirport> airportMap) throws IOException {
        List<PlannerOrder> orders = new ArrayList<>();
        int orderId = 1;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            String header = br.readLine();
            System.out.println("   📋 CSV Header: " + header);
            
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    System.out.println("   ⚠️  Line " + lineNumber + " skipped: insufficient columns");
                    continue;
                }
                
                try {
                    // Parse absolute timestamp (ISO 8601)
                    String timestampStr = parts[0].trim();
                    LocalDateTime orderTime = LocalDateTime.parse(timestampStr);
                    
                    String destCode = parts[1].trim();
                    int quantity = Integer.parseInt(parts[2].trim());
                    String clientId = parts[3].trim();
                    
                    // Get destination airport
                    PlannerAirport destination = airportMap.get(destCode);
                    if (destination == null) {
                        System.out.println("   ⚠️  Line " + lineNumber + ": Unknown airport code: " + destCode);
                        continue;
                    }
                    
                    // Determine origin based on destination continent and proximity
                    PlannerAirport origin = determineOptimalOrigin(destination, airportMap);
                    if (origin == null) {
                        System.out.println("   ⚠️  Line " + lineNumber + ": Could not determine origin for: " + destCode);
                        continue;
                    }
                    
                    PlannerOrder order = new PlannerOrder(orderId++, quantity, origin, destination);
                    order.setOrderTime(orderTime);
                    order.setClientId(clientId);
                    orders.add(order);
                    
                } catch (Exception e) {
                    System.out.println("   ⚠️  Line " + lineNumber + " parsing error: " + e.getMessage());
                }
            }
        }
        
        System.out.println("   ✅ Successfully parsed " + orders.size() + " orders with absolute timestamps");
        return orders;
    }
    
    /**
     * Determines the optimal origin (MoraPack distribution center) using completely dynamic assignment.
     * Prioritizes flight availability and operational efficiency over geographic restrictions.
     * Any hub can serve any destination if it provides the best service.
     * MoraPack has 3 distribution centers:
     * - Lima, Peru (SPIM) 
     * - Brussels, Belgium (EBCI)  
     * - Baku, Azerbaijan (UBBB)
     */
    private static PlannerAirport determineOptimalOrigin(PlannerAirport destination, Map<String, PlannerAirport> airportMap) {
        String destCode = destination.getCode();
        
        // Calculate dynamic scores for each distribution center
        double limaScore = calculateDynamicOriginScore(destCode, "SPIM", isSouthAmerica(destCode));
        double brusselsScore = calculateDynamicOriginScore(destCode, "EBCI", isEurope(destCode));
        double bakuScore = calculateDynamicOriginScore(destCode, "UBBB", isAsia(destCode));
        
        // Select the origin with highest score
        String selectedOrigin;
        if (limaScore >= brusselsScore && limaScore >= bakuScore) {
            selectedOrigin = "SPIM";
        } else if (brusselsScore >= bakuScore) {
            selectedOrigin = "EBCI";
        } else {
            selectedOrigin = "UBBB";
        }
        
        // Debug output to track dynamic assignments (3% sample for cleaner output)
        if (Math.random() < 0.03) {
            System.out.printf("🎯 Dynamic: %s -> %s (L=%.1f, B=%.1f, K=%.1f) %s%n",
                destCode, selectedOrigin, limaScore, brusselsScore, bakuScore,
                getRegionLabel(destCode));
        }
        
        return airportMap.get(selectedOrigin);
    }
    
    /**
     * Calculates dynamic score with adaptive regional weights for optimal assignment.
     * Uses different scoring strategies per region for better performance.
     */
    private static double calculateDynamicOriginScore(String destCode, String originCode, boolean isContinentalMatch) {
        double flightScore = getFlightAvailabilityScore(originCode, destCode);
        double operationalScore = getOperationalScore(originCode);
        double proximityScore = isContinentalMatch ? 20.0 : 8.0;
        
        // Adaptive weights based on destination region for optimal performance
        double flightWeight, operationalWeight, proximityWeight;
        
        if (isSouthAmerica(destCode)) {
            // South America: Prioritize geographic proximity (Lima advantage)
            flightWeight = 0.3;
            operationalWeight = 0.2;
            proximityWeight = 0.5;
        } else if (isEurope(destCode)) {
            // Europe: Prioritize flight availability (Brussels hub advantage)
            flightWeight = 0.6;
            operationalWeight = 0.3;
            proximityWeight = 0.1;
        } else if (isAsia(destCode)) {
            // Asia/Middle East: Balance operational efficiency (Baku strategic position)
            flightWeight = 0.4;
            operationalWeight = 0.4;
            proximityWeight = 0.2;
        } else {
            // Default balanced approach for other regions
            flightWeight = 0.5;
            operationalWeight = 0.3;
            proximityWeight = 0.2;
        }
        
        return (flightScore * flightWeight) + (operationalScore * operationalWeight) + (proximityScore * proximityWeight);
    }
    
    /**
     * Returns flight availability score based on hub capacity and route diversity.
     * Higher scores for hubs with more flights and better connectivity.
     */
    private static double getFlightAvailabilityScore(String originCode, String destCode) {
        double baseScore = 0.0;
        switch (originCode) {
            case "SPIM": baseScore = 46.0; break;   // 92 flights -> 46% of max score
            case "EBCI": baseScore = 53.0; break;  // 106 flights -> 53% of max score  
            case "UBBB": baseScore = 53.5; break; // 107 flights -> 53.5% of max score
        }
        
        // Additional bonus for hub specialization and route optimization
        // European destinations often have better connections from Brussels
        if (originCode.equals("EBCI") && isEurope(destCode)) {
            baseScore += 7.0;
        }
        // Asian destinations might benefit from Baku's strategic positioning
        if (originCode.equals("UBBB") && isAsia(destCode)) {
            baseScore += 7.0;
        }
        // South American routes optimized from Lima
        if (originCode.equals("SPIM") && isSouthAmerica(destCode)) {
            baseScore += 7.0;
        }
        
        return baseScore;
    }
    
    /**
     * Returns a region label for debugging purposes.
     */
    private static String getRegionLabel(String airportCode) {
        if (isSouthAmerica(airportCode)) return "[SA]";
        if (isEurope(airportCode)) return "[EU]";  
        if (isAsia(airportCode)) return "[AS]";
        return "[??]";
    }
    
    /**
     * Returns operational efficiency score based on hub characteristics and capacity.
     */
    private static double getOperationalScore(String originCode) {
        switch (originCode) {
            case "SPIM": return 29.0;  // Lima: good capacity (440), moderate efficiency
            case "EBCI": return 30.0;  // Brussels: excellent European hub (440), highest efficiency
            case "UBBB": return 28.0;  // Baku: good capacity (400), strategic Eurasian location
            default: return 25.0;
        }
    }
    
    private static boolean isSouthAmerica(String airportCode) {
        // South American ICAO codes typically start with S
        return airportCode.startsWith("SK") ||  // Colombia
               airportCode.startsWith("SE") ||  // Ecuador  
               airportCode.startsWith("SV") ||  // Venezuela
               airportCode.startsWith("SB") ||  // Brazil
               airportCode.startsWith("SP") ||  // Peru
               airportCode.startsWith("SL") ||  // Bolivia
               airportCode.startsWith("SC") ||  // Chile
               airportCode.startsWith("SA") ||  // Argentina
               airportCode.startsWith("SG") ||  // Paraguay
               airportCode.startsWith("SU");    // Uruguay
    }
    
    private static boolean isEurope(String airportCode) {
        // European ICAO codes
        return airportCode.startsWith("LA") ||  // Albania
               airportCode.startsWith("ED") ||  // Germany
               airportCode.startsWith("LO") ||  // Austria
               airportCode.startsWith("EB") ||  // Belgium
               airportCode.startsWith("UM") ||  // Belarus
               airportCode.startsWith("LB") ||  // Bulgaria
               airportCode.startsWith("LK") ||  // Czech Republic
               airportCode.startsWith("LD") ||  // Croatia
               airportCode.startsWith("EK") ||  // Denmark
               airportCode.startsWith("EH");    // Netherlands
    }
    
    private static boolean isAsia(String airportCode) {
        // Asian ICAO codes
        return airportCode.startsWith("VI") ||  // India
               airportCode.startsWith("OS") ||  // Syria
               airportCode.startsWith("OE") ||  // Saudi Arabia
               airportCode.startsWith("OM") ||  // UAE
               airportCode.startsWith("OA") ||  // Afghanistan
               airportCode.startsWith("OO") ||  // Oman
               airportCode.startsWith("OY") ||  // Yemen
               airportCode.startsWith("OP") ||  // Pakistan
               airportCode.startsWith("UB") ||  // Azerbaijan
               airportCode.startsWith("OJ");    // Jordan
    }
}