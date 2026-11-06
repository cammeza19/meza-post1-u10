package com.universidad.enviosservice.estrategia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnvioEstandarStrategyTest {

    private final EnvioEstandarStrategy estrategia = new EnvioEstandarStrategy();

    @ParameterizedTest
    @CsvSource({
        "3.0, 100.0, 50.0",    // peso <= 5 kg: 100 * 0.5
        "5.0, 100.0, 50.0",    // limite exacto 5 kg: aun pertenece al primer tramo
        "10.0, 100.0, 54.0",   // 5 < peso <= 20 kg: 50 + 5 * 0.8
        "20.0, 100.0, 62.0",   // limite exacto 20 kg: 50 + 15 * 0.8
        "25.0, 100.0, 68.0"    // peso > 20 kg: 50 + 12 + 5 * 1.2
    })
    void calculaCostoPorTramosDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }

    @Test
    void aplicaRecargoDistanciaMayorA500() {
        // peso 3kg, distancia 600km: 600 * 0.5 + 10 = 310.0
        assertEquals(310.0, estrategia.calcular(3.0, 600.0), 0.01);
    }

    @Test
    void noAplicaRecargoEnElLimiteExactoDe500Km() {
        // 500 km no supera el umbral (> 500): 500 * 0.5 = 250.0
        assertEquals(250.0, estrategia.calcular(3.0, 500.0), 0.01);
    }
}
