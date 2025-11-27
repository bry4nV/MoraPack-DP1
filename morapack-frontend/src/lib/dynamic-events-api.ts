/**
 * API client for dynamic events (flight cancellations and dynamic orders)
 */

import { API_CONFIG, API_ENDPOINTS } from '@/config/api';
import type {
  FlightCancellation,
  DynamicOrder,
  CancelFlightRequest,
  DynamicOrderRequest,
  CancellationApiResponse,
  DynamicOrderApiResponse,
  CancellationsListResponse,
  DynamicOrdersListResponse,
} from '@/types/simulation/events.types';

const baseUrl = API_CONFIG.BASE_URL;

/**
 * Cancel a flight manually
 */
export async function cancelFlight(data: CancelFlightRequest): Promise<CancellationApiResponse> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.CANCEL_FLIGHT}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error cancelling flight:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

/**
 * Add a dynamic order manually
 */
export async function addDynamicOrder(data: DynamicOrderRequest): Promise<DynamicOrderApiResponse> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.ADD_ORDER}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error adding dynamic order:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

/**
 * Get all cancellations (pending and executed)
 */
export async function getCancellations(): Promise<FlightCancellation[]> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.GET_CANCELLATIONS}`);

    if (!response.ok) {
      console.warn(`Failed to fetch cancellations: HTTP ${response.status}`);
      return [];
    }

    // Check if response is JSON
    const contentType = response.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      console.warn('Cancellations endpoint returned non-JSON response');
      return [];
    }

    const data: CancellationsListResponse = await response.json();

    // 🔍 DEBUG: Log raw data from backend
    console.log('🔍 [getCancellations API] Total count:', data.count);
    console.log('🔍 [getCancellations API] Raw cancellations array:', data.cancellations);

    if (data.cancellations && data.cancellations.length > 0) {
      console.log('🔍 [getCancellations API] First cancellation:');
      console.log('   - ID:', data.cancellations[0].id);
      console.log('   - Status:', data.cancellations[0].status);
      console.log('   - Flight:', data.cancellations[0].flightIdentifier);
      console.log('   - Full object:', JSON.stringify(data.cancellations[0], null, 2));
    }

    return data.cancellations || [];
  } catch (error) {
    console.error('Error fetching cancellations:', error);
    return [];
  }
}

/**
 * Get executed cancellations only
 */
export async function getExecutedCancellations(): Promise<FlightCancellation[]> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.GET_CANCELLATIONS_EXECUTED}`);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const data: CancellationsListResponse = await response.json();
    return data.cancellations || [];
  } catch (error) {
    console.error('Error fetching executed cancellations:', error);
    return [];
  }
}

/**
 * Get all dynamic orders (pending and injected)
 */
export async function getDynamicOrders(): Promise<DynamicOrder[]> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.GET_ORDERS}`);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const data: DynamicOrdersListResponse = await response.json();
    return data.orders || [];
  } catch (error) {
    console.error('Error fetching dynamic orders:', error);
    return [];
  }
}

/**
 * Get injected orders only
 */
export async function getInjectedOrders(): Promise<DynamicOrder[]> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.GET_ORDERS_INJECTED}`);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const data: DynamicOrdersListResponse = await response.json();
    return data.orders || [];
  } catch (error) {
    console.error('Error fetching injected orders:', error);
    return [];
  }
}

/**
 * Load cancellations from a file (backend reads the file)
 */
export async function loadCancellationsFromFile(filePath: string, startDate: string): Promise<{ success: boolean; message: string; count?: number }> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.LOAD_CANCELLATIONS}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ filePath, startDate }),
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error loading cancellations from file:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

/**
 * Load dynamic orders from a file (backend reads the file)
 */
export async function loadOrdersFromFile(filePath: string, startDate: string): Promise<{ success: boolean; message: string; count?: number }> {
  try {
    const response = await fetch(`${baseUrl}${API_ENDPOINTS.DYNAMIC_EVENTS.LOAD_ORDERS}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ filePath, startDate }),
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error loading orders from file:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}


