"use client";

import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { toast } from "sonner";
import { Plus, Loader2 } from "lucide-react";

interface Props {
  aeropuertos: any[];
  onPedidoCreado: () => void; // Función para recargar la lista al guardar
}

export function NuevoPedidoModal({ aeropuertos, onPedidoCreado }: Props) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  // Estado del formulario
  const [formData, setFormData] = useState({
    cliente: "",
    cantidad: "",
    origen: "",
    destino: "",
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validaciones simples
    if (!formData.origen || !formData.destino) {
        toast.error("Debes seleccionar origen y destino");
        return;
    }
    if (formData.origen === formData.destino) {
        toast.error("El origen y destino no pueden ser iguales");
        return;
    }

    setLoading(true);

    try {
      // 1. Enviar datos al Backend
      // Ajusta la URL si tu endpoint es diferente (ej. /api/orders)
      const response = await fetch("http://localhost:8080/api/orders/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          clientName: formData.cliente,
          quantity: parseInt(formData.cantidad),
          originCode: formData.origen,
          destinationCode: formData.destino,
          status: "PENDING", // Estado inicial
          registrationDate: new Date().toISOString()
        }),
      });

      if (!response.ok) throw new Error("Error al guardar en servidor");

      // 2. Éxito
      toast.success("Pedido registrado correctamente");
      setOpen(false); // Cierra el modal
      setFormData({ cliente: "", cantidad: "", origen: "", destino: "" }); // Limpia el form
      
      // 3. ¡IMPORTANTE! Avisar al padre que recargue la lista
      onPedidoCreado(); 

    } catch (error) {
      console.error(error);
      toast.error("Error al conectar con el servidor");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {/* Botón Negro que activa el Modal */}
        <Button className="w-full bg-black hover:bg-gray-800 text-white shadow transition-colors">
          <Plus className="mr-2 h-4 w-4" /> Nuevo Pedido
        </Button>
      </DialogTrigger>
      
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Registrar Nuevo Pedido</DialogTitle>
        </DialogHeader>
        
        <form onSubmit={handleSubmit} className="grid gap-4 py-4">
          
          {/* Campo Cliente */}
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="cliente" className="text-right">Cliente</Label>
            <Input 
              id="cliente" 
              required
              className="col-span-3" 
              placeholder="Nombre del cliente"
              value={formData.cliente}
              onChange={(e) => setFormData({...formData, cliente: e.target.value})}
            />
          </div>
          
          {/* Campo Origen */}
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Origen</Label>
            <div className="col-span-3">
                <Select required onValueChange={(val) => setFormData({...formData, origen: val})}>
                    <SelectTrigger>
                        <SelectValue placeholder="Seleccionar aeropuerto" />
                    </SelectTrigger>
                    <SelectContent>
                        {aeropuertos.map((a) => (
                            <SelectItem key={a.code} value={a.code}>
                                {a.city} ({a.code})
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>
          </div>

          {/* Campo Destino */}
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Destino</Label>
            <div className="col-span-3">
                <Select required onValueChange={(val) => setFormData({...formData, destino: val})}>
                    <SelectTrigger>
                        <SelectValue placeholder="Seleccionar aeropuerto" />
                    </SelectTrigger>
                    <SelectContent>
                        {aeropuertos.map((a) => (
                            <SelectItem key={a.code} value={a.code}>
                                {a.city} ({a.code})
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>
          </div>

          {/* Campo Cantidad */}
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="cantidad" className="text-right">Paquetes</Label>
            <Input 
              id="cantidad" 
              type="number" 
              required
              min="1"
              className="col-span-3" 
              placeholder="0"
              value={formData.cantidad}
              onChange={(e) => setFormData({...formData, cantidad: e.target.value})}
            />
          </div>

          {/* Botones de Acción */}
          <div className="flex justify-end gap-3 mt-4">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={loading} className="bg-blue-600 hover:bg-blue-700">
              {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Guardar
            </Button>
          </div>

        </form>
      </DialogContent>
    </Dialog>
  );
}