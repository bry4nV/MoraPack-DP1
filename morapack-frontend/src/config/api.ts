// API Configuration
export const API_CONFIG = {
  BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
  WS_URL: process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws',
  TIMEOUT: 30000,
  RETRY_ATTEMPTS: 3,
} as const;

// API Endpoints
export const API_ENDPOINTS = {
  ORDERS: {
    BASE: '/api/orders',
    BY_ID: (id: number) => `/api/orders/${id}`, // Cambié de string a number
    BULK_CREATE: '/api/orders/bulk-create',
    BULK_DELETE: '/api/orders/bulk-delete',
  },
  AIRPORTS: {
    BASE: '/api/airports',
    BY_ID: (id: number) => `/api/airports/${id}`, // Cambié de string a number
    BULK_CREATE: '/api/airports/bulk-create',
    BULK_DELETE: '/api/airports/bulk-delete',
  },
  DYNAMIC_EVENTS: {
    CANCEL_FLIGHT: '/api/simulation/events/cancel-flight',
    LOAD_CANCELLATIONS: '/api/simulation/events/load-cancellations',
    ADD_ORDER: '/api/simulation/events/add-order',
    LOAD_ORDERS: '/api/simulation/events/load-orders',
    GET_CANCELLATIONS: '/api/simulation/events/cancellations',
    GET_ORDERS: '/api/simulation/events/orders',
    GET_CANCELLATIONS_EXECUTED: '/api/simulation/events/cancellations/executed',
    GET_ORDERS_INJECTED: '/api/simulation/events/orders/injected',
  },

  FLIGHTS: {
    BASE: '/api/flights',
    BY_ID: (id: number) => `/api/flights/${id}`, // Cambié de string a number
    CANCEL: (id: number) => `/api/flights/${id}/cancel`,
  },
  CLIENTS: {
    BASE: '/api/clients',
    BY_ID: (id: number) => `/api/clients/${id}`,
  },
} as const;
