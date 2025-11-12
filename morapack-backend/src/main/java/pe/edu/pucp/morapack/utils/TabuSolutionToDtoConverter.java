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

    public static AeropuertoDTO[] toAirportDtos(List<PlannerAirport> airports) {
        List<AeropuertoDTO> out = new ArrayList<>();
        AtomicInteger id = new AtomicInteger(1);
        for (PlannerAirport a : airports) {
            AeropuertoDTO dto = new AeropuertoDTO();
            dto.id = id.getAndIncrement();
            dto.nombre = a.getName();
            dto.codigo = a.getCode();
            dto.ciudad = a.getCity();
            dto.latitud = a.getLatitude();
            dto.longitud = a.getLongitude();
            dto.gmt = a.getGmt();
            dto.esSede = "SPIM".equals(a.getCode());
            
            // ✅ Add capacity information (will be enriched later with dynamic data)
            dto.capacidadTotal = a.getStorageCapacity();
            dto.capacidadUsada = 0;  // Will be calculated by enrichAirportData
            dto.capacidadDisponible = a.getStorageCapacity();
            dto.porcentajeUso = 0.0;
            
            out.add(dto);
        }
        return out.toArray(new AeropuertoDTO[0]);
    }

    public static ItinerarioDTO[] toItinerarioDtos(TabuSolution solution) {
        return toItinerarioDtos(solution, java.time.Instant.now());
    }

    public static ItinerarioDTO[] toItinerarioDtos(TabuSolution solution, java.time.Instant snapshotTime) {
        List<ItinerarioDTO> out = new ArrayList<>();
        for (PlannerShipment ps : solution.getPlannerShipments()) {
            ItinerarioDTO it = new ItinerarioDTO();
            it.id = "sh-" + ps.getId();
            it.orderId = ps.getOrder().getId(); // ✅ Asignar el ID del pedido
            List<SegmentoDTO> segs = new ArrayList<>();

            // Build segmentos and compute distances/times
            List<PlannerFlight> flights = ps.getFlights();
            double totalMeters = 0.0;
            double[] segMeters = new double[flights.size()];
            java.time.Instant[] depInst = new java.time.Instant[flights.size()];
            java.time.Instant[] arrInst = new java.time.Instant[flights.size()];

            for (int i = 0; i < flights.size(); i++) {
                PlannerFlight f = flights.get(i);
                SegmentoDTO s = new SegmentoDTO();
                s.orden = i + 1;
                VueloDTO v = new VueloDTO();
                v.codigo = f.getCode();
                v.origen = airportToDto(f.getOrigin());
                v.destino = airportToDto(f.getDestination());
                // ✅ FIX: Formatear directamente como hora local (sin conversión de zona)
                // Backend y frontend usan la MISMA referencia de tiempo (hora local)
                v.salidaProgramadaISO = f.getDepartureTime().format(ISO);
                v.llegadaProgramadaISO = f.getArrivalTime().format(ISO);
                v.capacidad = f.getCapacity();
                v.preplanificado = f.isPreplanned();
                v.estado = f.getStatus().name();
                s.vuelo = v;
                segs.add(s);

                // compute distance and instants
                double meters = haversineMeters(f.getOrigin().getLatitude(), f.getOrigin().getLongitude(), f.getDestination().getLatitude(), f.getDestination().getLongitude());
                segMeters[i] = meters;
                totalMeters += meters;
                depInst[i] = f.getDepartureTime().atZone(java.time.ZoneId.systemDefault()).toInstant();
                arrInst[i] = f.getArrivalTime().atZone(java.time.ZoneId.systemDefault()).toInstant();
            }

            it.segmentos = segs.toArray(new SegmentoDTO[0]);

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

            out.add(it);
        }
        return out.toArray(new ItinerarioDTO[0]);
    }

    private static AeropuertoDTO airportToDto(PlannerAirport a) {
        AeropuertoDTO dto = new AeropuertoDTO();
        // id left as 0 when used inside flight objects; frontend can map by code
        dto.id = 0;
        dto.nombre = a.getName();
        dto.codigo = a.getCode();
        dto.ciudad = a.getCity();
        dto.latitud = a.getLatitude();
        dto.longitud = a.getLongitude();
        dto.gmt = a.getGmt();
        dto.esSede = "SPIM".equals(a.getCode());
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



