package pe.edu.pucp.morapack.web.tabsim;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.model.Country;
import pe.edu.pucp.morapack.model.Continent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.morapack.algos.data.DataLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight CSV / data loader for demo purposes.
 * - loads airports (with coordinates if available) from `data/airports_real.txt` (preferred)
 * - loads flights from `data/flights.csv`
 * - loads orders from `data/pedidos.csv`
 *
 * This is intentionally permissive and built for the project's existing data files.
 */
public class CsvDataLoader {
    private final Path dataDir;

    public CsvDataLoader() {
        this.dataDir = Path.of("data");
    }

    public List<PlannerAirport> loadAirports() {
        Path real = dataDir.resolve("airports_real.txt");
        Path simple = dataDir.resolve("airports.txt");

        if (Files.exists(real)) return parseAirportsReal(real);
        if (Files.exists(simple)) return parseAirportsSimple(simple);
        throw new RuntimeException("No airports file found in data/ (expected airports_real.txt or airports.txt)");
    }

    private List<PlannerAirport> parseAirportsReal(Path p) {
        List<PlannerAirport> out = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(p)) {
            String line;
            Continent current = null;
            int idCounter = 1;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.toLowerCase().contains("america del sur")) { current = Continent.AMERICA; continue; }
                if (line.toLowerCase().contains("europa")) { current = Continent.EUROPE; continue; }
                if (line.toLowerCase().contains("asia")) { current = Continent.ASIA; continue; }

                // Only parse lines that contain 'Latitude' (data lines)
                if (!line.contains("Latitude")) continue;

                // Split left/right by Latitude to isolate the fixed fields
                String[] parts = line.split("Latitude:");
                if (parts.length < 2) continue;
                String left = parts[0].trim();
                String right = "Latitude:" + parts[1];

                String[] leftToks = left.split("\\s+");
                if (leftToks.length < 4) continue;
                String code = leftToks[1];

                // Guess GMT and capacity from the tail tokens
                String gmtStr = leftToks[leftToks.length - 2];
                String capStr = leftToks[leftToks.length - 1];
                int gmt = 0;
                int cap = 0;
                try { gmt = Integer.parseInt(gmtStr.replace("+","")); } catch (Exception ignored) {}
                try { cap = Integer.parseInt(capStr); } catch (Exception ignored) {}

                // Try to extract country name from the left side (approximate)
                String countryName = (leftToks.length >= 5) ? leftToks[leftToks.length - 4] : "Unknown";
                Country country = new Country(idCounter, countryName, current);

                // Parse latitude/longitude from the right part
                double lat = 0.0, lon = 0.0;
                try {
                    Pattern latPattern = Pattern.compile("Latitude:\s*([^L]+)Longitude:");
                    Matcher m = latPattern.matcher(right);
                    String latPart, lonPart;
                    if (m.find()) {
                        latPart = m.group(1).trim();
                        lonPart = right.substring(m.end()).trim();
                    } else {
                        String[] rr = right.split("Longitude:");
                        latPart = rr.length > 0 ? rr[0].replace("Latitude:","").trim() : "";
                        lonPart = rr.length > 1 ? rr[1].trim() : "";
                    }
                    lat = parseDms(latPart);
                    lon = parseDms(lonPart);
                } catch (Exception e) {
                    // keep zeros if parsing fails
                }

                PlannerAirport a = new PlannerAirport(idCounter++, code, "", "", country, cap, gmt, lat, lon);
                out.add(a);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    private double parseDms(String s) {
        if (s == null) return 0.0;
        // Remove extra characters
        s = s.replace("\uFEFF", "").replace("\u00A0"," ").trim();
        // Example: 04° 42' 05" N
        Pattern p = Pattern.compile("(\\d+)[°\\s]+(\\d+)[']+[\\s]*(\\d+)[\"]*\\s*([NSEW])");
        Matcher m = p.matcher(s);
        if (m.find()) {
            int deg = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            int sec = Integer.parseInt(m.group(3));
            String dir = m.group(4);
            double dec = deg + min / 60.0 + sec / 3600.0;
            if (dir.equalsIgnoreCase("S") || dir.equalsIgnoreCase("W")) dec = -dec;
            return dec;
        }
        // fallback: try to parse simple signed number
        try {
            String cleaned = s.replaceAll("[^0-9.\\\\-]", "");
            if (cleaned.isEmpty()) return 0.0;
            double val = Double.parseDouble(cleaned);
            // Heuristic: if value is obviously out of lat/lon range, try DMS-like conversion
            double absVal = Math.abs(val);
            if (absVal >= 10000 && absVal < 10000000) {
                try {
                    long iv = Math.round(absVal);
                    int deg = (int) (iv / 10000);
                    int min = (int) ((iv - deg * 10000) / 100);
                    int sec = (int) (iv - deg * 10000 - min * 100);
                    double dec = deg + min / 60.0 + sec / 3600.0;
                    if (val < 0) dec = -dec;
                    System.out.println(String.format("[DATA] parseDms: converted DMS-like '%s' -> %.6f", cleaned, dec));
                    return dec;
                } catch (Exception ignored) {
                    // fall through and return raw value clamped by caller if necessary
                }
            }
            return val;
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private List<PlannerAirport> parseAirportsSimple(Path p) {
        // Very small parser to get code, gmt and capacity when no coordinates are present.
        List<PlannerAirport> out = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(p)) {
            String line;
            Continent current = null;
            int idCounter = 1;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.toLowerCase().contains("america del sur")) { current = Continent.AMERICA; continue; }
                if (line.toLowerCase().contains("europa")) { current = Continent.EUROPE; continue; }
                if (line.toLowerCase().contains("asia")) { current = Continent.ASIA; continue; }
                String[] toks = line.split("\\s+");
                if (toks.length < 6) continue;
                String code = toks[1];
                String countryName = toks[toks.length - 4];
                int gmt = 0; int cap = 0;
                try { gmt = Integer.parseInt(toks[toks.length - 2].replace("+","")); } catch (Exception ignored) {}
                try { cap = Integer.parseInt(toks[toks.length - 1]); } catch (Exception ignored) {}
                Country country = new Country(idCounter, countryName, current);
                PlannerAirport a = new PlannerAirport(idCounter++, code, "", "", country, cap, gmt);
                out.add(a);
            }
        } catch (IOException e) { throw new RuntimeException(e); }
        return out;
    }

    public List<PlannerFlight> loadFlights(List<PlannerAirport> airports) {
        Map<String, PlannerAirport> byCode = new HashMap<>();
        for (PlannerAirport a : airports) byCode.put(a.getCode(), a);
        Path p = dataDir.resolve("flights.csv");
        List<PlannerFlight> out = new ArrayList<>();
        if (!Files.exists(p)) return out;
        try (BufferedReader r = Files.newBufferedReader(p)) {
            // skip header line if present
            r.readLine();
            String line;
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalDate today = LocalDate.now();
            
            // Generate flights for today and tomorrow to give more scheduling flexibility
            LocalDate[] dates = {today, today.plusDays(1)};
            
            List<String[]> flightRows = new ArrayList<>();
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",");
                if (cols.length >= 5) flightRows.add(cols);
            }
            
            // Create flights for each date
            for (LocalDate date : dates) {
                for (String[] cols : flightRows) {
                    String from = cols[0].trim();
                    String to = cols[1].trim();
                    String dep = cols[2].trim();
                    String arr = cols[3].trim();
                    String cap = cols[4].trim();
                    PlannerAirport o = byCode.get(from);
                    PlannerAirport d = byCode.get(to);
                    if (o == null || d == null) continue;
                    LocalDateTime depT = LocalDateTime.of(date, java.time.LocalTime.parse(dep, timeFmt));
                    LocalDateTime arrT = LocalDateTime.of(date, java.time.LocalTime.parse(arr, timeFmt));
                    if (arrT.isBefore(depT)) arrT = arrT.plusDays(1);
                    int capacity = 0; try { capacity = Integer.parseInt(cap); } catch (Exception ignored) {}
                    // Demo override: if capacity is non-positive in CSV, give a small default so demo can assign shipments
                    if (capacity <= 0) {
                        int demoCap = 50; // small demo capacity to ensure assignments
                        if (out.isEmpty()) { // only log once
                            System.out.println(String.format("[DATA] Demo override: flight %s->%s had capacity %s, set to %d for demo",
                                    from, to, cap, demoCap));
                        }
                        capacity = demoCap;
                    }
                    PlannerFlight f = new PlannerFlight(o, d, depT, arrT, capacity);
                    out.add(f);
                }
            }
        } catch (IOException e) { throw new RuntimeException(e); }
        System.out.println(String.format("[DATA] Loaded %d flights (across %d days)", out.size(), 2));
        return out;
    }

    public List<PlannerOrder> loadOrders(List<PlannerAirport> airports) {
        // Reuse the richer DataLoader order logic for realistic origin selection.
        Map<String, PlannerAirport> byCode = new HashMap<>();
        for (PlannerAirport a : airports) byCode.put(a.getCode(), a);
        Path p = dataDir.resolve("pedidos.csv");
        if (!Files.exists(p)) return new ArrayList<>();
        try {
            // DataLoader expects a file path and an airport map
            return DataLoader.loadOrders(p.toString(), byCode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load orders via DataLoader", e);
        }
    }
}
