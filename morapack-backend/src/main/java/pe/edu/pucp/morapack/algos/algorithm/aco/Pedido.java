package pe.edu.pucp.morapack.algos.algorithm.aco;

public class Pedido {
    private int dia;
    private int hora;
    private int minuto;
    private String destino;
    private int cantidadPaquetes;
    private int idCliente;

    
    public Pedido(int dia, int hora, int minuto, String destino, int cantidadPaquetes, int idCliente) {
        this.dia = dia;
        this.hora = hora;
        this.minuto = minuto;
        this.destino = destino;
        this.cantidadPaquetes = cantidadPaquetes;
        this.idCliente = idCliente;
    }

    
    public int getDia() {
        return dia;
    }

    public void setDia(int dia) {
        this.dia = dia;
    }

    public int getHora() {
        return hora;
    }

    public void setHora(int hora) {
        this.hora = hora;
    }

    public int getMinuto() {
        return minuto;
    }

    public void setMinuto(int minuto) {
        this.minuto = minuto;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public int getCantidadPaquetes() {
        return cantidadPaquetes;
    }

    public void setCantidadPaquetes(int cantidadPaquetes) {
        this.cantidadPaquetes = cantidadPaquetes;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public int getHoraPedidoEnMinutos() {
        return hora * 60 + minuto;
    }

    @Override
    public String toString() {
        return "Pedido{" +
                "dia=" + dia +
                ", hora=" + hora +
                ", minuto=" + minuto +
                ", destino='" + destino + '\'' +
                ", cantidadPaquetes=" + cantidadPaquetes +
                ", idCliente=" + idCliente +
                '}';
    }
}
