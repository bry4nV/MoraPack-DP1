package pe.edu.pucp.morapack.algos.algorithm.aco;

public class Arista {
    public Aeropuerto origen;
    public Aeropuerto destino;
    public int capacidad;
    public double tiempo; // tiempo de vuelo en horas o días
    public double feromona; // cantidad de feromona en esta ruta

    public Arista(Aeropuerto origen, Aeropuerto destino, int capacidad, double tiempo) {
        this.origen = origen;
        this.destino = destino;
        this.capacidad = capacidad;
        this.tiempo = tiempo;
        this.feromona = 0.1; // valor inicial de feromona
    }
}
