package com.colorinchi.app.dto;

public record OutfitPiece(String category, String colorName, String colorHex, int bodyZone, boolean lightText) {

    public static int zoneFor(String category) {
        if (category == null) return 99;
        return switch (category) {
            case "Accesorio", "Otro" -> 0;
            case "Chaqueta", "Abrigo" -> 1;
            case "Camisa", "Top", "Sudadera", "Vestido" -> 2;
            case "Pantalón", "Falda" -> 3;
            case "Zapatos" -> 4;
            default -> 99;
        };
    }

    public static boolean isLightText(String hex) {
        if (hex == null || hex.isBlank()) return false;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
            return lum > 0.55;
        } catch (Exception e) {
            return false;
        }
    }
}
