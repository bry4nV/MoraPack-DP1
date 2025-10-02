package pe.edu.pucp.morapack.algos.algorithm.aco;

import java.time.LocalTime;

public class Vuelo {
    final int id;
    final Aeropuerto origen;
    final Aeropuerto destino;
    final int capacidadMax;
    final int frecuenciaPorDia;
    final LocalTime horaOrigen;
    final LocalTime horaDestino;

    public Vuelo(int id, Aeropuerto origen, Aeropuerto destino, String horaOrigenStr, String horaDestinoStr, int capacidadMax, int frecuenciaPorDia) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.capacidadMax = capacidadMax;
        this.frecuenciaPorDia = Math.max(1, frecuenciaPorDia);

        // Convertir cadenas HH:mm a LocalTime
        this.horaOrigen = LocalTime.parse(horaOrigenStr);
        this.horaDestino = LocalTime.parse(horaDestinoStr);
    }

    // Método para calcular tiempo de vuelo en minutos
    public int getDuracionMinutos() {
        int duracion = (horaDestino.getHour() * 60 + horaDestino.getMinute()) - 
                       (horaOrigen.getHour() * 60 + horaOrigen.getMinute());
        // Si el resultado es negativo (vuelo pasa medianoche), ajustamos sumando 24h
        if (duracion < 0) duracion += 24 * 60;
        return duracion;
    }
}

      
    