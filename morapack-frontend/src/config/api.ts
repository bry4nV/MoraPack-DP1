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
