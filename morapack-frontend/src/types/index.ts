/**
 * Central type exports for the application.
 *
 * Structure:
 * - Shared types: OrderStatus, Airport, etc.
 * - CRUD types: Order, Flight, etc.
 * - Simulation types: OrderSummary, Itinerary, etc.
 */

// ═══════════════════════════════════════════════════════════════
// SHARED TYPES
// ═══════════════════════════════════════════════════════════════

export * from './shared';

// ═══════════════════════════════════════════════════════════════
// CRUD TYPES (Management Pages)
// ═══════════════════════════════════════════════════════════════

// Orders
export * from './order';

// Flights
export * from './flight';

// Airports
export * from './aeropuerto';
export * from './airport';

// Geography
export * from './continente';
export * from './pais';

// ═══════════════════════════════════════════════════════════════
// SIMULATION TYPES
// ═══════════════════════════════════════════════════════════════

// Order tracking
export * from './simulation/order-summary.types';

// Preview
export * from './simulation/preview.types';

// Itineraries and flights
export * from './simulation/itinerary.types';
export * from './simulation/flight.types';

// Dynamic events
export * from './simulation/events.types';
