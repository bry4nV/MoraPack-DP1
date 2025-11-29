package pe.edu.pucp.morapack.utils;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.dto.simulation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.format.DateTimeFormatter;

public class TabuSolutionToDtoConverter {
    // ✅ FIX: Usar ISO_LOCAL_DATE_TIME (sin conversión de zona horaria)
    // El backend y frontend trabajan en la MISMA zona horaria (hora local)
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static AirportDTO[] toAirportDtos(List<PlannerAirport> airports) {
        List<AirportDTO> out = new ArrayList<>();
        AtomicInteger id = new AtomicInteger(1);
        for (PlannerAirport a : airports) {
            AirportDTO dto = new AirportDTO();
            dto.id = id.getAndIncrement();
            dto.name = a.getName();
            dto.code = a.getCode();
            dto.city = a.getCity();
            // ✅ FIX: Include country and continent information
            dto.country = a.getCountry() != null ? a.getCountry().getName() : "Unknown";
            dto.continent = a.getContinent() != null ? a.getContinent().name() : "AMERICA";
            dto.latitude = a.getLatitude();
            dto.longitude = a.getLongitude();
            dto.gmt = a.getGmt();
            dto.isHub = "SPIM".equals(a.getCode());

            // Add capacity information (will be enriched later with dynamic data)
            dto.totalCapacity = a.getStorageCapacity();
            dto.usedCapacity = 0;  // Will be calculated by enrichAirportData
            dto.availableCapacity = a.getStorageCapacity();
            dto.usagePercentage = 0.0;

            out.add(dto);
        }
        return out.toArray(new AirportDTO[0]);
    }

    public static ItineraryDTO[] toItineraryDtos(TabuSolution solution) {
        return toItineraryDtos(solution, java.time.Instant.now());
    }

    public static ItineraryDTO[] toItineraryDtos(TabuSolution solution, java.time.Instant snapshotTime) {
        List<ItineraryDTO> out = new ArrayList<>();
        for (PlannerShipment ps : solution.getPlannerShipments()) {
            ItineraryDTO it = new ItineraryDTO();
            it.id = "sh-" + ps.getId();
            it.orderId = ps.getOrder().getId();
            List<RouteSegmentDTO> segs = new ArrayList<>();

            // Build segmentos and compute distances/times
            List<PlannerFlight> flights = ps.getFlights();
            double totalMeters = 0.0;
            double[] segMeters = new double[flights.size()];
            java.time.Instant[] depInst = new java.time.Instant[flights.size()];
            java.time.Instant[] arrInst = new java.time.Instant[flights.size()];

            for (int i = 0; i < flights.size(); i++) {
                PlannerFlight f = flights.get(i);
                RouteSegmentDTO s = new RouteSegmentDTO();
                s.order = i + 1;
                FlightDTO v = new FlightDTO();
                v.code = f.getCode();
                v.origin = airportToDto(f.getOrigin());
                v.destination = airportToDto(f.getDestination());
                // Format directly as local time (no timezone conversion)
                // Backend and frontend use the SAME time reference (local time)
                v.scheduledDepartureISO = f.getDepartureTime().format(ISO);
                v.scheduledArrivalISO = f.getArrivalTime().format(ISO);
                v.capacity = f.getCapacity();
                v.preplanned = f.isPreplanned();
                v.status = f.getStatus().name();
                s.flight = v;
                segs.add(s);

                // compute distance and instants
                double meters = haversineMeters(f.getOrigin().getLatitude(), f.getOrigin().getLongitude(), f.getDestination().getLatitude(), f.getDestination().getLongitude());
                segMeters[i] = meters;
                totalMeters += meters;
                depInst[i] = f.getDepartureTime().atZone(java.time.ZoneId.systemDefault()).toInstant();
                arrInst[i] = f.getArrivalTime().atZone(java.time.ZoneId.systemDefault()).toInstant();
            }

            it.segments = segs.toArray(new RouteSegmentDTO[0]);

            // Determine current segment by snapshotTime
            double accumulatedMetersBefore = 0.0;
            double posLat = 0.0, posLon = 0.0;
            double progressMeters = 0.0;
            boolean found = false;

            for (int i = 0; i < flights.size(); i++) {
                java.time.Instant dep = depInst[i];
                java.time.Instant arr = arrInst[i];
                if (snapshotTime.isBefore(dep)) {
                    // before this segment: position at origin
                    posLat = flights.get(i).getOrigin().getLatitude();
                    posLon = flights.get(i).getOrigin().getLongitude();
                    progressMeters = accumulatedMetersBefore;
                    found = true;
                    break;
                } else if (!snapshotTime.isBefore(dep) && snapshotTime.isBefore(arr)) {
                    // inside this segment
                    double segDur = (double) java.time.Duration.between(dep, arr).toMillis();
                    double elapsed = (double) java.time.Duration.between(dep, snapshotTime).toMillis();
                    double frac = segDur <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, elapsed / segDur));
                    PlannerFlight f = flights.get(i);
                    posLat = lerp(f.getOrigin().getLatitude(), f.getDestination().getLatitude(), frac);
                    posLon = lerp(f.getOrigin().getLongitude(), f.getDestination().getLongitude(), frac);
                    progressMeters = accumulatedMetersBefore + segMeters[i] * frac;
                    found = true;
                    break;
                } else {
                    // after this segment: accumulate and continue
                    accumulatedMetersBefore += segMeters[i];
                }
            }

            if (!found && flights.size() > 0) {
                // After all segments: at destination of last
                PlannerFlight last = flights.get(flights.size() - 1);
                posLat = last.getDestination().getLatitude();
                posLon = last.getDestination().getLongitude();
                progressMeters = totalMeters;
            }

            it.positionLat = posLat;
            it.positionLon = posLon;
            it.progressMeters = progressMeters;
            it.status = ps.getStatus().name();  // "ACTIVE" or "CANCELLED"

            out.add(it);
        }
        return out.toArray(new ItineraryDTO[0]);
    }

    private static AirportDTO airportToDto(PlannerAirport a) {
        AirportDTO dto = new AirportDTO();
        // id left as 0 when used inside flight objects; frontend can map by code
        dto.id = 0;
        dto.name = a.getName();
        dto.code = a.getCode();
        dto.city = a.getCity();
        // ✅ FIX: Include country and continent information
        dto.country = a.getCountry() != null ? a.getCountry().getName() : "Unknown";
        dto.continent = a.getContinent() != null ? a.getContinent().name() : "AMERICA";
        dto.latitude = a.getLatitude();
        dto.longitude = a.getLongitude();
        dto.gmt = a.getGmt();
        dto.isHub = "SPIM".equals(a.getCode());
        return dto;
    }

    private static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        // sanitize
        lat1 = clamp(lat1, -90, 90);
        lat2 = clamp(lat2, -90, 90);
        lon1 = clamp(lon1, -180, 180);
        lon2 = clamp(lon2, -180, 180);
        final int R = 6371000; // Earth radius in meters
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dphi = Math.toRadians(lat2 - lat1);
        double dlambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dphi/2) * Math.sin(dphi/2) + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda/2) * Math.sin(dlambda/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}



