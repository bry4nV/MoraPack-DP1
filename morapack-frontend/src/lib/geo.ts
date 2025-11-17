const R = 6371_000;
const toRad = (d:number)=>d*Math.PI/180, toDeg=(r:number)=>r*180/Math.PI;

/**
 * Convierte coordenadas DMS (Degrees Minutes Seconds) a formato decimal
 * Ejemplos:
 *   "04 42 05 N" -> 4.701389
 *   "74 08 49 W" -> -74.146944
 *   "12 01 19 S" -> -12.021944
 */
export function dmsToDecimal(dms: string | number): number {
  // Si ya es número, retornarlo directamente
  if (typeof dms === 'number') {
    return dms;
  }

  // Si es string pero parece decimal (ej: "4.701389"), parsearlo
  if (!dms.match(/[NSEW]/i)) {
    const num = parseFloat(dms);
    return isNaN(num) ? 0 : num;
  }

  // Formato DMS: "04 42 05 N" o "74 08 49 W"
  const parts = dms.trim().split(/\s+/);
  if (parts.length < 4) {
    console.warn(`Formato DMS inválido: "${dms}"`);
    return 0;
  }

  const degrees = parseInt(parts[0], 10);
  const minutes = parseInt(parts[1], 10);
  const seconds = parseInt(parts[2], 10);
  const direction = parts[3].toUpperCase();

  // Convertir a decimal
  let decimal = degrees + minutes / 60 + seconds / 3600;

  // Sur y Oeste son negativos
  if (direction === 'S' || direction === 'W') {
    decimal = -decimal;
  }

  return decimal;
}

export function haversineDistanceMeters([lon1,lat1]:[number,number],[lon2,lat2]:[number,number]){
  const φ1=toRad(lat1), φ2=toRad(lat2), Δφ=toRad(lat2-lat1), Δλ=toRad(lon2-lon1);
  const a=Math.sin(Δφ/2)**2+Math.cos(φ1)*Math.cos(φ2)*Math.sin(Δλ/2)**2;
  return 2*R*Math.asin(Math.sqrt(a));
}
function centralAngle(φ1:number,λ1:number,φ2:number,λ2:number){
  const x=Math.sin(φ1)*Math.sin(φ2)+Math.cos(φ1)*Math.cos(φ2)*Math.cos(λ2-λ1);
  return Math.acos(Math.min(1,Math.max(-1,x)));
}
export function interpolateGreatCircle([lon1,lat1]:[number,number],[lon2,lat2]:[number,number],t:number):[number,number]{
  const φ1=toRad(lat1), λ1=toRad(lon1), φ2=toRad(lat2), λ2=toRad(lon2);
  const θ=centralAngle(φ1,λ1,φ2,λ2); if(θ===0) return [lon1,lat1];
  const sinθ=Math.sin(θ), A=Math.sin((1-t)*θ)/sinθ, B=Math.sin(t*θ)/sinθ;
  const x=A*Math.cos(φ1)*Math.cos(λ1)+B*Math.cos(φ2)*Math.cos(λ2);
  const y=A*Math.cos(φ1)*Math.sin(λ1)+B*Math.cos(φ2)*Math.sin(λ2);
  const z=A*Math.sin(φ1)+B*Math.sin(φ2);
  const φ=Math.atan2(z,Math.hypot(x,y)), λ=Math.atan2(y,x);
  return [toDeg(λ),toDeg(φ)];
}
