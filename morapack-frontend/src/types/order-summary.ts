export type OrderStatus = 'PENDING' | 'IN_TRANSIT' | 'COMPLETED' | 'UNASSIGNED';

export interface OrderSummary {
  id: number;
  codigo: string;
  
  // Origen y destino
  origenCodigo: string;
  origenNombre: string;
  destinoCodigo: string;
  destinoNombre: string;
  
  // Cantidades
  cantidadTotal: number;
  cantidadAsignada: number;
  progresoPercent: number;
  
  // Tiempos
  fechaSolicitudISO: string;
  fechaETA_ISO?: string | null;
  
  // Estado
  estado: OrderStatus;
  
  // Vuelos asignados
  vuelosAsignados: string[];
  
  // Prioridad (opcional)
  prioridad?: number;
}

export interface OrderMetrics {
  totalPedidos: number;
  pendientes: number;
  enTransito: number;
  completados: number;
  sinAsignar: number;
  
  // Additional statistics
  totalProductos: number;
  productosAsignados: number;
  tasaAsignacionPercent: number;
}

export interface SimulationPreview {
  totalPedidos: number;
  totalProductos: number;
  totalVuelos: number;
  
  dateRange: {
    start: string;
    end: string;
  };
  
  pedidos: OrderSummary[];
  aeropuertosInvolucrados: string[];
  aeropuertos?: any[];  // Full airport data for map
  
  estadisticas: {
    pedidosPorDia: number[];
    productosPromedioPorPedido: number;
    pedidoMaxProductos: number;
    pedidoMinProductos: number;
  };
}

