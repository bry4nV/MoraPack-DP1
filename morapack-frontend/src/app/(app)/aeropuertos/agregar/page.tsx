"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { ArrowLeft, Save, X } from "lucide-react";
import { airportsApi } from "@/api/airports/airports";
import { Continent, type CreateAirportPayload } from "@/types/airport";

// Importación dinámica del CoordinatePicker (solo cliente)
const CoordinatePicker = dynamic(
  () => import("@/components/airports/coordinate-picker").then(mod => ({ default: mod.CoordinatePicker })),
  { ssr: false }
);

const CONTINENTS = [
  { value: Continent.AMERICA_DEL_SUR, label: "America del Sur." },
  { value: Continent.EUROPA, label: "Europa" },
  { value: Continent.ASIA, label: "Asia" }
];

export default function AgregarAeropuertoPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<Partial<CreateAirportPayload>>({
    continent: undefined,
    code: "",
    city: "",
    country: "",
    cityAcronym: "",
    gmt: undefined,
    capacity: undefined,
    latitude: "",
    longitude: "",
    isHub: false,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // Validar longitud del código
      if (formData.code && formData.code.length > 4) {
        throw new Error("El código IATA debe tener máximo 4 caracteres");
      }

      // Validar que todos los campos requeridos estén presentes
      if (!formData.continent || !formData.code || !formData.city || 
          !formData.country || !formData.cityAcronym || 
          formData.gmt === undefined || formData.capacity === undefined ||
          !formData.latitude || !formData.longitude) {
        throw new Error("Por favor complete todos los campos requeridos");
      }

      const payload: CreateAirportPayload = {
        continent: formData.continent,
        code: formData.code.toUpperCase().trim(),
        city: formData.city.trim(),
        country: formData.country.trim(),
        cityAcronym: formData.cityAcronym.toUpperCase().trim(),
        gmt: formData.gmt,
        capacity: formData.capacity,
        latitude: formData.latitude.trim(),
        longitude: formData.longitude.trim(),
        isHub: Boolean(formData.isHub), // Asegurarse de que sea boolean
      };

      console.log("=== DEBUG COMPLETO ===");
      console.log("formData.isHub:", formData.isHub);
      console.log("typeof formData.isHub:", typeof formData.isHub);
      console.log("payload.isHub:", payload.isHub);
      console.log("typeof payload.isHub:", typeof payload.isHub);
      console.log("Payload completo:", JSON.stringify(payload, null, 2));

      const result = await airportsApi.createAirport(payload);
      console.log("Aeropuerto creado:", result);
      
      // Redirigir a la lista de aeropuertos
      router.push("/aeropuertos");
    } catch (error) {
      console.error("Error al crear aeropuerto:", error);
      const errorMessage = error instanceof Error ? error.message : "Error desconocido al crear el aeropuerto";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    router.push("/aeropuertos");
  };

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span>Todos los aeropuertos</span>
        <span>/</span>
        <span className="text-foreground">Agregar aeropuerto</span>
      </div>

      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={handleCancel}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-4xl font-bold tracking-tight">Gestión de Aeropuertos</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Crear un nuevo aeropuerto en el sistema
          </p>
        </div>
      </div>

      {/* Form */}
      <Card>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-8">
            {/* Mostrar errores */}
            {error && (
              <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-md">
                <p className="font-semibold">Error</p>
                <p className="text-sm">{error}</p>
              </div>
            )}

            {/* Datos básicos */}
            <div className="space-y-4">
              <h3 className="text-base font-semibold text-muted-foreground">Datos básicos</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Código */}
                <div className="space-y-2">
                  <Label htmlFor="code">Código IATA *</Label>
                  <Input
                    id="code"
                    value={formData.code}
                    onChange={(e) => {
                      const value = e.target.value.toUpperCase().slice(0, 4); // Limitar a 4 caracteres
                      setFormData(prev => ({ ...prev, code: value }));
                    }}
                    placeholder="ej. LIM"
                    maxLength={4}
                    required
                  />
                  <p className="text-xs text-muted-foreground">
                    Exactamente 4 caracteres ({formData.code?.length || 0}/4)
                  </p>
                </div>

                {/* Acrónimo de ciudad */}
                <div className="space-y-2">
                  <Label htmlFor="cityAcronym">Acrónimo de ciudad *</Label>
                  <Input
                    id="cityAcronym"
                    value={formData.cityAcronym}
                    onChange={(e) => setFormData(prev => ({ ...prev, cityAcronym: e.target.value.toUpperCase() }))}
                    placeholder="ej. LIM"
                    maxLength={10}
                    required
                  />
                </div>

                {/* Ciudad */}
                <div className="space-y-2">
                  <Label htmlFor="city">Ciudad *</Label>
                  <Input
                    id="city"
                    value={formData.city}
                    onChange={(e) => setFormData(prev => ({ ...prev, city: e.target.value }))}
                    placeholder="ej. Lima"
                    maxLength={100}
                    required
                  />
                </div>

                {/* País */}
                <div className="space-y-2">
                  <Label htmlFor="country">País *</Label>
                  <Input
                    id="country"
                    value={formData.country}
                    onChange={(e) => setFormData(prev => ({ ...prev, country: e.target.value }))}
                    placeholder="ej. Perú"
                    maxLength={100}
                    required
                  />
                </div>

                {/* Continente */}
                <div className="space-y-2">
                  <Label htmlFor="continent">Continente *</Label>
                  <Select
                    value={formData.continent}
                    onValueChange={(value) => setFormData(prev => ({ ...prev, continent: value as Continent }))}
                    required
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Seleccione el continente" />
                    </SelectTrigger>
                    <SelectContent>
                      {CONTINENTS.map((continent) => (
                        <SelectItem key={continent.value} value={continent.value}>
                          {continent.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* GMT */}
                <div className="space-y-2">
                  <Label htmlFor="gmt">GMT (Zona horaria) *</Label>
                  <Input
                    id="gmt"
                    type="number"
                    min={-12}
                    max={14}
                    value={formData.gmt ?? ""}
                    onChange={(e) => setFormData(prev => ({ 
                      ...prev, 
                      gmt: e.target.value === "" ? undefined : parseInt(e.target.value) 
                    }))}
                    placeholder="ej. -5"
                    required
                  />
                  <p className="text-xs text-muted-foreground">Rango: -12 a +14</p>
                </div>

                {/* Capacidad */}
                <div className="space-y-2">
                  <Label htmlFor="capacity">Capacidad del almacén *</Label>
                  <Input
                    id="capacity"
                    type="number"
                    min={0}
                    value={formData.capacity ?? ""}
                    onChange={(e) => setFormData(prev => ({ 
                      ...prev, 
                      capacity: e.target.value === "" ? undefined : parseInt(e.target.value) 
                    }))}
                    placeholder="ej. 1000"
                    required
                  />
                </div>

                {/* Es Sede */}
                <div className="space-y-2 flex flex-col justify-end">
                  <div className="flex items-center space-x-2 h-10">
                    <Switch
                      id="isHub"
                      checked={formData.isHub || false}
                      onCheckedChange={(checked) => {
                        console.log("Switch changed to:", checked); // DEBUG
                        setFormData(prev => ({ ...prev, isHub: checked }));
                      }}
                    />
                    <Label htmlFor="isHub" className="cursor-pointer">
                      Es sede principal {formData.isHub ? "(Sí)" : "(No)"}
                    </Label>
                  </div>
                </div>
              </div>
            </div>

            {/* Separador */}
            <div className="border-t"></div>

            {/* Coordenadas geográficas */}
            <div className="space-y-4">
              <h3 className="text-base font-semibold text-muted-foreground">Coordenadas geográficas</h3>        

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Latitud */}
                <div className="space-y-2">
                  <Label htmlFor="latitude">Latitud *</Label>
                  <Input
                    id="latitude"
                    value={formData.latitude}
                    onChange={(e) => setFormData(prev => ({ ...prev, latitude: e.target.value }))}
                    placeholder="ej. 12° 02' 04&quot; S"
                    maxLength={20}
                    required
                  />
                  <p className="text-xs text-muted-foreground">Formato: DMS (04° 42&apos; 05&quot; N) o decimal (-12.0464)</p>
                </div>

                {/* Longitud */}
                <div className="space-y-2">
                  <Label htmlFor="longitude">Longitud *</Label>
                  <Input
                    id="longitude"
                    value={formData.longitude}
                    onChange={(e) => setFormData(prev => ({ ...prev, longitude: e.target.value }))}
                    placeholder="ej. 77° 05' 04&quot; W"
                    maxLength={20}
                    required
                  />
                  <p className="text-xs text-muted-foreground">Formato: DMS (74° 08&apos; 49&quot; W) o decimal (-77.0428)</p>
                </div>
              </div>

              {/* Selector de mapa */}
              <div>
                <CoordinatePicker
                  latitude={formData.latitude || ""}
                  longitude={formData.longitude || ""}
                  onCoordinatesChange={(lat, lng) => {
                    setFormData(prev => ({
                      ...prev,
                      latitude: lat,
                      longitude: lng
                    }));
                  }}
                />
              </div>
            </div>

            {/* Botones - CORREGIDO */}
            <div className="flex justify-end gap-3 pt-6 border-t">
              <Button
                type="button"
                variant="outline"
                onClick={handleCancel}
                disabled={loading}
              >
                <X className="h-4 w-4 mr-2" />
                Cancelar
              </Button>
              <Button type="submit" disabled={loading}>
                <Save className="h-4 w-4 mr-2" />
                {loading ? "Guardando..." : "Confirmar"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
