package com.universidad.enviosservice.estrategia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnvioOvernightStrategyTest {

    private final EnvioOvernightStrategy estrategia = new EnvioOvernightStrategy();

    @ParameterizedTest
    @CsvSource({
        "3.0, 100.0, 220.0",   // 100 * 2.0 + 20
        "8.0, 100.0, 227.5",   // 220 + 3 * 2.5
        "25.0, 100.0, 272.5"   // 220 + 37.5 + 5 * 3.0
    })
    void calculaCostoPorTramoDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }

    @Test
    void rechazaDistanciasMayoresA300Km() {
        assertThrows(IllegalArgumentException.class,
            () -> estrategia.calcular(3.0, 301.0));
    }
}
