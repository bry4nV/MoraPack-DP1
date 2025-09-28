package MoraTravel;

public class Aeropuerto {
    public int id;
    public String codigo;
    public String ciudad;
    public String pais;
    public String continente;
    public int capacidadAlmacen;
    public String latitud;
    public String longitud;
    public int husoHorario;

    // Constructor para inicializar todos los atributos
    public Aeropuerto(int id, String codigo, String ciudad, String pais, String continente, int capacidadAlmacen, String latitud, String longitud, int husoHorario) {
        this.id = id;
        this.codigo = codigo;
        this.ciudad = ciudad;
        this.pais = pais;
        this.continente = continente;
        this.capacidadAlmacen = capacidadAlmacen;
        this.latitud = latitud;
        this.longitud = longitud;
        this.husoHorario = husoHorario;
    }

    // Método para obtener una representación en texto del aeropuerto
    @Override
    public String toString() {
        return String.format("Aeropuerto: %s (%s, %s) - Continente: %s - Capacidad: %d - Latitud: %s - Longitud: %s", 
                codigo, ciudad, pais, continente, capacidadAlmacen, latitud, longitud);
    }

    // Getters y setters (si necesitas manipular los datos fuera de esta clase)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }


    public String getContinente() {
        return continente;
    }


    public int getCapacidadAlmacen() {
        return capacidadAlmacen;
    }

    public void setCapacidadAlmacen(int capacidadAlmacen) {
        this.capacidadAlmacen = capacidadAlmacen;
    }

    public String getLatitud() {
        return latitud;
    }



    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }
}
