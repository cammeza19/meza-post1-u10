package com.universidad.enviosservice.estrategia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnvioExpressStrategyTest {

    private final EnvioExpressStrategy estrategia = new EnvioExpressStrategy();

    @ParameterizedTest
    @CsvSource({
        "3.0, 100.0, 156.0",     // (100 * 1.2) * 1.3
        "5.0, 100.0, 156.0",     // limite exacto 5 kg: primer tramo
        "10.0, 100.0, 165.75",   // (120 + 5 * 1.5) * 1.3
        "20.0, 100.0, 185.25",   // limite exacto 20 kg: (120 + 15 * 1.5) * 1.3
        "25.0, 100.0, 198.25"    // (120 + 22.5 + 5 * 2.0) * 1.3
    })
    void calculaCostoPorTramosDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }

    @Test
    void aplicaRecargoDistanciaMayorA500() {
        // (600 * 1.2 + 25) * 1.3 = 745 * 1.3 = 968.5
        assertEquals(968.5, estrategia.calcular(3.0, 600.0), 0.01);
    }

    @Test
    void noAplicaRecargoEnElLimiteExactoDe500Km() {
        // (500 * 1.2) * 1.3 = 780.0, sin los 25 de recargo
        assertEquals(780.0, estrategia.calcular(3.0, 500.0), 0.01);
    }
}
