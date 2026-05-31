package com.colorinchi.app.colorimetry.data;

import com.colorinchi.app.colorimetry.model.ColorSeason;
import com.colorinchi.app.colorimetry.model.NamedColor;
import com.colorinchi.app.colorimetry.util.ColorSpaceConverter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ColorPaletteStore {

    // ---------------------------------------------------------------
    // SPRING — warm, clear, light
    // ---------------------------------------------------------------
    private static final List<NamedColor> SPRING = List.of(
        new NamedColor("Coral",              "#FF7F50", ColorSeason.SPRING, false),
        new NamedColor("Melocotón",          "#FFDAB9", ColorSeason.SPRING, false),
        new NamedColor("Amarillo pollito",   "#FFE135", ColorSeason.SPRING, false),
        new NamedColor("Crema",              "#FFFDD0", ColorSeason.SPRING, false),
        new NamedColor("Verde manzana",      "#8DB600", ColorSeason.SPRING, false),
        new NamedColor("Azul cielo",         "#87CEEB", ColorSeason.SPRING, false),
        new NamedColor("Turquesa claro",     "#40E0D0", ColorSeason.SPRING, false),
        new NamedColor("Marfil",             "#FFFFF0", ColorSeason.SPRING, false),
        new NamedColor("Beige dorado",       "#E8C39E", ColorSeason.SPRING, false),
        new NamedColor("Salmón",             "#FA8072", ColorSeason.SPRING, false),
        new NamedColor("Naranja claro",      "#FFB347", ColorSeason.SPRING, false),
        new NamedColor("Rosa coral",         "#F08080", ColorSeason.SPRING, false),
        new NamedColor("Lavanda claro",      "#E6E6FA", ColorSeason.SPRING, false),
        new NamedColor("Verde menta",        "#98FF98", ColorSeason.SPRING, false),
        new NamedColor("Oro claro",          "#FADA5E", ColorSeason.SPRING, false),
        new NamedColor("Topo claro",         "#A89F91", ColorSeason.SPRING, false),
        new NamedColor("Arena",              "#C3B091", ColorSeason.SPRING, false),
        new NamedColor("Melón",              "#FDBCB4", ColorSeason.SPRING, false),
        new NamedColor("Albaricoque",        "#FBCEB1", ColorSeason.SPRING, false),
        new NamedColor("Perla",              "#F0E6D3", ColorSeason.SPRING, false),
        new NamedColor("Aguamarina",         "#7FFFD4", ColorSeason.SPRING, false),
        new NamedColor("Lila claro",         "#C8A2C8", ColorSeason.SPRING, false),
        new NamedColor("Mantequilla",        "#FFF3B3", ColorSeason.SPRING, false),
        new NamedColor("Cobre claro",        "#DA8A67", ColorSeason.SPRING, false),
        new NamedColor("Terracota suave",    "#E2725B", ColorSeason.SPRING, false)
    );

    // ---------------------------------------------------------------
    // SUMMER — cool, muted, soft
    // ---------------------------------------------------------------
    private static final List<NamedColor> SUMMER = List.of(
        new NamedColor("Rosa empolvado",      "#F4C2C2", ColorSeason.SUMMER, false),
        new NamedColor("Lavanda",             "#D8BFD8", ColorSeason.SUMMER, false),
        new NamedColor("Azul cielo grisáceo", "#B0C4DE", ColorSeason.SUMMER, false),
        new NamedColor("Verde salvia",        "#8A9A5B", ColorSeason.SUMMER, false),
        new NamedColor("Gris perla",          "#C0C0C0", ColorSeason.SUMMER, false),
        new NamedColor("Lila",                "#C8A2C8", ColorSeason.SUMMER, false),
        new NamedColor("Azul pizarra claro",  "#7B8D8E", ColorSeason.SUMMER, false),
        new NamedColor("Rosa viejo",          "#C08081", ColorSeason.SUMMER, false),
        new NamedColor("Azul denim lavado",   "#6F8FA0", ColorSeason.SUMMER, false),
        new NamedColor("Beige rosado",        "#E8C5A0", ColorSeason.SUMMER, false),
        new NamedColor("Malva",               "#B784A7", ColorSeason.SUMMER, false),
        new NamedColor("Turquesa pastel",     "#AFE4DE", ColorSeason.SUMMER, false),
        new NamedColor("Verde musgo claro",   "#7B9C6E", ColorSeason.SUMMER, false),
        new NamedColor("Gris topo",           "#8B8589", ColorSeason.SUMMER, false),
        new NamedColor("Frambuesa suave",     "#D4879A", ColorSeason.SUMMER, false),
        new NamedColor("Uva",                 "#6B3FA0", ColorSeason.SUMMER, false),
        new NamedColor("Ciruela claro",       "#A389B4", ColorSeason.SUMMER, false),
        new NamedColor("Azul acero claro",    "#8CB4D3", ColorSeason.SUMMER, false),
        new NamedColor("Crema rosada",        "#FFDED9", ColorSeason.SUMMER, false),
        new NamedColor("Plata",               "#C9C9C9", ColorSeason.SUMMER, false),
        new NamedColor("Nude rosado",         "#F5D2C1", ColorSeason.SUMMER, false),
        new NamedColor("Lino",                "#FAF0E6", ColorSeason.SUMMER, false),
        new NamedColor("Violeta claro",       "#D3B8E6", ColorSeason.SUMMER, false),
        new NamedColor("Azul tormenta",       "#4F6F8F", ColorSeason.SUMMER, false),
        new NamedColor("Rosa bebé",           "#F4C4C8", ColorSeason.SUMMER, false)
    );

    // ---------------------------------------------------------------
    // AUTUMN — warm, deep, earthy
    // ---------------------------------------------------------------
    private static final List<NamedColor> AUTUMN = List.of(
        new NamedColor("Mostaza",           "#FFDB58", ColorSeason.AUTUMN, false),
        new NamedColor("Terracota",         "#E2725B", ColorSeason.AUTUMN, false),
        new NamedColor("Verde oliva",       "#808000", ColorSeason.AUTUMN, false),
        new NamedColor("Marrón chocolate",  "#7B3F00", ColorSeason.AUTUMN, false),
        new NamedColor("Naranja quemado",   "#CC5500", ColorSeason.AUTUMN, false),
        new NamedColor("Borgoña",           "#800020", ColorSeason.AUTUMN, false),
        new NamedColor("Caqui",             "#C3B091", ColorSeason.AUTUMN, false),
        new NamedColor("Camel",             "#C19A6B", ColorSeason.AUTUMN, false),
        new NamedColor("Marrón tostado",    "#8B4513", ColorSeason.AUTUMN, false),
        new NamedColor("Ocre",              "#CC7722", ColorSeason.AUTUMN, false),
        new NamedColor("Verde bosque",      "#228B22", ColorSeason.AUTUMN, false),
        new NamedColor("Óxido",             "#B7410E", ColorSeason.AUTUMN, false),
        new NamedColor("Marrón canela",     "#D2691E", ColorSeason.AUTUMN, false),
        new NamedColor("Mostaza oscura",    "#D4AF37", ColorSeason.AUTUMN, false),
        new NamedColor("Crema marfil",      "#FFFDD0", ColorSeason.AUTUMN, false),
        new NamedColor("Dorado",            "#FFD700", ColorSeason.AUTUMN, false),
        new NamedColor("Teja",              "#CC5533", ColorSeason.AUTUMN, false),
        new NamedColor("Berenjena",         "#483D8B", ColorSeason.AUTUMN, false),
        new NamedColor("Verde cardo",       "#D8BFD8", ColorSeason.AUTUMN, false),
        new NamedColor("Beige marrón",      "#BC987E", ColorSeason.AUTUMN, false),
        new NamedColor("Latón",             "#B5A642", ColorSeason.AUTUMN, false),
        new NamedColor("Oliva oscuro",      "#556B2F", ColorSeason.AUTUMN, false),
        new NamedColor("Marrón tabaco",     "#6B4C3E", ColorSeason.AUTUMN, false),
        new NamedColor("Siena",             "#A0522D", ColorSeason.AUTUMN, false),
        new NamedColor("Granate",           "#800000", ColorSeason.AUTUMN, false)
    );

    // ---------------------------------------------------------------
    // WINTER — cool, clear, deep
    // ---------------------------------------------------------------
    private static final List<NamedColor> WINTER = List.of(
        new NamedColor("Azul klein",        "#002FA7", ColorSeason.WINTER, false),
        new NamedColor("Negro",             "#000000", ColorSeason.WINTER, false),
        new NamedColor("Blanco puro",       "#FFFFFF", ColorSeason.WINTER, false),
        new NamedColor("Rojo",              "#FF0000", ColorSeason.WINTER, false),
        new NamedColor("Frambuesa",         "#DC143C", ColorSeason.WINTER, false),
        new NamedColor("Verde esmeralda",   "#50C878", ColorSeason.WINTER, false),
        new NamedColor("Plateado",          "#C0C0C0", ColorSeason.WINTER, false),
        new NamedColor("Azul marino",       "#000080", ColorSeason.WINTER, false),
        new NamedColor("Fucsia",            "#FF00FF", ColorSeason.WINTER, false),
        new NamedColor("Blanco hielo",      "#F0FFFF", ColorSeason.WINTER, false),
        new NamedColor("Gris carbón",       "#36454F", ColorSeason.WINTER, false),
        new NamedColor("Azul cobalto",      "#0047AB", ColorSeason.WINTER, false),
        new NamedColor("Violeta",           "#8F00FF", ColorSeason.WINTER, false),
        new NamedColor("Púrpura",           "#800080", ColorSeason.WINTER, false),
        new NamedColor("Magenta",           "#FF0090", ColorSeason.WINTER, false),
        new NamedColor("Verde botella",     "#006A4E", ColorSeason.WINTER, false),
        new NamedColor("Azul medianoche",   "#191970", ColorSeason.WINTER, false),
        new NamedColor("Burdeos",           "#8B0000", ColorSeason.WINTER, false),
        new NamedColor("Gris oscuro",       "#A9A9A9", ColorSeason.WINTER, false),
        new NamedColor("Hielo",             "#E0FFFF", ColorSeason.WINTER, false),
        new NamedColor("Oro brillante",     "#FFD700", ColorSeason.WINTER, false),
        new NamedColor("Ciruela intenso",   "#660066", ColorSeason.WINTER, false),
        new NamedColor("Azul Prusia",       "#003153", ColorSeason.WINTER, false),
        new NamedColor("Rojo cereza",       "#DE3163", ColorSeason.WINTER, false),
        new NamedColor("Blanco níveo",      "#FFFAFA", ColorSeason.WINTER, false)
    );

    // ---------------------------------------------------------------
    // NEUTRAL colours (season = null, neutral = true)
    // ---------------------------------------------------------------
    private static final List<NamedColor> NEUTRALS = List.of(
        new NamedColor("Negro",          "#000000", null, true),
        new NamedColor("Blanco",         "#FFFFFF", null, true),
        new NamedColor("Gris",           "#808080", null, true),
        new NamedColor("Beige",          "#F5F5DC", null, true),
        new NamedColor("Camel",          "#C19A6B", null, true),
        new NamedColor("Marfil",         "#FFFFF0", null, true),
        new NamedColor("Arena",          "#C3B091", null, true),
        new NamedColor("Nude",           "#E8C5A0", null, true),
        new NamedColor("Caqui",          "#BDB76B", null, true),
        new NamedColor("Azul Marino",    "#000080", null, true),
        new NamedColor("Azul Niebla",    "#B0C4DE", null, true),
        new NamedColor("Marrón Chocolate", "#7B3F00", null, true),
        new NamedColor("Verde Musgo",    "#4A5D23", null, true),
        new NamedColor("Gris Perla",     "#C0C0C0", null, true),
        new NamedColor("Blanco Roto",    "#FDF5E6", null, true)
    );

    // ---------------------------------------------------------------
    // Lazy-loaded palettes map
    // ---------------------------------------------------------------
    private final Map<ColorSeason, List<NamedColor>> palettes = loadPalettes();

    // ---------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------

    private static Map<ColorSeason, List<NamedColor>> loadPalettes() {
        Map<ColorSeason, List<NamedColor>> map = new HashMap<>(4);
        map.put(ColorSeason.SPRING, SPRING);
        map.put(ColorSeason.SUMMER, SUMMER);
        map.put(ColorSeason.AUTUMN, AUTUMN);
        map.put(ColorSeason.WINTER, WINTER);
        return Map.copyOf(map);
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Returns {@code true} when the given hex colour is perceptually close
     * (ΔE76 < 15) to any colour in the neutral palette.
     */
    public boolean isNeutral(String hex) {
        double[] lab = ColorSpaceConverter.hexToLab(hex);
        for (NamedColor neutral : NEUTRALS) {
            double[] refLab = ColorSpaceConverter.hexToLab(neutral.hex());
            if (ColorSpaceConverter.deltaE76(lab, refLab) < 15.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the closest named colour across <strong>all</strong> seasonal
     * palettes and the neutral list using CIEDE76, and returns the
     * {@link ColorSeason} of that colour.
     *
     * @return the season of the nearest named colour, or {@code null} if the
     *         closest colour is a neutral
     */
    public ColorSeason classifyByNearestNeighbor(String hex) {
        double[] lab = ColorSpaceConverter.hexToLab(hex);
        double minDelta = Double.MAX_VALUE;
        ColorSeason closest = null;

        // Search all seasonal palettes
        for (Map.Entry<ColorSeason, List<NamedColor>> entry : palettes.entrySet()) {
            for (NamedColor named : entry.getValue()) {
                double[] refLab = ColorSpaceConverter.hexToLab(named.hex());
                double delta = ColorSpaceConverter.deltaE76(lab, refLab);
                if (delta < minDelta) {
                    minDelta = delta;
                    closest = entry.getKey();
                }
            }
        }

        // Search neutrals (season = null)
        for (NamedColor neutral : NEUTRALS) {
            double[] refLab = ColorSpaceConverter.hexToLab(neutral.hex());
            double delta = ColorSpaceConverter.deltaE76(lab, refLab);
            if (delta < minDelta) {
                minDelta = delta;
                closest = null;  // neutral → no season
            }
        }

        return closest;
    }

    /**
     * Returns the palette for a given season as an unmodifiable list.
     */
    public List<NamedColor> getPalette(ColorSeason season) {
        return palettes.get(season);
    }

    /**
     * Returns an unmodifiable view of all seasonal palettes.
     */
    public Map<ColorSeason, List<NamedColor>> getAllPalettes() {
        return palettes;
    }
}
