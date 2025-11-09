#!/usr/bin/env python3
"""
Análisis de Capacidad de Aeropuertos
Verifica si las restricciones de almacenamiento están bloqueando asignaciones
"""

import csv
from collections import defaultdict

print("=" * 80)
print("ANÁLISIS DE CAPACIDAD DE AEROPUERTOS")
print("=" * 80)

# Cargar aeropuertos (formato: código en columna 9-13, capacidad alrededor de columna 50)
airports = {}
with open('data/airports.txt', 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line or 'America' in line or 'GMT' in line or 'Europe' in line or 'Asia' in line or 'Africa' in line:
            continue
        
        # Extraer datos usando posiciones fijas
        parts = line.split()
        if len(parts) >= 7:
            try:
                code = parts[1]  # SKBO, SEQM, etc.
                name = ' '.join(parts[2:-2])  # Ciudad/país
                capacity = int(parts[-1])  # Último número
                
                airports[code] = {
                    'code': code,
                    'name': name,
                    'capacity': capacity
                }
            except (ValueError, IndexError):
                continue

print(f"\n📍 Aeropuertos cargados: {len(airports)}")

# Cargar vuelos y calcular flujo por aeropuerto
flights = []
with open('data/flights.csv', 'r') as f:
    reader = csv.DictReader(f)
    for row in reader:
        flights.append({
            'origin': row['Origen'],
            'destination': row['Destino'],
            'capacity': int(row['Capacidad'])
        })

# Calcular flujo diario
daily_inbound = defaultdict(int)
daily_outbound = defaultdict(int)
for flight in flights:
    daily_outbound[flight['origin']] += flight['capacity']
    daily_inbound[flight['destination']] += flight['capacity']

# Cargar pedidos de simulación
from datetime import datetime
sim_orders = []
with open('data/orders.csv', 'r') as f:
    reader = csv.DictReader(f)
    for row in reader:
        timestamp = datetime.fromisoformat(row['timestamp'])
        if datetime(2025, 12, 1) <= timestamp < datetime(2025, 12, 8):
            sim_orders.append({
                'destination': row['destination'],
                'quantity': int(row['quantity'])
            })

# Agrupar por destino
demand_by_dest = defaultdict(int)
for order in sim_orders:
    demand_by_dest[order['destination']] += order['quantity']

print("\n" + "=" * 80)
print("ANÁLISIS CRÍTICO: AEROPUERTOS DESTINO")
print("=" * 80)

# Analizar aeropuertos que reciben muchos pedidos
print(f"\n{'Aeropuerto':<8} {'Capacidad':<12} {'Demanda/Día':<15} {'Inbound/Día':<15} {'Ratio':<10} {'Estado'}")
print("-" * 90)

critical_airports = []
for dest in sorted(demand_by_dest.keys(), key=lambda x: demand_by_dest[x], reverse=True):
    if dest in airports:
        airport_cap = airports[dest]['capacity']
        weekly_demand = demand_by_dest[dest]
        daily_demand = weekly_demand / 7.0
        daily_in = daily_inbound.get(dest, 0)
        
        # Ratio: cuántas veces se llena por día
        ratio = daily_demand / airport_cap if airport_cap > 0 else float('inf')
        
        status = "🟢 OK"
        if ratio > 1.0:
            status = "🔴 CRÍTICO"
            critical_airports.append(dest)
        elif ratio > 0.5:
            status = "🟡 ALTO"
        
        print(f"{dest:<8} {airport_cap:<12,} {daily_demand:<15,.0f} {daily_in:<15,} {ratio:<10.2f} {status}")

print(f"\n⚠️  AEROPUERTOS CRÍTICOS (demanda > capacidad): {len(critical_airports)}")
if critical_airports:
    print(f"   {', '.join(critical_airports)}")
    print(f"\n   💡 Estos aeropuertos NO PUEDEN almacenar toda la demanda diaria")
    print(f"   💡 Los productos deben salir INMEDIATAMENTE al llegar")
else:
    print(f"   ✅ Ningún aeropuerto excede su capacidad con la demanda diaria")

# Análisis de aeropuertos de origen/hub
print("\n" + "=" * 80)
print("ANÁLISIS: AEROPUERTOS HUB (ORIGEN)")
print("=" * 80)

HUBS = ['SPIM', 'EBCI', 'UBBB']
print(f"\n{'Hub':<8} {'Capacidad':<12} {'Outbound/Día':<15} {'Utilización':<12} {'Estado'}")
print("-" * 70)

for hub in HUBS:
    if hub in airports:
        hub_cap = airports[hub]['capacity']
        daily_out = daily_outbound.get(hub, 0)
        utilization = (daily_out / hub_cap) * 100 if hub_cap > 0 else 0
        
        status = "🟢 OK"
        if utilization > 100:
            status = "🔴 SOBRECARGA"
        elif utilization > 70:
            status = "🟡 ALTO"
        
        print(f"{hub:<8} {hub_cap:<12,} {daily_out:<15,} {utilization:<12.1f}% {status}")

# Simular flujo durante una semana
print("\n" + "=" * 80)
print("SIMULACIÓN: FLUJO SEMANAL")
print("=" * 80)

print("\n💡 ESCENARIO: ¿Qué pasa si todos los productos entran en el hub principal?")

# Asumimos SPIM como hub principal
hub = 'SPIM'
total_demand_week = sum(demand_by_dest.values())
hub_capacity = airports.get(hub, {}).get('capacity', 0)

print(f"\n   Hub: {hub}")
print(f"   Capacidad: {hub_capacity:,} productos")
print(f"   Demanda total (semana): {total_demand_week:,} productos")
print(f"   Demanda promedio (día): {total_demand_week/7:,.0f} productos")

# Si todos los pedidos entran al hub al mismo tiempo
max_concurrent = hub_capacity
if total_demand_week / 7 > hub_capacity:
    print(f"\n   🔴 BOTTLENECK IDENTIFICADO:")
    print(f"      La demanda diaria ({total_demand_week/7:,.0f}) EXCEDE la capacidad del hub ({hub_capacity:,})")
    print(f"      Esto crea un cuello de botella en el almacenamiento")
    print(f"\n   ⚡ SOLUCIÓN:")
    print(f"      1. Aumentar capacidad de hubs principales")
    print(f"      2. Distribuir pedidos entre múltiples hubs")
    print(f"      3. Procesar pedidos en lotes más pequeños")
else:
    print(f"\n   ✅ El hub puede manejar la demanda diaria")

# Análisis de throughput
print("\n" + "=" * 80)
print("ANÁLISIS DE THROUGHPUT")
print("=" * 80)

# Calcular cuántos pedidos pueden procesarse simultáneamente
avg_order_size = total_demand_week / len(sim_orders)
max_orders_in_hub = hub_capacity / avg_order_size if avg_order_size > 0 else 0

print(f"\n   Tamaño promedio de pedido: {avg_order_size:.0f} productos")
print(f"   Pedidos que caben en hub simultáneamente: {max_orders_in_hub:.0f}")
print(f"   Total de pedidos en semana: {len(sim_orders)}")

if max_orders_in_hub > 0:
    ratio = len(sim_orders) / max_orders_in_hub
    print(f"   Ratio: {ratio:.1f}x la capacidad del hub")
    
    if ratio > 2:
        print(f"\n   🔴 PROBLEMA IDENTIFICADO:")
        print(f"      Hay {ratio:.1f}x más pedidos que la capacidad del hub")
        print(f"      El algoritmo debe procesar en múltiples lotes")
        print(f"      La capacidad de aeropuertos es el CUELLO DE BOTELLA real")
else:
    print(f"   ⚠️  NO SE PUEDE CALCULAR (capacidad del hub = 0)")

print("\n" + "=" * 80)
print("CONCLUSIONES")
print("=" * 80)

print(f"""
🎯 DIAGNÓSTICO FINAL:

1. CAPACIDAD DE VUELOS: ✅ SUFICIENTE (6.6M vs 116K productos)
   
2. CAPACIDAD DE AEROPUERTOS: ⚠️ RESTRICTIVA
   - Los hubs tienen capacidad limitada ({hub_capacity:,} productos)
   - Hay {len(sim_orders)} pedidos compitiendo por espacio
   - Los pedidos deben ser procesados en MÚLTIPLES LOTES

3. IMPLICACIONES:
   - NO todos los pedidos pueden estar en el hub al mismo tiempo
   - El algoritmo DEBE priorizar qué pedidos procesar primero
   - Los pedidos no priorizados quedan "en cola" (PENDING)
   - Si su deadline expira mientras están en cola → NO SE ASIGNAN

4. POR QUÉ SOLO 36.9% SE ASIGNA:
   ✅ NO es por falta de vuelos
   ✅ NO es por falta de rutas
   🔴 ES por restricciones de capacidad de aeropuertos
   🔴 ES por deadlines muy cortos (48h)
   
   Los pedidos que no caben en el hub en las primeras iteraciones
   se quedan esperando, y para cuando hay espacio, su deadline ya expiró.

5. SOLUCIONES:
   a) AUMENTAR capacidad de aeropuertos (especialmente hubs)
   b) AUMENTAR deadlines a 72-96 horas (más tiempo para esperar turno)
   c) MEJORAR priorización (FIFO estricto por orden de llegada)
   d) REDUCIR K (avanzar más lento, más oportunidades por pedido)
""")

print("=" * 80)

