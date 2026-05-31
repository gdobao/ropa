Plan: Mejoras del Motor de Colorimetría — Ejecutable para OpenCode (DeepSeek v4 Flash)
=====================================================================================

Estado: Planificado
Fecha: 2026-06-01
Modelo objetivo: DeepSeek v4 Flash vía OpenCode
Alcance: 10 issues, 10 archivos producción, 5 archivos test

================================================================================
ESTRATEGIA DE PARALELIZACIÓN
================================================================================

El plan se divide en 3 fases. Cada fase tiene tareas que se pueden ejecutar
en paralelo (subagentes independientes).

Fase 1 — Cimientos (sin dependencias entre tareas)
  Tareas 1A, 1B, 1C, 1D se pueden hacer en paralelo.

Fase 2 — Motor de scoring (depende de Fase 1)
  Tareas 2A, 2B se pueden hacer en paralelo.

Fase 3 — Integración y tests (depende de Fase 2)
  Tareas 3A, 3B se pueden hacer en paralelo.

================================================================================
FASE 1 — CIMIENTOS (tareas independientes, paralelizables)
================================================================================

TAREA 1A: Enum ColorTemperature — añadir SEMI_WARM y SEMI_COOL
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/colorimetry/model/ColorTemperature.java

Código actual:
  public enum ColorTemperature {
      WARM("Cálido"),
      COOL("Frío"),
      NEUTRAL("Neutro");
      ...
  }

Código nuevo:
  public enum ColorTemperature {
      WARM("Cálido"),
      SEMI_WARM("Semi-cálido"),
      COOL("Frío"),
      SEMI_COOL("Semi-frío"),
      NEUTRAL("Neutro");
      // ... constructor y displayName() sin cambios
  }

Impacto: 0 archivos de test afectados (enum nuevo, no rompe nada).

---

TAREA 1B: Enum ColorIntensity — añadir MEDIUM
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/colorimetry/model/ColorIntensity.java

Código actual:
  public enum ColorIntensity {
      BRIGHT("Vívido"),
      SOFT("Suave");
      ...
  }

Código nuevo:
  public enum ColorIntensity {
      BRIGHT("Vívido"),
      MEDIUM("Medio"),
      SOFT("Suave");
      // ... constructor y displayName() sin cambios
  }

Impacto: 0 archivos de test afectados (enum nuevo, no rompe nada).

---

TAREA 1C: ColorPaletteStore — inyectar ColorimetryProperties
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/colorimetry/data/ColorPaletteStore.java

Cambios:
  1. Añadir campo y constructor con ColorimetryProperties:
     private final ColorimetryProperties props;
     public ColorPaletteStore(ColorimetryProperties props) {
         this.props = props;
     }

  2. Línea 190: isNeutral() usar deltaE2000 + props.neutralDeltaThreshold():
     // Antes:
     if (ColorSpaceConverter.deltaE76(lab, refLab) < 15.0)
     // Después:
     if (ColorSpaceConverter.deltaE2000(lab, refLab) < props.neutralDeltaThreshold())

  3. Línea 214: classifyByNearestNeighbor() usar deltaE2000:
     // Antes:
     double delta = ColorSpaceConverter.deltaE76(lab, refLab);
     // Después:
     double delta = ColorSpaceConverter.deltaE2000(lab, refLab);

  4. Línea 225: mismo cambio en el bucle de NEUTRALS.

Impacto en tests:
  - ColorPaletteStoreTest.java: línea 22, setUp() → new ColorPaletteStore(ColorimetryProperties.defaults())
  - ColorCompatibilityEngineTest.java: línea 19 → new ColorPaletteStore(ColorimetryProperties.defaults())
  - ColorSeasonClassifierTest.java: línea 19 → new ColorPaletteStore(ColorimetryProperties.defaults())

---

TAREA 1D: ColorPaletteStore — eliminar duplicados
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/colorimetry/data/ColorPaletteStore.java

Eliminar estas entradas duplicadas (mantener en la primera estación donde aparecen):
  - SPRING línea 22: "Crema" #FFFDD0 → duplicado en AUTUMN línea 95 → ELIMINAR de SPRING
  - SPRING línea 34: "Arena" #C3B091 → duplicado en AUTUMN línea 87 → ELIMINAR de SPRING
  - SPRING línea 40: "Lila claro" #C8A2C8 → duplicado en SUMMER línea 55 → ELIMINAR de SPRING
  - SPRING línea 43: "Terracota suave" #E2725B → duplicado en AUTUMN línea 82 → ELIMINAR de SPRING
  - AUTUMN línea 96: "Dorado" #FFD700 → duplicado en WINTER línea 132 → ELIMINAR de AUTUMN
  - SUMMER línea 54: "Gris perla" #C0C0C0 → duplicado en WINTER línea 118 → ELIMINAR de SUMMER

Resultado: cada estación pasa de 25 a 24 colores.

Impacto en tests:
  - ColorPaletteStoreTest.java:
    - Línea 38: assertEquals(25, ...) → assertEquals(24, ...) para SPRING
    - Línea 46: assertEquals(25, ...) → assertEquals(24, ...) para SUMMER
    - Línea 54: assertEquals(25, ...) → assertEquals(24, ...) para AUTUMN
    - Línea 62: assertEquals(25, ...) → assertEquals(24, ...) para WINTER
    - Línea 95-96: totalCountIsRoughly100 → assertTrue(total >= 90 && total <= 105)

---

FASE 2 — MOTOR DE SCORING (depende de Fase 1 completada)
================================================================================

TAREA 2A: ColorProfile — actualizar fromLab() para SEMI_* y MEDIUM
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/colorimetry/model/ColorProfile.java

Cambios en el método fromLab():

  1. Temperatura (líneas 30-37):
     // Antes:
     if (b > warmCoolThreshold) { temperature = WARM; }
     else if (b < -warmCoolThreshold) { temperature = COOL; }
     else { temperature = NEUTRAL; }

     // Después:
     if (b > warmCoolThreshold) { temperature = WARM; }
     else if (b > warmCoolThreshold * 0.6) { temperature = SEMI_WARM; }
     else if (b < -warmCoolThreshold) { temperature = COOL; }
     else if (b < -warmCoolThreshold * 0.6) { temperature = SEMI_COOL; }
     else { temperature = NEUTRAL; }

  2. Intensidad (líneas 40-44):
     // Antes:
     ColorIntensity intensity = normalisedChroma > intensityThreshold
         ? ColorIntensity.BRIGHT : ColorIntensity.SOFT;

     // Después:
     ColorIntensity intensity;
     if (normalisedChroma > intensityThreshold) {
         intensity = ColorIntensity.BRIGHT;
     } else if (normalisedChroma > intensityThreshold * 0.4) {
         intensity = ColorIntensity.MEDIUM;
     } else {
         intensity = ColorIntensity.SOFT;
     }

Impacto en tests:
  - ColorSeasonClassifierTest.java:
    - coralIsSpring: #FF7F50 tiene chroma alto → BRIGHT. No cambia.
    - lavandaIsSummer: #B2A4D4 tiene chroma bajo → SOFT. No cambia.
    - blackClassifies: b*=0 → NEUTRAL. No cambia.
    - whiteClassifies: b*=0 → NEUTRAL. No cambia.

---

TAREA 2B: ColorCompatibilityEngine — reescribir scoring
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/colorimetry/service/ColorCompatibilityEngine.java

Cambios:

  1. Método score() — aplicar pesos configurables (línea 106):
     // Antes:
     double raw = deltaEScore + seasonScore + ruleScore;

     // Después:
     double raw = deltaEScore * props.deltaEWeight()
                + seasonScore * props.seasonWeight()
                + ruleScore * props.ruleWeight();

  2. computeSeasonScore() — tratar SEMI_WARM/SEMI_COOL como puente (líneas 142-149):
     // Antes:
     if (p1.temperature() == NEUTRAL || p2.temperature() == NEUTRAL) { return 0; }

     // Después:
     if (p1.temperature() == NEUTRAL || p1.temperature() == SEMI_WARM
         || p1.temperature() == SEMI_COOL || p2.temperature() == NEUTRAL
         || p2.temperature() == SEMI_WARM || p2.temperature() == SEMI_COOL) {
         return 0;
     }

  3. computeRuleScore() — reducir blacklist de -30 a -10 (líneas 180-185):
     // Antes:
     if (isBadRojoRosa(hex1, hex2)) { score -= 30; }
     if (isBadNegroAzulMarino(hex1, hex2)) { score -= 30; }

     // Después:
     if (isRojoRosaPair(hex1, hex2)) { score -= 10; }
     if (isNegroAzulMarinoPair(hex1, hex2)) { score -= 10; }

  4. Renombrar métodos helper:
     - isBadRojoRosa → isRojoRosaPair (línea 205)
     - isBadNegroAzulMarino → isNegroAzulMarinoPair (línea 210)
     - Actualizar buildWarnings() (líneas 292-298) para usar los nuevos nombres

  5. isSameTemperatureFamily() — incluir SEMI_* (línea 232):
     // Antes:
     if (t1 == NEUTRAL || t2 == NEUTRAL) return true;
     return t1 == t2;

     // Después:
     if (t1 == NEUTRAL || t2 == NEUTRAL) return true;
     if (t1 == SEMI_WARM || t1 == SEMI_COOL || t2 == SEMI_WARM || t2 == SEMI_COOL) return true;
     return t1 == t2;

  6. isBasePlusLayer() — ampliar categorías (línea 240):
     // Antes:
     private static final Set<String> BASE_CATEGORIES = Set.of("Top", "Pantalón", "Vestido", "Falda");
     private static final Set<String> LAYER_CATEGORIES = Set.of("Chaqueta", "Abrigo");

     // Después:
     private static final Set<String> TOP_CATEGORIES = Set.of("Top", "Camisa", "Sudadera");
     private static final Set<String> BOTTOM_CATEGORIES = Set.of("Pantalón", "Falda", "Vestido");
     private static final Set<String> LAYER_CATEGORIES = Set.of("Chaqueta", "Abrigo");

     // isBasePlusLayer reescrito:
     private static boolean isBasePlusLayer(String cat1, String cat2) {
         if (cat1 == null || cat2 == null) return false;
         // Top + Bottom
         if ((TOP_CATEGORIES.contains(cat1) && BOTTOM_CATEGORIES.contains(cat2)) ||
             (BOTTOM_CATEGORIES.contains(cat2) && TOP_CATEGORIES.contains(cat1))) {
             return true;
         }
         // Top/Bottom + Layer
         if ((TOP_CATEGORIES.contains(cat1) || BOTTOM_CATEGORIES.contains(cat1))
             && LAYER_CATEGORIES.contains(cat2)) return true;
         if ((TOP_CATEGORIES.contains(cat2) || BOTTOM_CATEGORIES.contains(cat2))
             && LAYER_CATEGORIES.contains(cat1)) return true;
         // Layer + Bottom
         if ((LAYER_CATEGORIES.contains(cat1) && BOTTOM_CATEGORIES.contains(cat2)) ||
             (LAYER_CATEGORIES.contains(cat2) && BOTTOM_CATEGORIES.contains(cat1))) return true;
         return false;
     }

Impacto en tests:
  - ColorCompatibilityEngineTest.java:
    - sameColorScoresHigh(): el score cambia porque ahora se aplican pesos.
      Antes: raw = deltaE~100 + season~30 + rule~30 = ~160 → clamp 100.
      Después: raw = 100*0.4 + 30*0.3 + 30*0.3 = 40+9+9 = 58.
      Cambiar assertTrue(result.score() > 70) → assertTrue(result.score() >= 50)
    - blackAndWhiteScoresHigh(): antes ~100, después ~55. Cambiar a >= 45.
    - redAndPinkHasWarning(): sigue produciendo warning. No cambia.
    - springAndSummerAdjacentScoresWell(): sigue siendo >= 0. No cambia.

---

FASE 3 — INTEGRACIÓN Y TESTS (depende de Fase 2 completada)
================================================================================

TAREA 3A: Añadir @Cacheable a ColorSeasonClassifier.classify()
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/colorimetry/service/ColorSeasonClassifier.java

Cambios:
  1. Añadir import:
     import org.springframework.cache.annotation.Cacheable;

  2. Método classify() — añadir null check y @Cacheable:
     @Cacheable(cacheNames = "color-classifications", key = "#hex", unless = "#result == null")
     public ColorProfile classify(String hex) {
         if (hex == null || hex.isBlank()) {
             throw new IllegalArgumentException("Hex cannot be null or blank");
         }
         // ... resto del método sin cambios
     }

Verificación:
  - ColorinchiApplication.java ya tiene @EnableCaching (línea 8). No cambia.

Impacto en tests:
  - ColorSeasonClassifierTest.java:
    - nullHexThrowsException(): ahora lanza IllegalArgumentException (Runtime EXC sigue ok)
    - emptyHexThrowsException(): igual
    - consistentClassification(): con cache sigue siendo consistente

---

TAREA 3B: AiRecommendationService — actualizar prompt
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/service/AiRecommendationService.java

Línea 95 del prompt:
  // Antes:
  "- Evitá combinar rojo con rosa, o negro con azul marino."

  // Después:
  "- Rojo con rosa y negro con azul marino son combinaciones de riesgo: si las usás, equilibrá con un neutro."

Impacto: 0 tests afectados (el prompt no se testa directamente).

---

TAREA 3C: GarmentCompatibilityService — ampliar CATEGORY_COMPATIBILITY
────────────────────────────────────────────────────────────────
Archivo: src/main/java/com/colorinchi/app/service/GarmentCompatibilityService.java

Líneas 21-32, añadir reglas para Zapatos y Accesorio:

  // Antes:
  Map.entry("Zapatos", Set.of("Top", "Pantalón", "Vestido", "Chaqueta", "Accesorio")),
  Map.entry("Accesorio", Set.of("Top", "Pantalón", "Vestido", "Chaqueta", "Zapatos")),

  // Después:
  Map.entry("Zapatos", Set.of("Top", "Camisa", "Sudadera", "Pantalón", "Falda", "Vestido", "Chaqueta", "Abrigo", "Accesorio")),
  Map.entry("Accesorio", Set.of("Top", "Camisa", "Sudadera", "Pantalón", "Falda", "Vestido", "Chaqueta", "Abrigo", "Zapatos")),

Impacto en tests:
  - GarmentCompatibilityServiceTest.java: no cambia (mockea el engine, no usa CATEGORY_COMPATIBILITY directamente).

---

RESUMEN DE DEPENDENCIAS
================================================================================

Fase 1 (paralelas, sin dependencias):
  1A: ColorTemperature enum  ─┐
  1B: ColorIntensity enum  ───┤
  1C: ColorPaletteStore (props + deltaE2000) ─┤──→ Sin dependencias entre sí
  1D: ColorPaletteStore (duplicados) ──────────┘

Fase 2 (paralelas, dependen de Fase 1):
  2A: ColorProfile.fromLab() (SEMI_*, MEDIUM) ──┐
  2B: ColorCompatibilityEngine (pesos, reglas) ──┘

Fase 3 (paralelas, dependen de Fase 2):
  3A: ColorSeasonClassifier (@Cacheable) ────────┐
  3B: AiRecommendationService (prompt) ──────────┤──→ Sin dependencias entre sí
  3C: GarmentCompatibilityService (categorias) ──┘

================================================================================
VERIFICACIÓN FINAL
================================================================================

Tras completar las 3 fases:

  mvn test

Debe pasar con 0 fallos. Los 476 tests actuales deben seguir pasando.

Verificaciones manuales:
  - Scores de compatibilidad más interpretativos (65 = buena, 40 = mediocre, 20 = conflictiva)
  - Clasificación de estación estable (cache + deltaE2000)
  - Colores limítrofes con más matices (SEMI_WARM/SEMI_COOL, MEDIUM intensity)
  - Palette con colores únicos por estación (96 colores en total, no 100)

================================================================================
ARCHIVOS RESUMEN
================================================================================

Producción (10 archivos):
  1. ColorTemperature.java          — añadir SEMI_WARM, SEMI_COOL
  2. ColorIntensity.java            — añadir MEDIUM
  3. ColorPaletteStore.java         — inyectar props, deltaE2000, eliminar duplicados
  4. ColorProfile.java              — fromLab() con SEMI_*, MEDIUM
  5. ColorCompatibilityEngine.java  — pesos, reglas, categorías
  6. ColorSeasonClassifier.java     — @Cacheable, null check
  7. AiRecommendationService.java   — actualizar prompt
  8. GarmentCompatibilityService.java — ampliar CATEGORY_COMPATIBILITY
  9. ColorinchiApplication.java     — ya tiene @EnableCaching, no cambia

Test (4 archivos):
  1. ColorPaletteStoreTest.java     — constructor, sizes de paletas
  2. ColorCompatibilityEngineTest.java — thresholds de score
  3. ColorSeasonClassifierTest.java — constructor, null check
  4. GarmentCompatibilityServiceTest.java — no cambia

Config (1 archivo):
  1. application.yml                — no cambia (valores por defecto correctos)
