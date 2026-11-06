package com.universidad.enviosservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ServicioCalculoEnvioTest {

    private final ServicioCalculoEnvio servicio = new ServicioCalculoEnvio();

    @Test
    void calculaEnvioEstandarPesoBajo() {
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "REGULAR", false);
        assertEquals(50.0, costo, 0.01);
    }

    @Test
    void aplicaDescuentoClientePremium() {
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "PREMIUM", false);
        assertEquals(45.0, costo, 0.01);
    }

    @Test
    void aplicaDescuentoClienteCorporativo() {
        // 50.0 * 0.85 = 42.5
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "CORPORATIVO", false);
        assertEquals(42.5, costo, 0.01);
    }

    @Test
    void aplicaRecargoPaqueteFragil() {
        // 50.0 + (50.0 * 0.15) = 57.5
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "REGULAR", true);
        assertEquals(57.5, costo, 0.01);
    }

    @Test
    void rechazaMetodoEnvioNoSoportado() {
        assertThrows(IllegalArgumentException.class,
            () -> servicio.calcularCosto("DRON", 3.0, 100.0, "REGULAR", false));
    }
}