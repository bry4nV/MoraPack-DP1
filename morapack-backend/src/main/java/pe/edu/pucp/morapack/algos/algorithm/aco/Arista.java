package pe.edu.pucp.morapack.algos.algorithm.aco;

public class Arista {
    public Aeropuerto origen;
    public Aeropuerto destino;
    public int capacidad;
    public int horaSalida;    // en minutos desde medianoche
    public int horaLlegada;   // en minutos desde medianoche
    public double feromona;

    public Arista(Aeropuerto origen, Aeropuerto destino, int capacidad, int horaSalida, int horaLlegada) {
        this.origen = origen;
        this.destino = destino;
        this.capacidad = capacidad;
        this.horaSalida = horaSalida;
        this.horaLlegada = horaLlegada;
        this.feromona = 0.1;
    }

    public int getDuracionMinutos() {
        int duracion = horaLlegada - horaSalida;
        if (duracion < 0) duracion += 24 * 60;
        return duracion;
    }
}
