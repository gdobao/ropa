Plan: Correcciones menores post-colorimetría
=============================================

Estado: Planificado
Fecha: 2026-06-01
Modelo: DeepSeek v4 Flash vía OpenCode
Tests: 476/476 passing (baseline)

================================================================================
TAREA 1: Arreglar Depth en ColorProfile.fromLab()
================================================================================

Archivo: src/main/java/com/colorinchi/app/colorimetry/model/ColorProfile.java
Líneas: 55-65

Problema: ColorDepth tiene 3 valores (LIGHT, MEDIUM, DARK) pero MEDIUM nunca
se asigna. El código tiene dos ramas `else if` que hacen lo mismo:
  L < 25 → DARK
  L > 70 → LIGHT
  L > 50 → LIGHT  ← duplicado (L > 70 ya capturó esto)
  else → DARK     ← esto es 25 <= L <= 50

Resultado: MEDIUM es dead code, y la zona 25-50 se trata como DARK (demasiado
oscuro para muchos colores de esa luminosidad).

Solución: Corregir la lógica para usar los 3 niveles correctamente:
  L < 25 → DARK
  25 <= L <= 50 → MEDIUM
  L > 50 → LIGHT

Código actual:
```java
// 3. Depth
ColorDepth depth;
if (L < 25) {
    depth = ColorDepth.DARK;
} else if (L > 70) {
    depth = ColorDepth.LIGHT;
} else if (L > 50) {
    depth = ColorDepth.LIGHT;
} else {
    depth = ColorDepth.DARK;
}
```

Código nuevo:
```java
// 3. Depth
ColorDepth depth;
if (L < 25) {
    depth = ColorDepth.DARK;
} else if (L <= 50) {
    depth = ColorDepth.MEDIUM;
} else {
    depth = ColorDepth.LIGHT;
}
```

Impacto en tests:
- WardrobeContextAssemblerTest.java líneas 258 y 285 usan ColorDepth.DARK y
  LIGHT respectivamente. Estos tests mockean el classifier con valores de Lab
  específicos. Hay que verificar si alguno de los colores de prueba cae en la
  zona 25-50 de L* y si esperaba DARK pero ahora recibe MEDIUM.
- Los tests de ColorSeasonClassifierTest no verifican depth explícitamente.
- Los tests de ColorCompatibilityEngineTest no verifican depth explícitamente.

Verificación tras el cambio:
- Correr `mvn test` completo.
- Si algún test falla por depth, revisar qué color de prueba cae en L*=25-50.

================================================================================
TAREA 2: Corregir tamaños de paletas en tests
================================================================================

Archivo: src/test/java/com/colorinchi/app/colorimetry/data/ColorPaletteStoreTest.java

Problema: Los nombres de los tests dicen "~25" o "~24" pero los valores reales
son 21, 24, 24, 25. Los valores de assertEquals son correctos pero los títulos
de los tests son engañosos.

Cambios:

Línea 35: cambiar nombre del test
  // Antes:
  @DisplayName("SPRING has ~25 colors and is not empty")
  // Después:
  @DisplayName("SPRING has 21 colors and is not empty")

Línea 39: el valor 21 ya es correcto. No cambiar.

Línea 43: cambiar nombre del test
  // Antes:
  @DisplayName("SUMMER has ~24 colors and is not empty")
  // Después:
  @DisplayName("SUMMER has 24 colors and is not empty")

Línea 51: cambiar nombre del test
  // Antes:
  @DisplayName("AUTUMN has ~24 colors and is not empty")
  // Después:
  @DisplayName("AUTUMN has 24 colors and is not empty")

Línea 59: cambiar nombre del test
  // Antes:
  @DisplayName("WINTER has ~24 colors and is not empty")
  // Después:
  @DisplayName("WINTER has 25 colors and is not empty")

Línea 63: el valor 25 ya es correcto. No cambiar.

Línea 88: cambiar nombre del test
  // Antes:
  @DisplayName("total palette count is roughly 100")
  // Después:
  @DisplayName("total palette count is 94")

Línea 96: ajustar el range
  // Antes:
  assertTrue(total >= 90 && total <= 105, "total should be 90-105, got " + total);
  // Después:
  assertEquals(94, total, "total should be exactly 94 (21+24+24+25)");

Impacto: 0 cambios de lógica, solo nombres y assert de total.

================================================================================
TAREA 3: Añadir test para ColorDepth.MEDIUM
================================================================================

Archivo: src/test/java/com/colorinchi/app/colorimetry/service/ColorSeasonClassifierTest.java
(crear tests nuevos al final de la clase, antes del último `}`)

Problema: ColorDepth.MEDIUM existía como enum pero nunca se asignaba. Ahora
que Tarea 1 lo corrige, hay que añadir tests que verifiquen explícitamente los
3 niveles de depth.

Añadir estos tests:

```java
@Test
void darkColorHasDarkDepth() {
    // L=10, a=0, b=0 → very dark gray
    ColorProfile profile = classifier.classify("#0A0A0A");
    assertEquals(ColorDepth.DARK, profile.depth());
    assertTrue(profile.depth() == ColorDepth.DARK);
}

@Test
void mediumColorHasMediumDepth() {
    // L=40, a=0, b=0 → medium gray
    ColorProfile profile = classifier.classify("#282828");
    assertEquals(ColorDepth.MEDIUM, profile.depth());
}

@Test
void lightColorHasLightDepth() {
    // L=85, a=0, b=0 → very light gray
    ColorProfile profile = classifier.classify("#D9D9D9");
    assertEquals(ColorDepth.LIGHT, profile.depth());
}

@Test
void depthBoundariesAreCorrect() {
    // L=24 → DARK (just below threshold)
    ColorProfile dark = classifier.classify("#191919");
    assertEquals(ColorDepth.DARK, dark.depth());

    // L=25 → MEDIUM (at threshold)
    ColorProfile mediumLow = classifier.classify("#1C1C1C");
    assertEquals(ColorDepth.MEDIUM, mediumLow.depth());

    // L=50 → MEDIUM (at upper threshold)
    ColorProfile mediumHigh = classifier.classify("#808080");
    assertEquals(ColorDepth.MEDIUM, mediumHigh.depth());

    // L=51 → LIGHT (just above threshold)
    ColorProfile light = classifier.classify("#858585");
    assertEquals(ColorDepth.LIGHT, light.depth());
}
```

Nota: Los hex se eligen para que L* esté en el rango correcto. Los valores
aproximados de L* en CIELAB para grises:
- #0A0A0A → L* ≈ 4 → DARK
- #191919 → L* ≈ 10 → DARK
- #1C1C1C → L* ≈ 11 → DARK (espera, esto también es oscuro)

Mejor usar colores con L* conocido:
- Negro puro #000000 → L*=0 → DARK
- Gris oscuro #333333 → L*≈16 → DARK
- Gris medio #808080 → L*≈50 → MEDIUM (límite)
- Gris claro #CCCCCC → L*≈80 → LIGHT

Los tests deben basarse en L* real, no en el hex aproximado. Para grises
puros (a*=0, b*=0), L* ≈ valor RGB en escala 0-100. Así que:
- #0A0A0A → L* ≈ 3 → DARK ✓
- #191919 → L* ≈ 10 → DARK ✓
- #333333 → L* ≈ 16 → DARK ✓
- #505050 → L* ≈ 25 → MEDIUM (límite inferior)
- #808080 → L* ≈ 50 → MEDIUM (límite superior)
- #999999 → L* ≈ 60 → LIGHT
- #CCCCCC → L* ≈ 80 → LIGHT
- #F0F0F0 → L* ≈ 94 → LIGHT

Tests corregidos:
```java
@Test
void darkColorHasDarkDepth() {
    ColorProfile profile = classifier.classify("#0A0A0A");
    assertEquals(ColorDepth.DARK, profile.depth());
}

@Test
void mediumColorHasMediumDepth() {
    // #505050 → L* ≈ 25 (at MEDIUM boundary)
    ColorProfile profile = classifier.classify("#505050");
    assertEquals(ColorDepth.MEDIUM, profile.depth());
}

@Test
void lightColorHasLightDepth() {
    // #CCCCCC → L* ≈ 80
    ColorProfile profile = classifier.classify("#CCCCCC");
    assertEquals(ColorDepth.LIGHT, profile.depth());
}

@Test
void depthBoundariesAreCorrect() {
    // L* < 25 → DARK
    ColorProfile dark = classifier.classify("#333333");
    assertEquals(ColorDepth.DARK, dark.depth());

    // L* = 25 → MEDIUM (lower boundary)
    ColorProfile mediumLow = classifier.classify("#505050");
    assertEquals(ColorDepth.MEDIUM, mediumLow.depth());

    // L* = 50 → MEDIUM (upper boundary)
    ColorProfile mediumHigh = classifier.classify("#808080");
    assertEquals(ColorDepth.MEDIUM, mediumHigh.depth());

    // L* > 50 → LIGHT
    ColorProfile light = classifier.classify("#999999");
    assertEquals(ColorDepth.LIGHT, light.depth());
}
```

Impacto: 0 cambios en tests existentes. Solo tests nuevos que validan el fix
de Tarea 1.

================================================================================
ORDEN DE EJECUCIÓN
================================================================================

Tarea 1 → Tarea 2 → Tarea 3

La Tarea 1 es la que tiene más riesgo (cambia lógica de depth). Las Tareas 2
y 3 son seguras (solo tests).

Las Tareas 2 y 3 son independientes entre sí y se podrían hacer en paralelo
después de la Tarea 1.

================================================================================
VERIFICACIÓN FINAL
================================================================================

Tras completar las 3 tareas:
  mvn test

Debe pasar con 476+ tests (476 originales + 4 nuevos de Tarea 3 = 480).
0 fallos, 0 errores.
