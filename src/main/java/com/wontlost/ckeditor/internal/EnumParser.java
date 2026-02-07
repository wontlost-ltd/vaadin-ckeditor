package com.wontlost.ckeditor.internal;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Safe enum parsing utility class.
 *
 * <p>Provides type-safe enum parsing, avoiding the following common issues:</p>
 * <ul>
 *   <li>Case-conversion issues caused by special locales such as Turkish</li>
 *   <li>Exceptions caused by null or empty strings</li>
 *   <li>IllegalArgumentException caused by invalid values</li>
 * </ul>
 *
 * <p>This class is an internal API and should not be used directly by external code.</p>
 */
public final class EnumParser {

    private static final Logger logger = Logger.getLogger(EnumParser.class.getName());

    private EnumParser() {
        // Utility class, prevent instantiation
    }

    /**
     * Safely parse an enum value.
     *
     * <p>Parsing rules:</p>
     * <ul>
     *   <li>Uses {@link Locale#ROOT} for case conversion to ensure internationalization safety</li>
     *   <li>Returns the default value for null or empty strings</li>
     *   <li>Returns the default value and logs a warning for invalid values</li>
     * </ul>
     *
     * @param value the string value to parse
     * @param enumType the target enum type
     * @param defaultValue the default value when parsing fails
     * @param <T> the enum type
     * @return the parsed enum value, or the default value on failure
     */
    public static <T extends Enum<T>> T parse(String value, Class<T> enumType, T defaultValue) {
        if (value == null || value.isEmpty()) {
            logger.log(Level.FINE, () ->
                String.format("Null or empty value for %s, using default: %s",
                    enumType.getSimpleName(), defaultValue));
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, () ->
                String.format("Invalid %s value: '%s', using default: %s",
                    enumType.getSimpleName(), value, defaultValue));
            return defaultValue;
        }
    }

    /**
     * Safely parse an enum value with a custom log message context.
     *
     * @param value the string value to parse
     * @param enumType the target enum type
     * @param defaultValue the default value when parsing fails
     * @param context context description (used in log messages)
     * @param <T> the enum type
     * @return the parsed enum value, or the default value on failure
     */
    public static <T extends Enum<T>> T parse(String value, Class<T> enumType, T defaultValue, String context) {
        if (value == null || value.isEmpty()) {
            logger.log(Level.FINE, () ->
                String.format("[%s] Null or empty value for %s, using default: %s",
                    context, enumType.getSimpleName(), defaultValue));
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, () ->
                String.format("[%s] Invalid %s value: '%s', using default: %s",
                    context, enumType.getSimpleName(), value, defaultValue));
            return defaultValue;
        }
    }

    /**
     * Strictly parse an enum value, throwing an exception for invalid values.
     *
     * @param value the string value to parse
     * @param enumType the target enum type
     * @param <T> the enum type
     * @return the parsed enum value
     * @throws IllegalArgumentException if the value is null, empty, or invalid
     */
    public static <T extends Enum<T>> T parseStrict(String value, Class<T> enumType) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("%s value must not be null or empty", enumType.getSimpleName()));
        }

        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("Invalid %s value: '%s'. Valid values: %s",
                    enumType.getSimpleName(), value, java.util.Arrays.toString(enumType.getEnumConstants())),
                e);
        }
    }
}
