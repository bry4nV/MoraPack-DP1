// src/data/aeropuertos.ts
import type { Aeropuerto } from '@/types';

export const AEROPUERTOS: Aeropuerto[] = [
  {
    id: 1,
    nombre: 'Jorge Chávez International Airport',
    codigo: 'LIM',
    ciudad: 'Lima',
    pais: { id: 51, nombre: 'Perú', continente: 'AMERICA' },
    capacidadAlmacen: 900,
    gmt: -5,
    latitud: -12.0219,
    longitud: -77.1143
  },
  {
    id: 2,
    nombre: 'Brussels Airport',
    codigo: 'BRU',
    ciudad: 'Brussels',
    pais: { id: 56, nombre: 'Belgium', continente: 'EUROPE' },
    capacidadAlmacen: 850,
    gmt: 1,
    latitud: 50.9010,
    longitud: 4.4844
  },
  {
    id: 3,
    nombre: 'Heydar Aliyev International Airport',
    codigo: 'GYD',
    ciudad: 'Baku',
    pais: { id: 31, nombre: 'Azerbaijan', continente: 'ASIA' },
    capacidadAlmacen: 800,
    gmt: 4,
    latitud: 40.4675,
    longitud: 50.0467
  },
  {
    id: 4,
    nombre: 'John F. Kennedy International Airport',
    codigo: 'JFK',
    ciudad: 'New York',
    pais: { id: 840, nombre: 'United States', continente: 'AMERICA' },
    capacidadAlmacen: 1000,
    gmt: -5,
    latitud: 40.6413,
    longitud: -73.7781
  },
  {
    id: 5,
    nombre: 'São Paulo/Guarulhos – Gov. André Franco Montoro Intl',
    codigo: 'GRU',
    ciudad: 'São Paulo',
    pais: { id: 76, nombre: 'Brazil', continente: 'AMERICA' },
    capacidadAlmacen: 950,
    gmt: -3,
    latitud: -23.4356,
    longitud: -46.4731
  },
  {
    id: 6,
    nombre: 'Los Angeles International Airport',
    codigo: 'LAX',
    ciudad: 'Los Angeles',
    pais: { id: 840, nombre: 'United States', continente: 'AMERICA' },
    capacidadAlmacen: 900,
    gmt: -8,
    latitud: 33.9416,
    longitud: -118.4085
  },
  {
    id: 7,
    nombre: 'Haneda Airport',
    codigo: 'HND',
    ciudad: 'Tokyo',
    pais: { id: 392, nombre: 'Japan', continente: 'ASIA' },
    capacidadAlmacen: 950,
    gmt: 9,
    latitud: 35.5494,
    longitud: 139.7798
  },
  {
    id: 8,
    nombre: 'Incheon International Airport',
    codigo: 'ICN',
    ciudad: 'Seoul',
    pais: { id: 410, nombre: 'South Korea', continente: 'ASIA' },
    capacidadAlmacen: 900,
    gmt: 9,
    latitud: 37.4602,
    longitud: 126.4407
  },
  {
    id: 9,
    nombre: 'Adolfo Suárez Madrid–Barajas Airport',
    codigo: 'MAD',
    ciudad: 'Madrid',
    pais: { id: 724, nombre: 'Spain', continente: 'EUROPE' },
    capacidadAlmacen: 880,
    gmt: 1,
    latitud: 40.4983,
    longitud: -3.5676
  },
  {
    id: 10,
    nombre: 'Sydney Kingsford Smith Airport',
    codigo: 'SYD',
    ciudad: 'Sydney',
    pais: { id: 36, nombre: 'Australia', continente: 'OCEANIA' },
    capacidadAlmacen: 820,
    gmt: 10,
    latitud: -33.9399,
    longitud: 151.1753
  }
];
