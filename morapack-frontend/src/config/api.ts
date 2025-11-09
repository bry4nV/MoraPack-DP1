// API Configuration
export const API_CONFIG = {
  BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
  WS_URL: process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws',
  TIMEOUT: 30000, // 30 seconds
  RETRY_ATTEMPTS: 3,
} as const;

// API Endpoints
export const API_ENDPOINTS = {
  // Order endpoints
  ORDERS: {
    BASE: '/api/orders',  // ✅ Cambiado de '/orders/' a '/api/orders'
    BY_ID: (id: string) => `/api/orders/${id}`,  // ✅ Sin barra al final
    BULK_CREATE: '/api/orders/bulk-create',  // ✅ Sin barra al final
    BULK_DELETE: '/api/orders/bulk-delete',  // ✅ Sin barra al final
  },
  AIRPORTS: {
    BASE: '/api/airports',
    BY_ID: (id: string) => `/api/airports/${id}`,
    BULK_CREATE: '/api/airports/bulk-create',
    BULK_DELETE: '/api/airports/bulk-delete',
  },

  FLIGHTS: {
    BASE: '/api/flights', // <-- Este es el endpoint que creamos en el backend
    BY_ID: (id: string) => `/api/flights/${id}`,
    // (Puedes añadir bulk_create/delete si los necesitas)
  },

} as const;

// HTTP Status Codes
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500,
} as const;
