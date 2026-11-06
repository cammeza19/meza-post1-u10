package com.universidad.enviosservice.estrategia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnvioInternacionalStrategyTest {

    private final EnvioInternacionalStrategy estrategia = new EnvioInternacionalStrategy();

    @ParameterizedTest
    @CsvSource({
        "3.0, 1000.0, 1850.0",    // 1000 * 1.8 + 50
        "5.0, 1000.0, 1850.0",    // limite exacto 5 kg: primer tramo
        "10.0, 1000.0, 1865.0",   // 1850 + 5 * 3.0
        "20.0, 1000.0, 1895.0",   // limite exacto 20 kg: 1850 + 15 * 3.0
        "25.0, 1000.0, 1917.5"    // 1850 + 45 + 5 * 4.5
    })
    void calculaCostoPorTramosDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }

    @Test
    void aplicaRecargoDistanciaMayorA5000() {
        // 6000 * 1.8 + 50 + 100 = 10950.0
        assertEquals(10950.0, estrategia.calcular(3.0, 6000.0), 0.01);
    }

    @Test
    void noAplicaRecargoEnElLimiteExactoDe5000Km() {
        // 5000 * 1.8 + 50 = 9050.0, sin los 100 de recargo
        assertEquals(9050.0, estrategia.calcular(3.0, 5000.0), 0.01);
    }
}
