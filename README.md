# Envios Service — Diagnóstico y Mejora Guiada por Métricas

## Decisiones de diseño

1. **Entorno de análisis: Docker local**
   Se eligió ejecutar **SonarQube Community Edition localmente con Docker** (`sonarqube:community` en `http://localhost:9000`) en lugar de SonarCloud con base en dos criterios de la guía:
   - **Independencia y control total de la configuración:** No depende de conectividad externa constante ni de las restricciones de un plan gratuito en la nube. Permite administrar directamente el servidor local, duplicar Quality Profiles ("Estándar Laboratorio"), activar y parametrizar la regla `S1541` (`Threshold = 10`) y definir condiciones personalizadas en el Quality Gate sin límites de permisos.
   - **Privacidad y costo asumido:** Los artefactos y reportes se procesan íntegramente en la máquina local sin exponer públicamente el historial de análisis en un servicio externo. Como contrapartida técnica documentada en la guía, se asumió el consumo de memoria RAM y CPU del contenedor durante toda la sesión de laboratorio.

2. **Exclusiones del análisis**
   Se decidió **no excluir ninguna clase** (`sonar.exclusions` no declarado en `sonar-project.properties`). El proyecto `envios-service` es un módulo Maven plano enfocado exclusivamente en la lógica de negocio (`ServicioCalculoEnvio` y las implementaciones de `EstrategiaEnvio`), sin clases de arranque (`Main` o `@SpringBootApplication`), clases de configuración (`@Configuration`) ni DTOs anémicos cuya ausencia de pruebas pudiera distorsionar artificialmente el porcentaje global de cobertura.

3. **Métrica usada como señal principal: Complejidad Ciclomática (Cyclomatic Complexity)**
   Entre las tres señales expuestas por SonarQube (`Cyclomatic Complexity = 20`, `Cognitive Complexity = 32` y `Condition Coverage = 18.4%`), se seleccionó la **Complejidad Ciclomática (CC = 20)** como señal principal para elegir e intervenir el método `ServicioCalculoEnvio.calcularCosto()` en la Parte 2:
   - **Coincidencia de señales en `calcularCosto()`:** Tanto la complejidad alta como la cobertura baja apuntan al mismo método porque `calcularCosto()` concentraba toda la lógica del negocio en una única estructura condicional anidada.
   - **Causa raíz vs. síntoma:** Según la Sección 3.3 de la guía, un método con $CC = 20$ tiene 20 caminos linealmente independientes y exige un mínimo teórico de 20 casos de prueba para cubrir sus ramas. La baja cobertura inicial (18.4%) era consecuencia directa de esa complejidad estructural. Usar únicamente la cobertura como guía habría llevado a agregar pruebas sobre un método monolítico y frágil (rango "Alta" en la Tabla 3.2), mientras que atacar la Complejidad Ciclomática obligó a descomponer el diseño antes de ampliar la suite de pruebas.

4. **Patrón elegido: Strategy**
   Se analizó la naturaleza real de los condicionales de `calcularCosto()` contrastando los tres patrones candidatos de la guía:
   - **Descarte de State:** El objeto `ServicioCalculoEnvio` es completamente *stateless* (sin estado interno mutable); no recuerda ningún estado entre invocaciones ni existen transiciones válidas de un estado a otro que condicionen su comportamiento futuro.
   - **Descarte de Chain of Responsibility:** No existe una cadena de manejadores donde el orden de evaluación importe o donde múltiples eslabones puedan procesar parcialmente la solicitud antes de delegarla; el tipo de envío selecciona una única fórmula de forma directa y mutuamente excluyente.
   - **Elección de Strategy:** Cada rama de `metodoEnvio` (`ESTANDAR`, `EXPRESS`, `OVERNIGHT`, `INTERNACIONAL`) calcula el mismo tipo de resultado (el costo base del envío en función de `pesoKg` y `distanciaKm`) aplicando una fórmula propia e intercambiable a partir de un valor discreto de entrada, sin relación de orden ni de estado con las demás ramas.
   - **Qué se movió a las estrategias y qué permaneció en el orquestador:** Se extrajo el cálculo por tramos de peso, los recargos por distancia y la validación de límite de distancia de `OVERNIGHT` (> 300 km) hacia cuatro clases que implementan `EstrategiaEnvio` (`EnvioEstandarStrategy`, `EnvioExpressStrategy`, `EnvioOvernightStrategy`, `EnvioInternacionalStrategy`). En cambio, la validación de método no soportado, el recargo por paquete frágil (+15%) y los descuentos por `tipoCliente` (`PREMIUM` 10% y `CORPORATIVO` 15%) se mantuvieron deliberadamente en el orquestador `ServicioCalculoEnvio`. Estas tres últimas son reglas transversales que aplican por igual a todos los métodos de envío; moverlas dentro de cada estrategia habría cuatriplicado esa lógica. Con esta separación, `calcularCosto()` redujo su complejidad ciclomática de **20 a 5** (1 base + validación de estrategia nula + `esFragil` + 2 ramas de `tipoCliente`), ubicándose en el rango "Moderada" de la Tabla 3.2, mientras que cada estrategia quedó con una complejidad baja entre 3 y 4.

5. **Criterio de pruebas agregadas**
   En el análisis inicial de la Parte 1, el reporte de JaCoCo mostró que 31 de las 38 condiciones (81.6%) estaban sin cubrir porque la suite inicial solo ejercitaba `ESTANDAR` con peso bajo ($\le 5$ kg), distancia corta ($100$ km), sin fragilidad y para clientes `REGULAR` y `PREMIUM`. Para subir la cobertura de forma significativa sin recurrir a pruebas triviales, cada prueba nueva se diseñó para ejercitar una rama o valor límite previamente en rojo:
   - **`EnvioEstandarStrategyTest`:** Cubre con `@ParameterizedTest` (`@CsvSource`) los 3 tramos de peso ($\le 5$ kg, $5 < \text{peso} \le 20$ kg y $> 20$ kg) y el recargo por distancia $> 500$ km.
   - **`EnvioExpressStrategyTest`:** Cubre los 3 tramos de peso con el factor multiplicador de $1.3$ y la rama de recargo por distancia $> 500$ km.
   - **`EnvioOvernightStrategyTest`:** Cubre los 3 tramos de peso para distancias válidas y verifica con `assertThrows(IllegalArgumentException.class, ...)` la rama de rechazo cuando `distanciaKm > 300` ($301.0$ km).
   - **`EnvioInternacionalStrategyTest`:** Cubre los 3 tramos de peso y la rama de recargo para distancias $> 5000$ km ($6000.0$ km).
   - **`ServicioCalculoEnvioTest` (ampliado):** Mantiene los 2 casos base y agrega pruebas específicas para las ramas transversales del orquestador antes no cubiertas: recargo por `esFragil = true` (+15%), descuento para cliente `CORPORATIVO` (factor $0.85$) y lanzamiento de `IllegalArgumentException` ante un método de envío inexistente (`"DRON"`).

6. **Realismo del Quality Gate**
   Se configuró el Quality Profile **"Estándar Laboratorio"** activando la regla `S1541` con umbral **CC = 10** y el Quality Gate **"Estándar Laboratorio"** con tres condiciones (**Coverage < 70%**, **New Code Smells / Maintainability Issues > 0** y **Duplicated Lines > 5%**):
   - **Por qué CC = 10 por método (y no 5 o 20):** De acuerdo con la Tabla 3.2 de la guía (*Umbrales de Complejidad Ciclomática*), el rango **5–10** corresponde a una complejidad **"Moderada"** (*"Aceptable, requiere atención al agregar lógica"*), siendo 10 el límite superior antes de entrar al rango **"Alta" (11–20)** (*"Difícil de probar; mayor probabilidad de defectos; refactorizar"*). Fijar el umbral en 5 sería excesivamente restrictivo y forzaría a fragmentar métodos cohesivos como el propio orquestador (`CC = 5`), mientras que fijarlo en 20 permitiría el paso de métodos monolíticos difíciles de mantener como la versión original de `calcularCosto()` (`CC = 20`).
   - **Por qué 70% de cobertura (y no 100%):** Como explica la Sección 2.3 de la guía (*Limitaciones de la Cobertura*), exigir un 100% de cobertura como meta absoluta genera incentivos perversos (Ley de Goodhart): induce a escribir pruebas superficiales o sin aserciones reales solo para satisfacer la métrica y bloquea el pipeline ante código de infraestructura o defensivo. Un umbral de **70%** (alineado con el umbral de referencia de JaCoCo en la Sección 2.2) exige que la lógica de negocio principal esté sólidamente probada sin obligar a construir pruebas artificiales.

## Comparación antes / después

| Métrica | Antes (Parte 1) | Después (Parte 2) |
|---------|------------------|--------------------|
| Complejidad ciclomática de `calcularCosto()` | 20 | 5 (reducción del 75%) |
| Complejidad cognitiva de `calcularCosto()` | 32 | 4 |
| Cobertura de líneas de la lógica de envío | 23.8% (10/42 líneas) | 100.0% (50/50 líneas) |
| Cobertura de ramas de la lógica de envío | 18.4% (7/38 ramas) | 100.0% (32/32 ramas) |
| Cobertura global en SonarQube | 21.3% | 100.0% |
| Code Smells / Maintainability Issues | 0 | 0 (ningún método supera CC = 10) |
| Duplicated Lines (%) | 0.0% | 7.92% |
| Quality Gate ("Estándar Laboratorio") | — | **Failed** (por condición de duplicación: 7.9% > 5.0%) |

**Interpretación del resultado del Quality Gate tras la refactorización:**
Al evaluar el segundo análisis contra el Quality Gate **"Estándar Laboratorio"**, el proyecto aprueba con holgura la condición de cobertura (**100.0% $\ge$ 70.0%**) y la condición de mantenibilidad/complejidad por método (**0 nuevos Code Smells / Maintainability Issues**, dado que `calcularCosto()` bajó a `CC = 5` y cada estrategia tiene `CC` entre 3 y 4, todas por debajo del umbral `S1541 = 10`). Sin embargo, el Quality Gate reporta estado **Failed** exclusivamente por la tercera condición: **`7.9% Duplicated Lines (%) is greater than 5.0%`**. Esto ocurre porque el detector de tokens de SonarQube identifica como bloque estructuralmente similar la escalera de condicionales de tramos de peso (`if (pesoKg <= 5) ... else if (pesoKg <= 20) ... else ...`) presente en las cuatro clases de estrategia, a pesar de que cada una aplica coeficientes y tarifas base distintas.

## Capturas
## Capturas

### Análisis Inicial (Parte 1)
![](docs/sonar-dashboard-inicial.jpeg)

![](docs/jacoco-inicial.jpeg)

### Análisis Final (Parte 2)
![](docs/sonar-dashboard-final.jpeg)

![](docs/jacoco-final.jpeg)





## Herramientas utilizadas
- Java 21, Apache Maven 3.9+
- SonarQube Community Edition (`sonarqube:community` en Docker), JaCoCo (`0.8.11`), JUnit Jupiter (`5.10.2`)
- Git, GitHub

## Conclusiones
El desarrollo de este laboratorio permitió comprobar en la práctica cómo las métricas de producto (Complejidad Ciclomática y Cobertura de Ramas) actúan como herramientas complementarias de diagnóstico: mientras que SonarQube identificó una complejidad ciclomática crítica de 20 en `calcularCosto()`, JaCoCo evidenció que dicha estructura monolítica dejaba el 81.6% de las ramas sin probar. La aplicación justificada del patrón de diseño Strategy permitió desacoplar los algoritmos de cálculo por método de envío de las reglas transversales del negocio, logrando una reducción real del 75% en la complejidad ciclomática del método orquestador (de 20 a 5) y facilitando la construcción de pruebas unitarias parametrizadas que elevaron la cobertura de ramas del 18.4% al 100.0%. Finalmente, la configuración del Quality Profile (regla S1541 con umbral 10) y del Quality Gate "Estándar Laboratorio" demostró cómo automatizar el control de calidad en el ciclo de construcción, evidenciando además el compromiso (*trade-off*) real entre separar clases por estrategia y el incremento en la métrica de duplicación estructural reportada por el analizador estático.
