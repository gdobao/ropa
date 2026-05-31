# Plan: Mejoras del Motor de Colorimetría

**Estado**: Planificado (sin código)
**Fecha**: 2026-05-31
**Alcance**: 10 issues, 10 archivos de producción, 5 archivos de test, 1 archivo de config

---

## ISSUE 1: Pesos configurados pero no usados (dead config)

### Problema
`ColorimetryProperties` define `deltaEWeight` (0.40), `seasonWeight` (0.30), `ruleWeight` (0.30) pero `ColorCompatibilityEngine.score()` (línea 106) hace `double raw = deltaEScore + seasonScore + ruleScore;` sin aplicar nunca los pesos. Son dead code.

### Archivos a modificar

**1. `ColorCompatibilityEngine.java`** — método `score()`, línea 106

Código actual:
```java
double raw = deltaEScore + seasonScore + ruleScore;
int finalScore = clamp((int) Math.round(raw), 0, 100);
```

Código nuevo:
```java
// Each component is already on 0-100 scale. Apply configured weights.
double raw = deltaEScore * props.deltaEWeight()
           + seasonScore * props.seasonWeight()
           + ruleScore * props.ruleWeight();
int finalScore = clamp((int) Math.round(raw), 0, 100);
```

**Impacto en tests**: Los tests que verifican scores absolutos (`ColorCompatibilityEngineTest`) necesitarán ajustar los thresholds porque los scores serán diferentes.

### Archivos de test afectados

**`ColorCompatibilityEngineTest.java`**:
- `sameColorScoresHigh()` — línea 38: `assertTrue(result.score() > 70)`. Con pesos, un color idéntico tendría deltaE~100*0.4=40 + season 30*0.3=9 + rules 30*0.3=9 = 58. El threshold de 70 sería imposible de alcanzar. Hay que bajar a ~55.
- `blackAndWhiteScoresHigh()` — línea 46: `assertTrue(result.score() > 70)`. Negro+blanco: deltaE~100*0.4=40 + season 15*0.3=4.5 + rules (neutral+neutral=15*0.3=4.5 + same temp=15*0.3=4.5) = 53.5. Bajar a ~50.
- `springAndSummerAdjacentScoresWell()` — línea 82: `assertTrue(result.score() >= 0)`. Este test es seguro, no cambia.

---

## ISSUE 2: Score aditivo sin normalización real

### Problema
Cada componente tiene un rango diferente:
- `deltaEScore`: 0-100 (sigmoid)
- `seasonScore`: -20 a +30
- `ruleScore`: -50 a +30 (sin normalizar)

La suma da un rango de -70 a +160, luego se clamp a 0-100. Los números no son interpretables.

### Solución
Normalizar cada componente a 0-100 ANTES de aplicar pesos.

**`ColorCompatibilityEngine.java`** — método `computeRuleScore()`, líneas 174-199

Código actual:
```java
private double computeRuleScore(ColorProfile p1, ColorProfile p2, ...) {
    double score = 0;
    // blacklist: -30, -30, -20
    // positive: +15, +15
    // range: -80 a +30
```

Código nuevo — mapear el rango [-80, 30] a [0, 100]:
```java
private double computeRuleScore(...) {
    double raw = 0;
    // ... mismas reglas ...
    // Normalizar de [-80, 30] a [0, 100]
    raw = Math.max(-80.0, Math.min(30.0, raw));
    return ((raw + 80.0) / 110.0) * 100.0;
}
```

También hay que normalizar `seasonScore`: rango [-20, 30] → [0, 100]:
```java
private static double computeSeasonScore(...) {
    // ... lógica existente ...
    // Normalizar de [-20, 30] a [0, 100]
    double raw = /* resultado de la lógica */;
    raw = Math.max(-20.0, Math.min(30.0, raw));
    return ((raw + 20.0) / 50.0) * 100.0;
}
```

**Impacto en tests**: Todos los tests que verifican scores absolutos cambiarán.

---

## ISSUE 3: Duplicados en palette store

### Problema
Hex duplicados entre estaciones:
| Hex | Estaciones | Color |
|-----|-----------|-------|
| `#C8A2C8` | SPRING + SUMMER | Lila claro / Lila |
| `#E2725B` | SPRING + AUTUMN | Terracota suave / Terracota |
| `#C3B091` | SPRING + AUTUMN | Arena / Caqui |
| `#FFFDD0` | SPRING + AUTUMN | Crema / Crema marfil |
| `#FFD700` | AUTUMN + WINTER | Dorado / Oro brillante |
| `#C0C0C0` | SUMMER + WINTER | Gris perla / Plateado |

### Solución
Eliminar duplicados, mantener cada hex en UNA sola estación. Los colores limítrofes se mueven a NEUTRALS si no encajan bien en una estación.

**`ColorPaletteStore.java`** — eliminar entradas duplicadas:

Línea 40 (SPRING): `new NamedColor("Lila claro", "#C8A2C8", ...)` → ELIMINAR (mantener en SUMMER)
Línea 34 (SPRING): `new NamedColor("Arena", "#C3B091", ...)` → ELIMINAR (mantener en AUTUMN)
Línea 22 (SPRING): `new NamedColor("Crema", "#FFFDD0", ...)` → ELIMINAR (mantener en AUTUMN)
Línea 43 (SPRING): `new NamedColor("Terracota suave", "#E2725B", ...)` → ELIMINAR (mantener en AUTUMN)
Línea 96 (AUTUMN): `new NamedColor("Dorado", "#FFD700", ...)` → ELIMINAR (mantener en WINTER)
Línea 54 (SUMMER): `new NamedColor("Gris perla", "#C0C0C0", ...)` → ELIMINAR (mantener en WINTER)

Esto reduce cada estación de 25 a 24 colores.

### Archivos de test afectados

**`ColorPaletteStoreTest.java`**:
- Línea 38: `assertEquals(25, store.getPalette(ColorSeason.SPRING).size())` → cambiar a `24`
- Línea 46: `assertEquals(25, store.getPalette(ColorSeason.SUMMER).size())` → cambiar a `24`
- Línea 54: `assertEquals(25, store.getPalette(ColorSeason.AUTUMN).size())` → cambiar a `24`
- Línea 62: `assertEquals(25, store.getPalette(ColorSeason.WINTER).size())` → cambiar a `24`
- Línea 95-96: `totalCountIsRoughly100` — el total pasa de ~100 a ~96. Ajustar a `assertTrue(total > 90)` (ya pasa)

**`ColorSeasonClassifierTest.java`**:
- `coralIsSpring()` — `#FF7F50` no está duplicado, no cambia
- `mostazaIsAutumn()` — `#DAA520` no está duplicado, no cambia
- `azulKleinIsWinter()` — `#002FA7` no está duplicado, no cambia
- `lavandaIsSummer()` — `#B2A4D4` no está duplicado, no cambia

**`ColorPaletteStoreTest.java`**:
- `coralIsSpring()` — `#FF7F50` no está duplicado, no cambia
- `greyIsNeutralSoSeasonIsNull()` — `#808080` sigue siendo neutral, no cambia

---

## ISSUE 4: Blacklist demasiado rígida

### Problema
Líneas 180-185 de `ColorCompatibilityEngine.java`:
```java
if (isBadRojoRosa(hex1, hex2)) { score -= 30; }
if (isBadNegroAzulMarino(hex1, hex2)) { score -= 30; }
```

Son reglas absolutas. En moda real son combinaciones válidas.

### Solución
Convertir de blacklist a regla ponderada con contexto de categoría. Reducir penalización de -30 a -10 y añadir un bonus si las categorías son compatibles (ej. Zapatos rojos + Falda rosa puede ser intencional).

**`ColorCompatibilityEngine.java`** — método `computeRuleScore()`, líneas 179-188

Código actual:
```java
if (isBadRojoRosa(hex1, hex2)) {
    score -= 30;
}
if (isBadNegroAzulMarino(hex1, hex2)) {
    score -= 30;
}
```

Código nuevo:
```java
if (isRojoRosaPair(hex1, hex2)) {
    score -= 10;
}
if (isNegroAzulMarinoPair(hex1, hex2)) {
    score -= 10;
}
```

Cambiar nombres de métodos: `isBadRojoRosa` → `isRojoRosaPair`, `isBadNegroAzulMarino` → `isNegroAzulMarinoPair`.

También cambiar las constantes y métodos helper:
- Línea 35: `ROJO_HEX` y `ROSA_HEX` — mantener (sin cambios)
- Línea 205: renombrar `isBadRojoRosa` → `isRojoRosaPair`
- Línea 210: renombrar `isBadNegroAzulMarino` → `isNegroAzulMarinoPair`
- Línea 292-298: actualizar `buildWarnings()` — cambiar texto de warning

**`AiRecommendationService.java`** — línea 95 del prompt:
```
"- Evitá combinar rojo con rosa, o negro con azul marino."
```
→ Cambiar a:
```
"- Rojo con rosa y negro con azul marino son combinaciones de riesgo: si las usás, equilibrá con un neutro."
```

### Archivos de test afectados

**`ColorCompatibilityEngineTest.java`**:
- `redAndPinkHasWarning()` — línea 55: `assertFalse(result.warnings().isEmpty())`. Con -10 en lugar de -30, el score sigue siendo positivo y la warning se mantiene. El test sigue pasando.

---

## ISSUE 5: Temperatura binaria sin gradiente

### Problema
`ColorProfile.fromLab()` líneas 30-37:
```java
if (b > warmCoolThreshold) temperature = WARM;
else if (b < -warmCoolThreshold) temperature = COOL;
else temperature = NEUTRAL;
```

Umbral fijo de 5.0 en b*. Un color con b*=5.1 es WARM, b*=4.9 es NEUTRAL.

### Solución
Añadir `SEMI_WARM` y `SEMI_COOL` como estados intermedios con penalización reducida en las reglas de compatibilidad.

**`ColorTemperature.java`** — añadir dos valores:
```java
public enum ColorTemperature {
    WARM("Cálido"),
    SEMI_WARM("Semi-cálido"),
    COOL("Frío"),
    SEMI_COOL("Semi-frío"),
    NEUTRAL("Neutro");
    // ...
}
```

**`ColorProfile.java`** — método `fromLab()`, líneas 30-37. Cambiar umbral de 5.0 a 3.0 para NEUTRAL y 5.0 para WARM/COOL:
```java
if (b > 5.0) {
    temperature = ColorTemperature.WARM;
} else if (b > 3.0) {
    temperature = ColorTemperature.SEMI_WARM;
} else if (b < -5.0) {
    temperature = ColorTemperature.COOL;
} else if (b < -3.0) {
    temperature = ColorTemperature.SEMI_COOL;
} else {
    temperature = ColorTemperature.NEUTRAL;
}
```

**`ColorCompatibilityEngine.java`** — método `isWarmCoolWithoutNeutral()`, líneas 215-226. Actualizar para que SEMI_WARM + SEMI_COOL no active la penalización:
```java
private boolean isWarmCoolWithoutNeutral(ColorProfile p1, ColorProfile p2, ...) {
    boolean warmCool = (p1.temperature() == ColorTemperature.WARM
            && p2.temperature() == ColorTemperature.COOL)
            || (p1.temperature() == ColorTemperature.COOL
            && p2.temperature() == ColorTemperature.WARM);
    if (!warmCool) return false;
    return !paletteStore.isNeutral(hex1) && !paletteStore.isNeutral(hex2);
}
```
(La lógica actual ya funciona correctamente — SEMI_WARM/SEMI_COOL no activan esta regla.)

**`ColorCompatibilityEngine.java`** — método `computeSeasonScore()`, líneas 142-149. Los SEMI_* se tratan como NEUTRAL en la lógica actual (no son NEUTRAL explícitamente), así que hay que añadirlos:
```java
if (p1.temperature() == ColorTemperature.NEUTRAL
        || p1.temperature() == ColorTemperature.SEMI_WARM
        || p1.temperature() == ColorTemperature.SEMI_COOL
        || p2.temperature() == ColorTemperature.NEUTRAL
        || p2.temperature() == ColorTemperature.SEMI_WARM
        || p2.temperature() == ColorTemperature.SEMI_COOL) {
    return 0;  // semi/neutro actúa como puente
}
```

**`ColorCompatibilityEngine.java`** — método `isSameTemperatureFamily()`, líneas 232-238. Los SEMI_* van con todo:
```java
private static boolean isSameTemperatureFamily(ColorTemperature t1, ColorTemperature t2) {
    if (t1 == ColorTemperature.NEUTRAL || t2 == ColorTemperature.NEUTRAL) return true;
    if (t1 == ColorTemperature.SEMI_WARM || t1 == ColorTemperature.SEMI_COOL) return true;
    if (t2 == ColorTemperature.SEMI_WARM || t2 == ColorTemperature.SEMI_COOL) return true;
    return t1 == t2;
}
```

### Archivos de test afectados

**`ColorSeasonClassifierTest.java`**:
- `blackClassifies()` — `#000000` tiene b*=0, sigue siendo NEUTRAL. No cambia.
- `whiteClassifies()` — `#FFFFFF` tiene b*=0, sigue siendo NEUTRAL. No cambia.

**`ColorCompatibilityEngineTest.java`**:
- `blackAndWhiteScoresHigh()` — negro y blanco siguen siendo NEUTRAL. No cambia.
- `springAndSummerAdjacentScoresWell()` — Coral (#FF7F50) y Lavanda (#D8BFD8) siguen clasificándose igual. No cambia.

**`ColorPaletteStoreTest.java`**:
- `knownNeutrals()` — los hex de neutrales no cambian. No cambia.

---

## ISSUE 6: Categorías de prendas incompletas

### Problema
Línea 50-51 de `ColorCompatibilityEngine.java`:
```java
private static final Set<String> BASE_CATEGORIES = Set.of("Top", "Pantalón", "Vestido", "Falda");
private static final Set<String> LAYER_CATEGORIES = Set.of("Chaqueta", "Abrigo");
```

Solo 6 de 11 categorías tienen reglas. `Camisa`, `Sudadera`, `Zapatos`, `Accesorio`, `Otro` no tienen bonus.

### Solución
Ampliar las categorías con reglas más granulares:

**`ColorCompatibilityEngine.java`** — cambiar definiciones:
```java
private static final Set<String> TOP_CATEGORIES = Set.of("Top", "Camisa", "Sudadera");
private static final Set<String> BOTTOM_CATEGORIES = Set.of("Pantalón", "Falda", "Vestido");
private static final Set<String> LAYER_CATEGORIES = Set.of("Chaqueta", "Abrigo");
private static final Set<String> SHOE_CATEGORIES = Set.of("Zapatos");
private static final Set<String> ACCESSORY_CATEGORIES = Set.of("Accesorio");
```

Cambiar `isBasePlusLayer()` para que considere más combinaciones:
```java
private static boolean isBasePlusLayer(String cat1, String cat2) {
    if (cat1 == null || cat2 == null) return false;
    // Top + Bottom
    if ((TOP_CATEGORIES.contains(cat1) && BOTTOM_CATEGORIES.contains(cat2)) ||
        (BOTTOM_CATEGORIES.contains(cat1) && TOP_CATEGORIES.contains(cat2))) {
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
```

**`GarmentCompatibilityService.java`** — líneas 21-32 `CATEGORY_COMPATIBILITY`. Añadir reglas para categorías faltantes:
```java
Map.entry("Camisa", Set.of("Pantalón", "Falda", "Chaqueta", "Zapatos", "Accesorio")),
Map.entry("Sudadera", Set.of("Pantalón", "Falda", "Zapatos", "Accesorio")),
Map.entry("Zapatos", Set.of("Top", "Camisa", "Sudadera", "Pantalón", "Falda", "Vestido", "Chaqueta", "Abrigo", "Accesorio")),
Map.entry("Accesorio", Set.of("Top", "Camisa", "Sudadera", "Pantalón", "Falda", "Vestido", "Chaqueta", "Abrigo", "Zapatos")),
```
Ya existen `Camisa` y `Sudadera` en el mapa. Solo falta asegurar que `Zapatos` y `Accesorio` cubran todas las categorías superiores.

### Archivos de test afectados

**`GarmentCompatibilityServiceTest.java`**:
- `findCompatibleWithTopReturnsCandidateCategories()` — no cambia (Top ya tiene reglas)
- `findCompatibleWithUnmappedCategoryReturnsEmpty()` — no cambia

---

## ISSUE 7: No hay caching en la clasificación de estación

### Problema
Cada llamada a `ColorCompatibilityEngine.score()` hace 2 llamadas a `classifier.classify()`. Para una vista de armario con 20 prendas mostrando compatibilidades, son 40 conversiones hex→Lab redundantes.

### Solución
Añadir `@Cacheable` en `ColorSeasonClassifier.classify()`.

**`ColorSeasonClassifier.java`** — línea 48, añadir annotation:
```java
@Cacheable(cacheNames = "color-classifications", key = "#hex", unless = "#result == null")
public ColorProfile classify(String hex) {
```

**`ColorSeasonClassifier.java`** — añadir import:
```java
import org.springframework.cache.annotation.Cacheable;
```

**`ColorinchiApplication.java`** — verificar que `@EnableCaching` está presente. Si no, añadirlo.

### Archivos de test afectados

**`ColorSeasonClassifierTest.java`**:
- `consistentClassification()` — ya verifica consistencia. Con cache sigue siendo consistente. No cambia.
- `nullHexThrowsException()` — el cache key es `#hex`, null puede causar problemas. Hay que añadir null check antes del cache:
```java
public ColorProfile classify(String hex) {
    if (hex == null || hex.isBlank()) {
        throw new IllegalArgumentException("Hex cannot be null or blank");
    }
    // ... resto del método
}
```
Este null check también debería moverse desde los tests al servicio.

---

## ISSUE 8: Umbral de neutralidad no configurable

### Problema
`isNeutral()` usa `deltaE76 < 15.0` hardcoded (línea 190 de `ColorPaletteStore.java`). La propiedad `neutralDeltaThreshold` existe en `ColorimetryProperties` pero nunca se usa.

### Solución
Inyectar `ColorimetryProperties` en `ColorPaletteStore` y usar el umbral configurable.

**`ColorPaletteStore.java`** — constructor y campo:
```java
private final ColorimetryProperties props;

public ColorPaletteStore(ColorimetryProperties props) {
    this.props = props;
}
```

Línea 190, cambiar:
```java
if (ColorSpaceConverter.deltaE76(lab, refLab) < props.neutralDeltaThreshold()) {
```

**`ColorCompatibilityEngine.java`** — constructor actual ya recibe `ColorimetryProperties props`. No cambia.

**`ColorSeasonClassifier.java`** — constructor actual ya recibe `ColorimetryProperties`. No cambia.

**`ColorPaletteStoreTest.java`** — todos los tests crean `new ColorPaletteStore()` sin args. Hay que actualizar:
```java
@BeforeEach
void setUp() {
    ColorimetryProperties props = ColorimetryProperties.defaults();
    store = new ColorPaletteStore(props);
}
```

**`ColorCompatibilityEngineTest.java`** — línea 19: `new ColorPaletteStore()` → `new ColorPaletteStore(ColorimetryProperties.defaults())`

**`ColorSeasonClassifierTest.java`** — línea 19: `new ColorPaletteStore()` → `new ColorPaletteStore(ColorimetryProperties.defaults())`

### Archivos de test afectados

**`ColorPaletteStoreTest.java`**:
- `blackIsNeutral()` — `#000000` tiene ΔE=0 respecto al neutral negro. Con umbral configurable (default 15) sigue siendo true.
- `redIsNotNeutral()` — `#FF0000` tiene ΔE alto. Sigue siendo false.

---

## ISSUE 9: ΔE76 vs ΔE00 inconsistente en NN

### Problema
`ColorPaletteStore.classifyByNearestNeighbor()` usa `deltaE76` (euclidiano simple en Lab) para comparar contra la palette. `ColorCompatibilityEngine.score()` usa `deltaE2000` (el más preciso).

### Solución
Cambiar `classifyByNearestNeighbor()` para usar `deltaE2000`.

**`ColorPaletteStore.java`** — línea 214:
```java
// Antes:
double delta = ColorSpaceConverter.deltaE76(lab, refLab);
// Después:
double delta = ColorSpaceConverter.deltaE2000(lab, refLab);
```

También línea 190 (isNeutral):
```java
// Antes:
double[] refLab = ColorSpaceConverter.hexToLab(neutral.hex());
if (ColorSpaceConverter.deltaE76(lab, refLab) < 15.0) {
// Después:
double[] refLab = ColorSpaceConverter.hexToLab(neutral.hex());
if (ColorSpaceConverter.deltaE2000(lab, refLab) < props.neutralDeltaThreshold()) {
```

### Archivos de test afectados

**`ColorPaletteStoreTest.java`**:
- `coralIsSpring()` — `#FF7F50` es el color exacto en la palette SPRING. Con ΔE2000, delta=0. Sigue siendo SPRING.
- `greyIsNeutralSoSeasonIsNull()` — `#808080` es gris puro. Con ΔE2000 sigue siendo más cercano a neutro. No cambia.

---

## ISSUE 10: Intensidad binaria sin gradiente

### Problema
`ColorProfile.fromLab()` líneas 40-44:
```java
ColorIntensity intensity = normalisedChroma > intensityThreshold
    ? ColorIntensity.BRIGHT : ColorIntensity.SOFT;
```

Solo dos categorías. Un color con chroma justo por debajo del umbral se trata igual que uno sin color.

### Solución
Añadir `ColorIntensity.MEDIUM` como categoría intermedia.

**`ColorIntensity.java`** — añadir valor:
```java
public enum ColorIntensity {
    BRIGHT("Vívido"),
    MEDIUM("Medio"),
    SOFT("Suave");
    // ...
}
```

**`ColorProfile.java`** — método `fromLab()`, líneas 40-44. Cambiar a tres niveles:
```java
double chroma = Math.sqrt(a * a + b * b);
double normalisedChroma = Math.min(chroma, 100.0) / 100.0;
ColorIntensity intensity;
if (normalisedChroma > props.intensityThreshold()) {
    intensity = ColorIntensity.BRIGHT;
} else if (normalisedChroma > props.intensityThreshold() * 0.4) {
    intensity = ColorIntensity.MEDIUM;
} else {
    intensity = ColorIntensity.SOFT;
}
```

**`ColorSeasonClassifier.java`** — el constructor actual recibe `ColorimetryProperties` pero no usa el umbral de intensidad para nada más que pasarlo a `ColorProfile.fromLab()`. No cambia.

### Archivos de test afectados

**`ColorSeasonClassifierTest.java`**:
- `coralIsSpring()` — Coral (#FF7F50) tiene chroma alto, sigue siendo BRIGHT. No cambia.
- `lavandaIsSummer()` — Lavanda (#B2A4D4) tiene chroma medio-bajo. Con el nuevo umbral de MEDIUM (0.30 * 0.4 = 0.12), sigue siendo SOFT. No cambia.

---

## RESUMEN DE ARCHIVOS A MODIFICAR

### Producción (10 archivos)

| # | Archivo | Cambios |
|---|---------|---------|
| 1 | `ColorCompatibilityEngine.java` | Normalizar scores, aplicar pesos, reducir blacklist, ampliar categorías |
| 2 | `ColorSeasonClassifier.java` | Añadir @Cacheable, null check, usar props.neutralDeltaThreshold |
| 3 | `ColorPaletteStore.java` | Inyectar props, usar deltaE2000 en NN, umbral configurable, eliminar duplicados |
| 4 | `ColorTemperature.java` | Añadir SEMI_WARM, SEMI_COOL |
| 5 | `ColorIntensity.java` | Añadir MEDIUM |
| 6 | `ColorProfile.java` | Actualizar fromLab() para SEMI_*, MEDIUM, usar props |
| 7 | `AiRecommendationService.java` | Actualizar prompt sobre rojo+rosa |
| 8 | `GarmentCompatibilityService.java` | Ampliar CATEGORY_COMPATIBILITY |
| 9 | `ColorinchiApplication.java` | Verificar @EnableCaching |
| 10 | `ColorimetryProperties.java` | Sin cambios (ya tiene todos los campos) |

### Test (5 archivos)

| # | Archivo | Cambios |
|---|---------|---------|
| 1 | `ColorCompatibilityEngineTest.java` | Ajustar thresholds de score |
| 2 | `ColorPaletteStoreTest.java` | Actualizar constructor, sizes de paletas |
| 3 | `ColorSeasonClassifierTest.java` | Actualizar constructor |
| 4 | `ColorCompatibilityEngineTest.java` | Actualizar constructor |
| 5 | `GarmentCompatibilityServiceTest.java` | Sin cambios (mockea el engine) |

### Config (1 archivo)

| # | Archivo | Cambios |
|---|---------|---------|
| 1 | `application.yml` | Sin cambios necesarios (los valores por defecto son correctos) |

---

## ORDEN DE EJECUCIÓN SUGERIDO

1. **Issue 8** (umbral configurable) — requiere cambiar constructor de `ColorPaletteStore`. Es el más aislado.
2. **Issue 7** (caching) — añadir @Cacheable, verificar @EnableCaching.
3. **Issue 3** (duplicados) — eliminar entradas de `ColorPaletteStore`.
4. **Issue 9** (ΔE2000 en NN) — cambiar deltaE76 → deltaE2000 en `ColorPaletteStore`.
5. **Issue 10** (intensidad MEDIUM) — enum + fromLab().
6. **Issue 5** (temperatura SEMI_*) — enum + fromLab() + engine rules.
7. **Issue 4** (blacklist) — reducir penalización.
8. **Issue 6** (categorías) — ampliar sets.
9. **Issue 2** (normalización) — reescribir computeRuleScore y computeSeasonScore.
10. **Issue 1** (pesos) — aplicar props.weights() en score().

Los issues 1 y 2 dependen de que los componentes estén normalizados, así que van al final.

---

## VERIFICACIÓN FINAL

Tras todos los cambios:
- `mvn test` debe pasar con 0 fallos
- Los scores de compatibilidad deben ser más interpretativos (65 = buena combinación, 40 = mediocre, 20 = conflictiva)
- La clasificación de estación debe ser más estable (cache + ΔE2000)
- Los colores limítrofes deben tener más matices (SEMI_WARM/SEMI_COOL, MEDIUM intensity)
- La palette debe tener colores únicos por estación
