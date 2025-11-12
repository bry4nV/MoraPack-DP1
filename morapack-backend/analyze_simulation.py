#!/usr/bin/env python3
"""
Análisis Exhaustivo de la Simulación MoraPack
Analiza vuelos, pedidos, conectividad y capacidad
"""

import csv
from datetime import datetime, timedelta
from collections import defaultdict, Counter
import sys

print("=" * 80)
print("ANÁLISIS EXHAUSTIVO DE SIMULACIÓN MORAPACK")
print("=" * 80)

# ==============================================================================
# 1. CARGAR DATOS
# ==============================================================================

print("\n[1/6] CARGANDO DATOS...")

flights = []
with open('data/flights.csv', 'r') as f:
    reader = csv.DictReader(f)
    for row in reader:
        origin = row['Origen']
        dest = row['Destino']
        hour_origin = row['HoraOrigen']
        hour_dest = row['HoraDestino']
        capacity = int(row['Capacidad'])
        
        flights.append({
            'origin': origin,
            'destination': dest,
            'departure_hour': hour_origin,
            'arrival_hour': hour_dest,
            'capacity': capacity
        })

orders = []
with open('data/orders.csv', 'r') as f:
    reader = csv.DictReader(f)
    for row in reader:
        timestamp = datetime.fromisoformat(row['timestamp'])
        destination = row['destination']
        quantity = int(row['quantity'])
        
        orders.append({
            'timestamp': timestamp,
            'destination': destination,
            'quantity': quantity
        })

print(f"   ✅ Vuelos cargados: {len(flights)}")
print(f"   ✅ Pedidos cargados: {len(orders)}")

# Filtrar pedidos en rango de simulación
sim_start = datetime(2025, 12, 1, 0, 0)
sim_end = datetime(2025, 12, 8, 0, 0)
sim_orders = [o for o in orders if sim_start <= o['timestamp'] < sim_end]
print(f"   ✅ Pedidos en simulación (2025-12-01 a 2025-12-08): {len(sim_orders)}")

# ==============================================================================
# 2. ANÁLISIS DE DISTRIBUCIÓN HORARIA DE VUELOS
# ==============================================================================

print("\n[2/6] DISTRIBUCIÓN HORARIA DE VUELOS")
print("-" * 80)

# Agrupar por hora de salida
flights_by_hour = defaultdict(list)
for flight in flights:
    hour = int(flight['departure_hour'].split(':')[0])
    flights_by_hour[hour].append(flight)

# Calcular estadísticas por hora
print("\n📊 Vuelos por franja horaria:")
print(f"{'Hora':<6} {'Vuelos':<8} {'Cap. Total':<12} {'Cap. Promedio':<15} {'Barra Visual'}")
print("-" * 80)

total_capacity_all = 0
for hour in sorted(flights_by_hour.keys()):
    hour_flights = flights_by_hour[hour]
    count = len(hour_flights)
    total_cap = sum(f['capacity'] for f in hour_flights)
    avg_cap = total_cap / count if count > 0 else 0
    total_capacity_all += total_cap
    
    # Barra visual (escala: cada █ = 10 vuelos)
    bar = '█' * (count // 10) + ('▌' if count % 10 >= 5 else '')
    
    print(f"{hour:02d}:00  {count:<8} {total_cap:<12,} {avg_cap:<15.0f} {bar}")

print(f"\n   💡 CAPACIDAD TOTAL DIARIA: {total_capacity_all:,} productos")
print(f"   💡 PRODUCTOS SOLICITADOS (semana): {sum(o['quantity'] for o in sim_orders):,}")

# Identificar horas con POCA capacidad
low_capacity_hours = [h for h in flights_by_hour.keys() 
                      if len(flights_by_hour[h]) < 50]
print(f"\n   ⚠️  HORAS CON BAJA CAPACIDAD (<50 vuelos): {sorted(low_capacity_hours)}")

# ==============================================================================
# 3. ANÁLISIS DE CAPACIDAD POR VENTANA TEMPORAL (Sc)
# ==============================================================================

print("\n[3/6] CAPACIDAD POR VENTANA TEMPORAL (Sc)")
print("-" * 80)

def analyze_window_capacity(window_minutes):
    """Analiza capacidad disponible en ventanas de Sc minutos"""
    # Simular 24 horas con ventanas de Sc minutos
    windows_per_day = (24 * 60) // window_minutes
    
    capacities = []
    for window_idx in range(windows_per_day):
        window_start_min = window_idx * window_minutes
        window_end_min = window_start_min + window_minutes
        
        window_start_hour = window_start_min / 60.0
        window_end_hour = window_end_min / 60.0
        
        # Contar vuelos que salen en esta ventana
        window_flights = []
        for flight in flights:
            dep_hour = int(flight['departure_hour'].split(':')[0])
            dep_min = int(flight['departure_hour'].split(':')[1])
            dep_decimal = dep_hour + dep_min / 60.0
            
            if window_start_hour <= dep_decimal < window_end_hour:
                window_flights.append(flight)
        
        total_cap = sum(f['capacity'] for f in window_flights)
        capacities.append({
            'start': f"{int(window_start_hour):02d}:{int((window_start_hour % 1) * 60):02d}",
            'flights': len(window_flights),
            'capacity': total_cap
        })
    
    return capacities

# Analizar diferentes valores de Sc
for sc_minutes in [120, 240, 360, 480, 720]:
    capacities = analyze_window_capacity(sc_minutes)
    
    avg_flights = sum(w['flights'] for w in capacities) / len(capacities)
    avg_cap = sum(w['capacity'] for w in capacities) / len(capacities)
    min_cap = min(w['capacity'] for w in capacities)
    max_cap = max(w['capacity'] for w in capacities)
    
    print(f"\n🕐 Sc = {sc_minutes} minutos ({sc_minutes/60:.1f} horas):")
    print(f"   Ventanas por día: {len(capacities)}")
    print(f"   Promedio vuelos/ventana: {avg_flights:.1f}")
    print(f"   Capacidad promedio/ventana: {avg_cap:,.0f} productos")
    print(f"   Capacidad mínima: {min_cap:,} | máxima: {max_cap:,}")
    
    # Calcular cuántos pedidos se pueden atender por ventana
    avg_order_size = sum(o['quantity'] for o in sim_orders) / len(sim_orders)
    orders_per_window = avg_cap / avg_order_size
    print(f"   📦 Pedidos que caben/ventana (promedio): {orders_per_window:.1f}")

# ==============================================================================
# 4. ANÁLISIS DE CONECTIVIDAD: RUTAS ORIGEN-DESTINO
# ==============================================================================

print("\n\n[4/6] ANÁLISIS DE CONECTIVIDAD")
print("-" * 80)

# Obtener origen de pedidos (asumimos SPIM como origen principal)
MAIN_ORIGINS = ['SPIM', 'EBCI', 'UBBB']  # Los 3 hubs principales

# Construir grafo de rutas
direct_routes = defaultdict(list)
for flight in flights:
    route_key = f"{flight['origin']}->{flight['destination']}"
    direct_routes[route_key].append(flight)

print(f"\n📍 RUTAS DIRECTAS DISPONIBLES: {len(direct_routes)}")

# Analizar destinos de pedidos en simulación
destinations = Counter(o['destination'] for o in sim_orders)
print(f"\n📦 DESTINOS ÚNICOS EN PEDIDOS: {len(destinations)}")
print(f"\nTop 20 destinos más solicitados:")
print(f"{'Destino':<8} {'Pedidos':<10} {'Vuelos Directos desde Hubs'}")
print("-" * 60)

for dest, count in destinations.most_common(20):
    # Contar vuelos directos desde cada hub
    direct_from_hubs = {}
    for origin in MAIN_ORIGINS:
        route_key = f"{origin}->{dest}"
        direct_from_hubs[origin] = len(direct_routes.get(route_key, []))
    
    direct_str = ' / '.join([f"{o}:{direct_from_hubs[o]}" for o in MAIN_ORIGINS])
    print(f"{dest:<8} {count:<10} {direct_str}")

# Identificar destinos SIN rutas directas desde ningún hub
print(f"\n⚠️  DESTINOS SIN VUELOS DIRECTOS desde ningún hub:")
no_direct_routes = []
for dest in destinations.keys():
    has_direct = any(f"{origin}->{dest}" in direct_routes for origin in MAIN_ORIGINS)
    if not has_direct:
        no_direct_routes.append(dest)
        print(f"   - {dest} ({destinations[dest]} pedidos)")

if not no_direct_routes:
    print("   ✅ Todos los destinos tienen al menos una ruta directa")

# ==============================================================================
# 5. ANÁLISIS DE CONECTIVIDAD CON 1 ESCALA
# ==============================================================================

print("\n[5/6] CONECTIVIDAD CON 1 ESCALA")
print("-" * 80)

# Construir mapa de aeropuertos intermedios
airports_as_hub = defaultdict(set)
for flight in flights:
    airports_as_hub[flight['origin']].add(flight['destination'])

# Para cada destino sin ruta directa, buscar rutas con 1 escala
for dest in no_direct_routes:
    print(f"\n🔍 Rutas posibles a {dest} con 1 escala:")
    found_routes = []
    
    for origin in MAIN_ORIGINS:
        # Buscar aeropuertos intermedios
        for intermediate in airports_as_hub[origin]:
            if dest in airports_as_hub[intermediate]:
                route_key1 = f"{origin}->{intermediate}"
                route_key2 = f"{intermediate}->{dest}"
                flights1 = len(direct_routes[route_key1])
                flights2 = len(direct_routes[route_key2])
                found_routes.append(f"   {origin}→{intermediate}→{dest} ({flights1}×{flights2} combinaciones)")
    
    if found_routes:
        for route in found_routes[:5]:  # Mostrar primeras 5
            print(route)
    else:
        print(f"   ❌ NO HAY RUTAS CON 1 ESCALA")

# ==============================================================================
# 6. SIMULACIÓN CON DIFERENTES CONFIGURACIONES
# ==============================================================================

print("\n\n[6/6] SIMULACIÓN DE ESCENARIOS")
print("=" * 80)

# Calcular demanda por día
orders_by_day = defaultdict(list)
for order in sim_orders:
    day = order['timestamp'].date()
    orders_by_day[day].append(order)

total_demand = sum(o['quantity'] for o in sim_orders)
print(f"\n📊 DEMANDA TOTAL (semana): {total_demand:,} productos")

# Configuraciones a probar
configs = [
    {'K': 24, 'Sc': 120, 'name': 'ACTUAL'},
    {'K': 24, 'Sc': 240, 'name': 'Sc x2'},
    {'K': 24, 'Sc': 360, 'name': 'Sc x3'},
    {'K': 12, 'Sc': 120, 'name': 'K /2'},
    {'K': 12, 'Sc': 240, 'name': 'K /2, Sc x2'},
    {'K': 8, 'Sc': 360, 'name': 'K /3, Sc x3'},
]

print(f"\n{'Configuración':<20} {'Iteraciones':<12} {'Vuelos/Iter':<15} {'Cap/Iter':<15} {'Cobertura Teórica'}")
print("-" * 90)

for config in configs:
    K = config['K']
    Sc = config['Sc']
    
    # Calcular iteraciones necesarias para 7 días
    total_minutes = 7 * 24 * 60
    iterations = total_minutes // Sc
    
    # Capacidad promedio por iteración
    window_caps = analyze_window_capacity(Sc)
    avg_flights_per_iter = sum(w['flights'] for w in window_caps) / len(window_caps)
    avg_cap_per_iter = sum(w['capacity'] for w in window_caps) / len(window_caps)
    
    # Capacidad total en toda la simulación
    # (Nota: cada ventana se repite múltiples veces en los 7 días)
    days = 7
    total_capacity = avg_cap_per_iter * iterations * days
    
    # Cobertura teórica (cuánto de la demanda se puede cubrir)
    coverage = min(100, (total_capacity / total_demand) * 100)
    
    print(f"{config['name']:<20} {iterations:<12} {avg_flights_per_iter:<15.1f} {avg_cap_per_iter:<15,.0f} {coverage:>5.1f}%")

# ==============================================================================
# RESUMEN Y RECOMENDACIONES
# ==============================================================================

print("\n\n" + "=" * 80)
print("RESUMEN Y RECOMENDACIONES")
print("=" * 80)

# Calcular métricas clave
flights_per_route = len(flights) / len(direct_routes)
avg_flight_capacity = sum(f['capacity'] for f in flights) / len(flights)
daily_capacity = total_capacity_all
weekly_capacity = daily_capacity * 7
weekly_demand = total_demand

print(f"\n📊 MÉTRICAS CLAVE:")
print(f"   • Vuelos totales: {len(flights):,}")
print(f"   • Rutas únicas: {len(direct_routes):,}")
print(f"   • Vuelos por ruta (promedio): {flights_per_route:.1f}")
print(f"   • Capacidad promedio por vuelo: {avg_flight_capacity:.0f} productos")
print(f"   • Capacidad diaria: {daily_capacity:,} productos")
print(f"   • Capacidad semanal: {weekly_capacity:,} productos")
print(f"   • Demanda semanal: {weekly_demand:,} productos")
print(f"   • Ratio capacidad/demanda: {(weekly_capacity/weekly_demand)*100:.1f}%")

print(f"\n🎯 DIAGNÓSTICO:")

# Diagnosticar el problema principal
if weekly_capacity < weekly_demand:
    print(f"   🔴 CAPACIDAD INSUFICIENTE:")
    print(f"      La capacidad semanal ({weekly_capacity:,}) es MENOR que la demanda ({weekly_demand:,})")
    print(f"      Faltan {weekly_demand - weekly_capacity:,} productos de capacidad")
else:
    print(f"   🟢 CAPACIDAD SUFICIENTE:")
    print(f"      La capacidad semanal ({weekly_capacity:,}) SUPERA la demanda ({weekly_demand:,})")
    print(f"      El problema NO es falta de capacidad global")

# Diagnosticar ventana temporal
current_config = analyze_window_capacity(120)
avg_cap_120 = sum(w['capacity'] for w in current_config) / len(current_config)
avg_order_size = weekly_demand / len(sim_orders)
orders_per_window = avg_cap_120 / avg_order_size

print(f"\n   🕐 VENTANA TEMPORAL (Sc=120 min):")
print(f"      Capacidad promedio: {avg_cap_120:,.0f} productos")
print(f"      Tamaño promedio pedido: {avg_order_size:.0f} productos")
print(f"      Pedidos que caben: {orders_per_window:.1f} pedidos/ventana")
if orders_per_window < 5:
    print(f"      🔴 VENTANA MUY PEQUEÑA: Solo caben ~{orders_per_window:.0f} pedidos por ventana")
    print(f"         Con {len(sim_orders)} pedidos en simulación, necesitarías {len(sim_orders)/orders_per_window:.0f} ventanas")
else:
    print(f"      🟢 VENTANA ADECUADA")

print(f"\n💡 RECOMENDACIONES:")
print(f"   1. AUMENTAR Sc a 240-360 minutos (más vuelos por iteración)")
print(f"   2. REDUCIR K a 12-8 (avanzar más lento, más oportunidades)")
print(f"   3. AUMENTAR deadlines a 72-96 horas (más flexibilidad)")
print(f"   4. VERIFICAR restricciones de capacidad de aeropuertos")
print(f"   5. CONSIDERAR relajar restricciones de 1 escala")

print("\n" + "=" * 80)
print("FIN DEL ANÁLISIS")
print("=" * 80)


