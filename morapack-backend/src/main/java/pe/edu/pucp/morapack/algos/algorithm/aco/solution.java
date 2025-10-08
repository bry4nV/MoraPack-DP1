package pe.edu.pucp.morapack.algos.algorithm.aco;

import java.util.HashMap;
import java.util.Map;




    public class solution {
        final Map<Integer,orderplan> planes;
        int vuelosUsados = 0;
        int paquetesOnTime = 0;
        int paquetesTotales = 0;
        double retrasoTotal = 0.0;
        int paquetesTardios = 0;
        
        public solution() {
            this.planes = new HashMap<>();
        }
        
        public solution(Map<Integer,orderplan> planes) {
            this.planes = planes;
            evaluar();
        }
        
        void evaluar() {
            vuelosUsados = 0;
            paquetesOnTime = 0;
            paquetesTotales = 0;
            retrasoTotal = 0.0;
            paquetesTardios = 0;

            for (orderplan op : planes.values()) {
                paquetesTotales += op.order.cantidad;
                vuelosUsados += op.legs.size();

                // ETA: máximo de llegadas de todos los legs usados
                double eta = 0.0;
                for (legassign la : op.legs) {
                    eta = Math.max(eta, la.slot.llegadaDia);
                }
                
                op.etaDias = op.legs.isEmpty() ? 0.0 : eta;

                // Calcular retraso y completitud
                double retraso = Math.max(0.0, eta - op.order.deadlineDias);
                boolean completo = (op.cantidadTotalAsignada >= op.order.cantidad);
                op.entregadoATiempo = completo && (retraso == 0.0);

                // Actualizar contadores
                if (op.entregadoATiempo) {
                    paquetesOnTime += op.order.cantidad;
                } else if (retraso > 0) {
                    paquetesTardios += op.cantidadTotalAsignada;
                }

                // Penalizar tardanza proporcional a lo realmente enviado
                retrasoTotal += retraso * op.cantidadTotalAsignada;
            }
        }
        
        public double cost() {
            // Penalización por paquetes tardíos
            double costoPaquetesTardios = 1000.0 * ((double)paquetesTardios / Math.max(1, paquetesTotales));
            
            // Penalización por retraso promedio
            double costoRetrasoPromedio = 10.0 * (retrasoTotal / Math.max(1, paquetesTotales));
            
            // Penalización por número de legs
            double costoLegs = 1.0 * ((double)vuelosUsados / Math.max(1, paquetesTotales));
            
            return costoPaquetesTardios + costoRetrasoPromedio + costoLegs;
        }

    }