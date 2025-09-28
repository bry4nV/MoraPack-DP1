
package MoraTravel;


public class ACOparams {
    // Parámetros ACO clásicos
    int mAnts = 15;              // Número de hormigas
    int iterMax = 100;           // Máximo de iteraciones
    double alpha = 1.0;          // Peso de la feromona
    double beta = 4.0;           // Peso de la heurística
    double rho = 0.15;           // Tasa de evaporación global
    double xi = 0.1;             // Tasa de evaporación local
    double q0 = 0.85;            // Probabilidad de explotación
    double tau0 = 0.1;           // Valor inicial de feromona
    
    // Parámetros de construcción
    int maxCandidatos = 10;      // Máximo slots candidatos por paso
    int maxSaltos = 2;           // Máximo de saltos (1=directo, 2=1 escala)
    
    // Pesos para evaluación
    double wTardios = 1000.0;    // Penalización por paquetes tardíos
    double wTardanza = 10.0;     // Peso para tardanza promedio
    double wLegs = 1.0;          // Peso para número de legs
}
