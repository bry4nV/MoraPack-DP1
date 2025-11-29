"use client";

import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Plane, Package, MapPin, Clock, AlertCircle, CheckCircle2, Loader2 } from "lucide-react";
import { getFlightCargo } from "@/lib/flights-api";
import type { FlightCargoResponse, ShipmentStatus } from "@/types/simulation/flights.types";

interface FlightDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  flightId: string;
  userId: string;
}

export function FlightDetailsModal({ isOpen, onClose, flightId, userId }: FlightDetailsModalProps) {
  const [cargoData, setCargoData] = useState<FlightCargoResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load cargo data when modal opens
  useEffect(() => {
    if (isOpen && flightId && userId) {
      loadCargoData();
    }
  }, [isOpen, flightId, userId]);

  const loadCargoData = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getFlightCargo(userId, flightId);
      setCargoData(data);
    } catch (err) {
      console.error("Error loading flight cargo:", err);
      setError("No se pudo cargar la información del vuelo");
    } finally {
      setLoading(false);
    }
  };

  const getShipmentStatusBadge = (status: ShipmentStatus) => {
    const config = {
      PENDING: { label: "Pendiente", className: "bg-gray-100 text-gray-800" },
      IN_TRANSIT: { label: "En Tránsito", className: "bg-blue-100 text-blue-800" },
      DELIVERED: { label: "Entregado", className: "bg-green-100 text-green-800" },
    };
    const { label, className } = config[status];
    return <Badge className={className}>{label}</Badge>;
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Plane className="h-5 w-5" />
            Vuelo {flightId}
          </DialogTitle>
        </DialogHeader>

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : error ? (
          <div className="flex items-center justify-center py-12 text-red-600">
            <AlertCircle className="h-8 w-8 mr-2" />
            <span>{error}</span>
          </div>
        ) : cargoData ? (
          <div className="space-y-4">
            {/* Flight Info */}
            <Card>
              <CardContent className="pt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <MapPin className="h-4 w-4 text-muted-foreground" />
                    <span className="font-semibold">
                      {cargoData.origin} → {cargoData.destination}
                    </span>
                  </div>
                  {cargoData.cancelled && (
                    <Badge variant="destructive">Cancelado</Badge>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <span className="text-muted-foreground">Salida:</span>
                    <div className="font-medium">
                      {new Date(cargoData.scheduledDeparture).toLocaleString("es-ES", {
                        dateStyle: "short",
                        timeStyle: "short",
                      })}
                    </div>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Llegada:</span>
                    <div className="font-medium">
                      {new Date(cargoData.scheduledArrival).toLocaleString("es-ES", {
                        dateStyle: "short",
                        timeStyle: "short",
                      })}
                    </div>
                  </div>
                </div>

                <div className="pt-2 border-t">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Package className="h-4 w-4 text-muted-foreground" />
                      <span className="text-sm font-semibold">Carga Total</span>
                    </div>
                    <div className="text-right">
                      <div className="text-lg font-bold">{cargoData.totalQuantity} unidades</div>
                      <div className="text-xs text-muted-foreground">
                        {cargoData.totalShipments} envío{cargoData.totalShipments !== 1 ? "s" : ""}
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Shipments List */}
            <div>
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <Package className="h-4 w-4" />
                Envíos Transportados ({cargoData.totalShipments})
              </h3>

              {cargoData.shipments.length === 0 ? (
                <Card>
                  <CardContent className="py-8 text-center text-muted-foreground">
                    <Package className="h-12 w-12 mx-auto mb-2 opacity-30" />
                    <p>Este vuelo no transporta envíos</p>
                  </CardContent>
                </Card>
              ) : (
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {cargoData.shipments.map((shipment, index) => (
                    <Card key={`${shipment.orderId}-${index}`} className="hover:bg-accent/50 transition-colors">
                      <CardContent className="p-3 space-y-2">
                        {/* Header */}
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs font-semibold text-muted-foreground">
                              #{shipment.orderId}
                            </span>
                            <Badge variant="outline" className="text-xs">
                              {shipment.quantity} unidades
                            </Badge>
                          </div>
                          {getShipmentStatusBadge(shipment.status)}
                        </div>

                        {/* Route */}
                        <div className="flex items-center gap-2 text-xs">
                          <MapPin className="h-3 w-3 text-muted-foreground flex-shrink-0" />
                          <div className="flex items-center gap-1 flex-wrap">
                            <span className="font-medium">{shipment.orderOrigin}</span>
                            <span className="text-muted-foreground">→</span>
                            <span className="font-medium">{shipment.orderDestination}</span>
                          </div>
                        </div>

                        {/* Multi-hop route if applicable */}
                        {shipment.route.length > 1 && (
                          <div className="text-xs text-muted-foreground pl-5">
                            <span className="font-semibold">Ruta:</span> {shipment.route.join(" → ")}
                          </div>
                        )}

                        {/* Deadline */}
                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                          <Clock className="h-3 w-3 flex-shrink-0" />
                          <span>
                            Deadline:{" "}
                            {new Date(shipment.deadline).toLocaleString("es-ES", {
                              dateStyle: "short",
                              timeStyle: "short",
                            })}
                          </span>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </div>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}
