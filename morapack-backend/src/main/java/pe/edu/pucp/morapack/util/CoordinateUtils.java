package pe.edu.pucp.morapack.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for converting coordinate formats.
 * Handles DMS (Degrees Minutes Seconds) to Decimal conversion.
 */
public class CoordinateUtils {

    // Pattern to match DMS format with symbols: 12°34'56"N or 12°34'56"S or 12°34'56"E or 12°34'56"W
    private static final Pattern DMS_PATTERN_SYMBOLS = Pattern.compile(
        "(\\d+)°(\\d+)'([\\d.]+)\"([NSEW])"
    );

    // Pattern to match DMS format with spaces: 12 34 56 N or 12 34 56 S (as stored in DB)
    private static final Pattern DMS_PATTERN_SPACES = Pattern.compile(
        "(\\d+)\\s+(\\d+)\\s+([\\d.]+)\\s+([NSEW])"
    );

    /**
     * Convert DMS (Degrees Minutes Seconds) coordinate to decimal format.
     *
     * Supported formats:
     *   "04°42'08\"S" → -4.702222 (with symbols)
     *   "04 42 08 S" → -4.702222 (with spaces, as stored in DB)
     *   "74°12'23\"W" → -74.206389
     *   "74 12 23 W" → -74.206389
     *   "52°18'29\"N" → 52.308056
     *   "-4.702222" → -4.702222 (already decimal)
     *
     * @param dms DMS formatted coordinate string
     * @return Decimal coordinate value
     * @throws IllegalArgumentException if format is invalid
     */
    public static double dmsToDecimal(String dms) {
        if (dms == null || dms.trim().isEmpty()) {
            throw new IllegalArgumentException("DMS coordinate cannot be null or empty");
        }

        dms = dms.trim();

        // Try to parse as decimal first (in case it's already decimal)
        try {
            return Double.parseDouble(dms);
        } catch (NumberFormatException e) {
            // Not a simple decimal, continue with DMS parsing
        }

        // Try pattern with symbols first (04°42'08"S)
        Matcher matcher = DMS_PATTERN_SYMBOLS.matcher(dms);
        if (!matcher.matches()) {
            // Try pattern with spaces (04 42 08 S)
            matcher = DMS_PATTERN_SPACES.matcher(dms);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid DMS format: " + dms +
                    " (expected format: '12°34'56\"N' or '12 34 56 N')");
            }
        }

        int degrees = Integer.parseInt(matcher.group(1));
        int minutes = Integer.parseInt(matcher.group(2));
        double seconds = Double.parseDouble(matcher.group(3));
        String direction = matcher.group(4);

        // Calculate decimal degrees
        double decimal = degrees + (minutes / 60.0) + (seconds / 3600.0);

        // Apply sign based on direction (S and W are negative)
        if (direction.equals("S") || direction.equals("W")) {
            decimal = -decimal;
        }

        return decimal;
    }

    /**
     * Safe conversion that returns 0.0 on error and logs warning.
     * Used when we want to continue processing even if a coordinate is invalid.
     *
     * @param dms DMS formatted coordinate string
     * @param airportCode Airport code for error logging
     * @param coordinateType "latitude" or "longitude" for error logging
     * @return Decimal coordinate value, or 0.0 if conversion fails
     */
    public static double dmsToDecimalSafe(String dms, String airportCode, String coordinateType) {
        try {
            return dmsToDecimal(dms);
        } catch (Exception e) {
            System.err.println("Warning: Could not parse " + coordinateType +
                " for airport " + airportCode + ": " + dms + " - " + e.getMessage());
            return 0.0;
        }
    }
}
