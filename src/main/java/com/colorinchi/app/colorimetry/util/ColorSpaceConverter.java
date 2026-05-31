package com.colorinchi.app.colorimetry.util;

import java.util.Locale;

/**
 * Static utility for colour-space conversions and colour-difference formulae.
 * <p>
 * All methods operate in the sRGB / D65 colour space.
 */
public final class ColorSpaceConverter {

    // D65 illuminant reference white (XYZ)
    private static final double[] D65 = {95.047, 100.0, 108.883};

    private static final double DEG = Math.PI / 180.0;
    private static final double RAD = 180.0 / Math.PI;

    private ColorSpaceConverter() {
    }

    // ---------------------------------------------------------------
    // hex → RGB
    // ---------------------------------------------------------------

    /**
     * Parses a hex colour string and returns [R, G, B] as doubles in the range 0–255.
     *
     * @param hex colour string; may include a leading {@code #}
     * @return array [R, G, B]
     * @throws IllegalArgumentException if the input is not a valid hex colour
     */
    public static double[] hexToRgb(String hex) {
        String clean = hex.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if (clean.length() != 6) {
            throw new IllegalArgumentException("Invalid hex colour: " + hex);
        }
        double r = Integer.parseInt(clean.substring(0, 2), 16);
        double g = Integer.parseInt(clean.substring(2, 4), 16);
        double b = Integer.parseInt(clean.substring(4, 6), 16);
        return new double[]{r, g, b};
    }

    // ---------------------------------------------------------------
    // RGB (0–255) → XYZ
    // ---------------------------------------------------------------

    /**
     * Converts sRGB values (0–255) to CIE XYZ (D65).
     */
    public static double[] rgbToXyz(double r, double g, double b) {
        double rLin = srgbLinearize(r / 255.0);
        double gLin = srgbLinearize(g / 255.0);
        double bLin = srgbLinearize(b / 255.0);

        double x = 0.4124564 * rLin + 0.3575761 * gLin + 0.1804375 * bLin;
        double y = 0.2126729 * rLin + 0.7151522 * gLin + 0.0721750 * bLin;
        double z = 0.0193339 * rLin + 0.1191920 * gLin + 0.9503041 * bLin;

        return new double[]{x * 100.0, y * 100.0, z * 100.0};
    }

    private static double srgbLinearize(double c) {
        if (c <= 0.04045) {
            return c / 12.92;
        }
        return Math.pow((c + 0.055) / 1.055, 2.4);
    }

    // ---------------------------------------------------------------
    // XYZ → CIELAB (D65)
    // ---------------------------------------------------------------

    /**
     * Converts CIE XYZ (D65) to CIELAB L*a*b*.
     */
    public static double[] xyzToLab(double x, double y, double z) {
        double fx = labF(x / D65[0]);
        double fy = labF(y / D65[1]);
        double fz = labF(z / D65[2]);

        double L = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double b = 200.0 * (fy - fz);

        return new double[]{L, a, b};
    }

    private static double labF(double t) {
        if (t > 0.008856) {
            return Math.cbrt(t);
        }
        return (903.3 * t + 16.0) / 116.0;
    }

    // ---------------------------------------------------------------
    // hex → Lab (convenience)
    // ---------------------------------------------------------------

    /**
     * Convenience method: hex → RGB → XYZ → Lab.
     */
    public static double[] hexToLab(String hex) {
        double[] rgb = hexToRgb(hex);
        double[] xyz = rgbToXyz(rgb[0], rgb[1], rgb[2]);
        return xyzToLab(xyz[0], xyz[1], xyz[2]);
    }

    // ---------------------------------------------------------------
    // hex → HSL
    // ---------------------------------------------------------------

    /**
     * Converts a hex colour to HSL.
     *
     * @return [H, S, L] where H = 0–360, S = 0–100, L = 0–100
     */
    public static double[] hexToHsl(String hex) {
        double[] rgb = hexToRgb(hex);
        double r = rgb[0] / 255.0;
        double g = rgb[1] / 255.0;
        double b = rgb[2] / 255.0;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;

        double h = 0.0;
        double s;
        double l = (max + min) / 2.0;

        if (delta == 0.0) {
            h = 0.0;
            s = 0.0;
        } else {
            s = l <= 0.5
                ? delta / (max + min)
                : delta / (2.0 - max - min);

            if (max == r) {
                h = ((g - b) / delta + (g < b ? 6.0 : 0.0)) * 60.0;
            } else if (max == g) {
                h = ((b - r) / delta + 2.0) * 60.0;
            } else {
                h = ((r - g) / delta + 4.0) * 60.0;
            }
        }

        return new double[]{h, s * 100.0, l * 100.0};
    }

    // ---------------------------------------------------------------
    // Colour-difference formulae
    // ---------------------------------------------------------------

    /**
     * CIEDE2000 colour-difference — the most accurate perceptual difference formula.
     *
     * @param lab1 [L, a, b]
     * @param lab2 [L, a, b]
     * @return ΔE00
     */
    public static double deltaE2000(double[] lab1, double[] lab2) {
        double L1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
        double L2 = lab2[0], a2 = lab2[1], b2 = lab2[2];

        // Step 1: Calculate C₁, C₂, C̄
        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double Cb = (C1 + C2) / 2.0;

        // Step 2: Calculate G
        double G = 0.5 * (1.0 - Math.sqrt(Math.pow(Cb, 7.0) / (Math.pow(Cb, 7.0) + Math.pow(25.0, 7.0))));

        // Step 3: Calculate a', C', h'
        double a1p = (1.0 + G) * a1;
        double a2p = (1.0 + G) * a2;

        double C1p = Math.sqrt(a1p * a1p + b1 * b1);
        double C2p = Math.sqrt(a2p * a2p + b2 * b2);

        double h1p = Math.toDegrees(Math.atan2(b1, a1p));
        if (h1p < 0) h1p += 360.0;
        double h2p = Math.toDegrees(Math.atan2(b2, a2p));
        if (h2p < 0) h2p += 360.0;

        // Step 4: Calculate ΔL', ΔC', ΔH'
        double dLp = L2 - L1;
        double dCp = C2p - C1p;

        double dhp;
        double Cbp = (C1p + C2p) / 2.0;
        double Hbp;

        double hpDiff = h2p - h1p;
        double hpSum = h1p + h2p;
        double hpAbsDiff = Math.abs(hpDiff);

        if (C1p == 0.0 || C2p == 0.0) {
            dhp = 0.0;
            Hbp = hpSum;
        } else if (hpAbsDiff <= 180.0) {
            dhp = hpDiff;
            Hbp = hpSum / 2.0;
        } else if (hpDiff > 180.0) {
            dhp = hpDiff - 360.0;
            Hbp = (hpSum + 360.0) / 2.0;
        } else {
            dhp = hpDiff + 360.0;
            Hbp = (hpSum - 360.0) / 2.0;
        }

        double dHp = 2.0 * Math.sqrt(C1p * C2p) * Math.sin(dhp * DEG);

        // Step 5: Calculate parametric factors — kL = kC = kH = 1.0
        double kL = 1.0, kC = 1.0, kH = 1.0;

        // Step 6: Calculate SL, SC, SH, RT
        double Lbp = (L1 + L2) / 2.0;

        double SL = 1.0 + 0.015 * Math.pow(Lbp - 50.0, 2.0) / Math.sqrt(20.0 + Math.pow(Lbp - 50.0, 2.0));
        double SC = 1.0 + 0.045 * Cbp;
        double SH = 1.0 + 0.015 * Cbp * T(Cbp, Hbp);

        // Rotation term
        double dTheta = 30.0 * Math.exp(-Math.pow((Hbp - 275.0) / 25.0, 2.0));
        double RC = 2.0 * Math.sqrt(Math.pow(Cbp, 7.0) / (Math.pow(Cbp, 7.0) + Math.pow(25.0, 7.0)));
        double RT = -RC * Math.sin(2.0 * dTheta * DEG);

        // Step 7: ΔE00
        double term1 = Math.pow(dLp / (kL * SL), 2.0);
        double term2 = Math.pow(dCp / (kC * SC), 2.0);
        double term3 = Math.pow(dHp / (kH * SH), 2.0);
        double term4 = RT * (dCp / (kC * SC)) * (dHp / (kH * SH));

        return Math.sqrt(term1 + term2 + term3 + term4);
    }

    private static double T(double Cbp, double Hbp) {
        return 1.0
            - 0.17 * Math.cos((Hbp - 30.0) * DEG)
            + 0.24 * Math.cos((2.0 * Hbp) * DEG)
            + 0.32 * Math.cos((3.0 * Hbp + 6.0) * DEG)
            - 0.20 * Math.cos((4.0 * Hbp - 63.0) * DEG);
    }

    /**
     * CIEDE76 colour-difference — simple Euclidean distance in CIELAB space.
     *
     * @param lab1 [L, a, b]
     * @param lab2 [L, a, b]
     * @return ΔE76
     */
    public static double deltaE76(double[] lab1, double[] lab2) {
        double dL = lab1[0] - lab2[0];
        double da = lab1[1] - lab2[1];
        double db = lab1[2] - lab2[2];
        return Math.sqrt(dL * dL + da * da + db * db);
    }
}
