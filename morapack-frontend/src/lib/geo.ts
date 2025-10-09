const R = 6371_000;
const toRad = (d:number)=>d*Math.PI/180, toDeg=(r:number)=>r*180/Math.PI;

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
