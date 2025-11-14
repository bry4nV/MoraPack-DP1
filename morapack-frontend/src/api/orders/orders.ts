import { API_CONFIG, API_ENDPOINTS } from '@/config/api';
import type { Order, CreateOrderDto } from '@/types/order';

const getFullUrl = (path: string) => `${API_CONFIG.BASE_URL}${path}`;

export const ordersApi = {
  async getOrders(): Promise<Order[]> {
    try {
      const url = getFullUrl(API_ENDPOINTS.ORDERS.BASE);
      const response = await fetch(url);
      if (!response.ok) throw new Error("Error al obtener pedidos");
      return response.json();
    } catch (error) {
      console.error("API Error getting orders:", error);
      throw error;
    }
  },

  async getOrderById(id: number): Promise<Order> {
    const url = getFullUrl(API_ENDPOINTS.ORDERS.BY_ID(id));
    const response = await fetch(url);
    if (!response.ok) throw new Error("Error al obtener pedido");
    return response.json();
  },

  async createOrder(payload: CreateOrderDto): Promise<Order> {
    try {
      const url = getFullUrl(API_ENDPOINTS.ORDERS.BASE);
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error("API Error creating order:", error);
      throw error;
    }
  },

  async updateOrder(id: number, payload: Partial<Order>): Promise<Order> {
    const url = getFullUrl(API_ENDPOINTS.ORDERS.BY_ID(id));
    const response = await fetch(url, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error("Error al actualizar pedido");
    return response.json();
  },

  async deleteOrder(id: number): Promise<void> {
    try {
      const url = getFullUrl(API_ENDPOINTS.ORDERS.BY_ID(id));
      const response = await fetch(url, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
        },
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }
    } catch (error) {
      console.error("API Error deleting order:", error);
      throw error;
    }
  },
};