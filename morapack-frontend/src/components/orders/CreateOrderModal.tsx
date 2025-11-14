"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { ordersApi } from "@/api/orders/orders";
import { CreateOrderDto } from "@/types/order";
import { useState } from "react";

// 2. Definimos las "reglas" del formulario con Zod
const formSchema = z.object({
  orderNumber: z.string().min(1, "El Nro. de Pedido es requerido"),
  clientCode: z.string().min(1, "El Cliente es requerido"),
  airportDestinationCode: z.string().min(1, "El Destino es requerido"),
  
  // --- ¡CAMBIO 1! ---
  // Tratamos la cantidad como 'string' para evitar el error de tipos.
  // El <Input type="number"> seguirá funcionando.
  quantity: z.string().min(1, "La cantidad es requerida"),
});

// 3. Definimos las propiedades que recibirá el componente
interface CreateOrderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onOrderCreated: () => void; 
}

export function CreateOrderModal({
  isOpen,
  onClose,
  onOrderCreated,
}: CreateOrderModalProps) {
  
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 4. Configuramos el formulario
  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema), // <-- El error rojo desaparecerá
    defaultValues: {
      orderNumber: "",
      clientCode: "",
      airportDestinationCode: "",
      
      // --- ¡CAMBIO 2! ---
      // El valor por defecto ahora es un string "1"
      quantity: "1",
    },
  });

  // 5. La función que se llama al enviar el formulario
  async function onSubmit(values: z.infer<typeof formSchema>) {
    setIsSubmitting(true);
    try {
      // Llamamos a la API
      const payload: CreateOrderDto = {
        ...values,
        // (La conversión a 'number' que ya teníamos
        // ahora es aún más importante)
        quantity: Number(values.quantity), 
      };
      await ordersApi.createOrder(payload);
      
      onOrderCreated(); 
      onClose(); 
      form.reset(); 

    } catch (error) {
      console.error("Error al crear el pedido:", error);
    } finally {
      setIsSubmitting(false);
    }
  }

  // Función para manejar el cierre del modal
  const handleClose = () => {
    if (isSubmitting) return; 
    form.reset();
    onClose();
  };

  // 6. El JSX (Sin cambios)
  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Agregar Nuevo Pedido</DialogTitle>
          <DialogDescription>
            Complete los campos para registrar un nuevo pedido. La fecha y hora
            serán registradas automáticamente.
          </DialogDescription>
        </DialogHeader>
        
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="orderNumber"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nro. Pedido</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: 000000267" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="clientCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cliente</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: 0007729" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="airportDestinationCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Destino (Código)</FormLabel>
                  <FormControl>
                    <Input placeholder="Ej: EBCI" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="quantity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Paquetes</FormLabel>
                  <FormControl>
                    {/* El 'type="number"' funciona bien, 
                        aunque el estado de React sea 'string' */}
                    <Input type="number" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="flex justify-end gap-2 pt-4">
              <Button
                type="button"
                variant="ghost"
                onClick={handleClose}
                disabled={isSubmitting}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Guardando..." : "Guardar Pedido"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}