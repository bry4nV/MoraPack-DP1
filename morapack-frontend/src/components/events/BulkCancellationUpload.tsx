"use client";

import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Upload, File, AlertCircle, CheckCircle, X, FileText, Calendar } from "lucide-react";

interface BulkCancellationUploadProps {
  onCancellationsUploaded: () => void;
}

interface ParsedCancellation {
  day: string;
  origin: string;
  destination: string;
  time: string;
  line: string;
  lineNumber: number;
}

export default function BulkCancellationUpload({
  onCancellationsUploaded,
}: BulkCancellationUploadProps) {
  const [selectedMonth, setSelectedMonth] = useState<string>("12");
  const [selectedYear, setSelectedYear] = useState<string>("2025");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ParsedCancellation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [successCount, setSuccessCount] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const months = [
    { value: "01", label: "Enero" },
    { value: "02", label: "Febrero" },
    { value: "03", label: "Marzo" },
    { value: "04", label: "Abril" },
    { value: "05", label: "Mayo" },
    { value: "06", label: "Junio" },
    { value: "07", label: "Julio" },
    { value: "08", label: "Agosto" },
    { value: "09", label: "Septiembre" },
    { value: "10", label: "Octubre" },
    { value: "11", label: "Noviembre" },
    { value: "12", label: "Diciembre" },
  ];

  const years = ["2024", "2025", "2026", "2027"];

  // Auto-hide success message after 5 seconds
  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => {
        setSuccess(false);
        setSuccessCount(0);
      }, 5000); // 5 segundos

      return () => clearTimeout(timer);
    }
  }, [success]);

  const processFile = async (file: File) => {
    setSelectedFile(file);
    setError(null);
    setSuccess(false);

    // Leer y parsear el archivo
    try {
      const text = await file.text();
      const parsed = parseCancellationsFile(text);
      setPreview(parsed);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al leer el archivo");
      setPreview([]);
    }
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    await processFile(file);
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = async (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    if (loading) return;

    const file = e.dataTransfer.files?.[0];
    if (!file) return;

    // Validar extensión
    if (!file.name.endsWith('.csv') && !file.name.endsWith('.txt')) {
      setError("Por favor selecciona un archivo CSV o TXT");
      return;
    }

    await processFile(file);
  };

  const parseCancellationsFile = (content: string): ParsedCancellation[] => {
    // Remove BOM if present
    const cleanContent = content.replace(/^\uFEFF/, '');
    const lines = cleanContent.split(/\r?\n/).map(line => line.trim()).filter(line => line);
    const cancellations: ParsedCancellation[] = [];

    lines.forEach((line, index) => {
      // Skip header if present (check for common header keywords)
      if (index === 0 && (
        line.toLowerCase().includes('dia') ||
        line.toLowerCase().includes('day') ||
        line.toLowerCase().includes('origen') ||
        line.toLowerCase().includes('origin') ||
        line.toLowerCase().includes('hora')
      )) {
        return;
      }

      if (!line) return;

      // Formato CSV: dia,origen,destino,hora_salida (separado por comas)
      const parts = line.split(',').map(p => p.trim());

      if (parts.length < 4) {
        throw new Error(`Línea ${index + 1}: Formato inválido. Debe tener 4 columnas separadas por coma (encontrado: ${parts.length} columnas en "${line}")`);
      }

      const day = parts[0].padStart(2, '0');
      const origin = parts[1].toUpperCase();
      const destination = parts[2].toUpperCase();
      // Remove colons and any whitespace from time
      const time = parts[3].replace(/[:\s]/g, '');

      // Validar día (01-31)
      const dayNum = parseInt(day);
      if (isNaN(dayNum) || dayNum < 1 || dayNum > 31) {
        throw new Error(`Línea ${index + 1}: Día inválido "${parts[0]}". Debe ser 01-31`);
      }

      // Validar códigos de aeropuerto (4 letras)
      if (!/^[A-Z]{4}$/.test(origin)) {
        throw new Error(`Línea ${index + 1}: Código de origen inválido "${parts[1]}". Debe ser 4 letras (ej: SPIM)`);
      }

      if (!/^[A-Z]{4}$/.test(destination)) {
        throw new Error(`Línea ${index + 1}: Código de destino inválido "${parts[2]}". Debe ser 4 letras (ej: SCEL)`);
      }

      // Validar formato de tiempo (debe ser exactamente 4 dígitos después de limpiar)
      if (!/^\d{4}$/.test(time)) {
        throw new Error(`Línea ${index + 1}: Hora inválida "${parts[3]}" (limpiado: "${time}"). Debe ser formato HHmm o HH:mm (ej: 0800 o 08:00)`);
      }

      cancellations.push({
        day,
        origin,
        destination,
        time,
        line,
        lineNumber: index + 1,
      });
    });

    return cancellations;
  };

  const handleUpload = async () => {
    if (preview.length === 0) {
      setError("Debes seleccionar un archivo primero");
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      // Convertir las cancelaciones parseadas al formato del backend
      const cancellationsData = preview.map(c => ({
        day: parseInt(c.day),
        origin: c.origin,
        destination: c.destination,
        departureTime: c.time, // HHmm format
      }));

      const startDate = `${selectedYear}-${selectedMonth}-01`;

      // Enviar las cancelaciones al backend
      const response = await fetch('/api/simulation/events/bulk-cancel-flights', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          cancellations: cancellationsData,
          startDate: startDate,
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();

      if (!data.success) {
        throw new Error(data.message || "Error al cargar cancelaciones");
      }

      // Guardar el conteo ANTES de limpiar el preview
      const uploadedCount = preview.length;

      setSuccessCount(uploadedCount);
      setSuccess(true);
      setPreview([]);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';

      // Refresh lists
      setTimeout(() => onCancellationsUploaded(), 500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al cargar cancelaciones");
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setSelectedFile(null);
    setPreview([]);
    setError(null);
    setSuccess(false);
    setSuccessCount(0);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  return (
    <div className="space-y-2">
      {/* Selectores de Mes y Año */}
      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label htmlFor="month" className="text-xs">Mes</Label>
          <Select value={selectedMonth} onValueChange={setSelectedMonth} disabled={loading}>
            <SelectTrigger id="month" className="h-9 text-sm">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {months.map((month) => (
                <SelectItem key={month.value} value={month.value} className="text-sm">
                  {month.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="year" className="text-xs">Año</Label>
          <Select value={selectedYear} onValueChange={setSelectedYear} disabled={loading}>
            <SelectTrigger id="year" className="h-9 text-sm">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {years.map((year) => (
                <SelectItem key={year} value={year} className="text-sm">
                  {year}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Indicador de mes seleccionado */}
      <div className="text-xs text-muted-foreground bg-slate-50 p-2 rounded border flex items-center gap-2">
        <Calendar className="h-3.5 w-3.5" />
        <span>
          Las cancelaciones se programarán para: <span className="font-semibold">{months.find(m => m.value === selectedMonth)?.label} {selectedYear}</span>
        </span>
      </div>

      {/* Drag & Drop Zone */}
      <div className="space-y-1.5">
        <Label className="text-xs">Cargar Archivo CSV</Label>
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`
            relative border-2 border-dashed rounded-lg p-4 transition-all cursor-pointer
            ${isDragging
              ? 'border-blue-500 bg-blue-50 dark:bg-blue-950'
              : selectedFile
                ? 'border-green-400 bg-green-50 dark:bg-green-950'
                : 'border-gray-300 hover:border-gray-400 bg-gray-50 dark:bg-gray-900'
            }
            ${loading ? 'opacity-50 cursor-not-allowed' : ''}
          `}
          onClick={() => !loading && fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".txt,.csv"
            onChange={handleFileSelect}
            className="hidden"
            disabled={loading}
          />

          <div className="flex flex-col items-center gap-2 text-center">
            {selectedFile ? (
              <>
                <File className="h-8 w-8 text-green-600" />
                <div className="space-y-0.5">
                  <p className="text-sm font-medium text-green-700 dark:text-green-400">
                    {selectedFile.name}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {(selectedFile.size / 1024).toFixed(2)} KB
                  </p>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="mt-1 h-7 text-xs"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleClear();
                  }}
                  disabled={loading}
                >
                  <X className="h-3 w-3 mr-1" />
                  Quitar archivo
                </Button>
              </>
            ) : (
              <>
                <Upload className="h-8 w-8 text-gray-400" />
                <div className="space-y-0.5">
                  <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {isDragging ? '¡Suelta el archivo aquí!' : 'Arrastra un archivo CSV aquí'}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    o haz clic para seleccionar
                  </p>
                </div>
                <Badge variant="outline" className="mt-1 text-[10px]">
                  .csv o .txt
                </Badge>
              </>
            )}
          </div>
        </div>
        <div className="text-[10px] text-muted-foreground space-y-0.5">
          <p><strong>Formato:</strong> dia,origen,destino,hora_salida</p>
          <pre className="text-[9px] bg-slate-100 p-1 rounded">01,SPIM,SCEL,0800
05,SKBO,SEQM,1430</pre>
          <p className="text-[9px] italic">Con o sin encabezado. Hora: HHmm o HH:mm</p>
        </div>
      </div>

      {/* Preview */}
      {preview.length > 0 && (
        <Card className="border">
          <CardContent className="p-3">
            <div className="flex items-center justify-between mb-2">
              <Label className="text-xs font-semibold">Vista Previa</Label>
              <Badge variant="outline" className="text-[10px]">
                {preview.length} cancelación{preview.length !== 1 && 'es'}
              </Badge>
            </div>
            <div className="max-h-28 overflow-y-auto space-y-1">
              {preview.map((c, idx) => (
                <div key={idx} className="text-xs flex items-center gap-2 p-1 bg-slate-50 rounded">
                  <FileText className="h-3 w-3 text-muted-foreground flex-shrink-0" />
                  <span className="font-mono text-[11px] flex-1">
                    Día {c.day}: {c.origin} → {c.destination} a las {c.time.slice(0, 2)}:{c.time.slice(2)}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Messages */}
      {error && (
        <div className="text-xs text-red-600 bg-red-50 p-2 rounded border border-red-200 flex items-start gap-2">
          <AlertCircle className="h-3.5 w-3.5 mt-0.5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="text-xs text-green-600 bg-green-50 p-2 rounded border border-green-200 flex items-center gap-2">
          <CheckCircle className="h-3.5 w-3.5" />
          <span>{successCount} cancelación{successCount !== 1 ? 'es' : ''} cargada{successCount !== 1 ? 's' : ''} exitosamente</span>
        </div>
      )}

      {/* Upload button */}
      <Button
        type="button"
        onClick={handleUpload}
        disabled={loading || preview.length === 0}
        className="w-full h-9 text-sm"
      >
        {loading ? "Cargando..." : `Cargar ${preview.length} Cancelación${preview.length !== 1 ? 'es' : ''}`}
      </Button>
    </div>
  );
}
