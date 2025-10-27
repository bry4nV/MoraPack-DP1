import { apiClient } from '@/lib/api-client';
import { API_ENDPOINTS } from '@/config/api';
import { Order } from '@/types/order';

export const ordersApi = {
  getOrders: async (): Promise<Order[]> => {
    return await apiClient.get<Order[]>(API_ENDPOINTS.ORDERS.BASE);
  },

  getOrderById: async (id: string): Promise<Order> => {
    return await apiClient.get<Order>(API_ENDPOINTS.ORDERS.BY_ID(id));
  },

  createOrder: async (payload: Partial<Order>): Promise<Order> => {
    return await apiClient.post<Order>(API_ENDPOINTS.ORDERS.BASE, payload);
  },

  updateOrder: async (id: string, payload: Partial<Order>): Promise<Order> => {
    return await apiClient.patch<Order>(API_ENDPOINTS.ORDERS.BY_ID(id), payload);
  },

  deleteOrder: async (id: string): Promise<void> => {
    return await apiClient.delete<void>(API_ENDPOINTS.ORDERS.BY_ID(id));
  },
};
