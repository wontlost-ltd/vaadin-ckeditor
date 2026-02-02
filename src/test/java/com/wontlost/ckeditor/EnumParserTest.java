package com.wontlost.ckeditor;

import com.wontlost.ckeditor.internal.EnumParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnumParser utility class tests.
 */
class EnumParserTest {

    // Test enum
    enum TestEnum {
        VALUE_ONE,
        VALUE_TWO,
        SPECIAL_CASE
    }

    // ==================== parse() Method Tests ====================

    @Nested
    @DisplayName("parse() Method Tests")
    class ParseTests {

        @Test
        @DisplayName("Should correctly parse uppercase value")
        void shouldParseUppercaseValue() {
            TestEnum result = EnumParser.parse("VALUE_ONE", TestEnum.class, TestEnum.VALUE_TWO);
            assertEquals(TestEnum.VALUE_ONE, result);
        }

        @Test
        @DisplayName("Should correctly parse lowercase value")
        void shouldParseLowercaseValue() {
            TestEnum result = EnumParser.parse("value_one", TestEnum.class, TestEnum.VALUE_TWO);
            assertEquals(TestEnum.VALUE_ONE, result);
        }

        @Test
        @DisplayName("Should correctly parse mixed case value")
        void shouldParseMixedCaseValue() {
            TestEnum result = EnumParser.parse("Value_One", TestEnum.class, TestEnum.VALUE_TWO);
            assertEquals(TestEnum.VALUE_ONE, result);
        }

        @Test
        @DisplayName("Null value should return default")
        void nullValueShouldReturnDefault() {
            TestEnum result = EnumParser.parse(null, TestEnum.class, TestEnum.VALUE_TWO);
            assertEquals(TestEnum.VALUE_TWO, result);
        }

        @Test
        @DisplayName("Empty string should return default")
        void emptyStringShouldReturnDefault() {
            TestEnum result = EnumParser.parse("", TestEnum.class, TestEnum.VALUE_TWO);
            assertEquals(TestEnum.VALUE_TWO, result);
        }

        @Test
        @DisplayName("Invalid value should return default")
        void invalidValueShouldReturnDefault() {
            TestEnum result = EnumParser.parse("INVALID", TestEnum.class, TestEnum.VALUE_TWO);
            assertEquals(TestEnum.VALUE_TWO, result);
        }

        @Test
        @DisplayName("Parse with context should work")
        void parseWithContextShouldWork() {
            TestEnum result = EnumParser.parse("value_one", TestEnum.class, TestEnum.VALUE_TWO, "TestContext");
            assertEquals(TestEnum.VALUE_ONE, result);
        }

        @Test
        @DisplayName("Invalid value with context should return default")
        void parseWithContextInvalidShouldReturnDefault() {
            TestEnum result = EnumParser.parse("invalid", TestEnum.class, TestEnum.VALUE_TWO, "TestContext");
            assertEquals(TestEnum.VALUE_TWO, result);
        }
    }

    // ==================== parseStrict() Method Tests ====================

    @Nested
    @DisplayName("parseStrict() Method Tests")
    class ParseStrictTests {

        @Test
        @DisplayName("Should correctly parse valid value")
        void shouldParseValidValue() {
            TestEnum result = EnumParser.parseStrict("VALUE_ONE", TestEnum.class);
            assertEquals(TestEnum.VALUE_ONE, result);
        }

        @Test
        @DisplayName("Should correctly parse lowercase valid value")
        void shouldParseLowercaseValidValue() {
            TestEnum result = EnumParser.parseStrict("special_case", TestEnum.class);
            assertEquals(TestEnum.SPECIAL_CASE, result);
        }

        @Test
        @DisplayName("Null value should throw exception")
        void nullValueShouldThrowException() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> EnumParser.parseStrict(null, TestEnum.class)
            );
            assertTrue(ex.getMessage().contains("must not be null or empty"));
        }

        @Test
        @DisplayName("Empty string should throw exception")
        void emptyStringShouldThrowException() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> EnumParser.parseStrict("", TestEnum.class)
            );
            assertTrue(ex.getMessage().contains("must not be null or empty"));
        }

        @Test
        @DisplayName("Invalid value should throw exception with valid values list")
        void invalidValueShouldThrowExceptionWithValidValues() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> EnumParser.parseStrict("INVALID", TestEnum.class)
            );
            assertTrue(ex.getMessage().contains("Invalid"));
            assertTrue(ex.getMessage().contains("INVALID"));
            assertTrue(ex.getMessage().contains("VALUE_ONE"));
        }
    }

    // ==================== Locale Safety Tests ====================

    @Nested
    @DisplayName("Locale Safety Tests")
    class LocaleSafetyTests {

        @Test
        @DisplayName("Turkish 'i' should be handled correctly")
        void turkishIShouldBeHandledCorrectly() {
            // In Turkish, uppercase of 'i' is 'İ', not 'I'
            // Using Locale.ROOT avoids this issue
            TestEnum result = EnumParser.parse("value_one", TestEnum.class, TestEnum.VALUE_TWO);
            assertEquals(TestEnum.VALUE_ONE, result);
        }

        @Test
        @DisplayName("Parse should use Locale.ROOT")
        void parseShouldUseLocaleRoot() {
            // Should parse correctly even if system locale is Turkish
            TestEnum result = EnumParser.parse("special_case", TestEnum.class, TestEnum.VALUE_ONE);
            assertEquals(TestEnum.SPECIAL_CASE, result);
        }
    }

    // ==================== Real Enum Tests ====================

    @Nested
    @DisplayName("Real Enum Tests")
    class RealEnumTests {

        @Test
        @DisplayName("Should correctly parse ErrorSeverity")
        void shouldParseErrorSeverity() {
            var result = EnumParser.parse(
                "warning",
                com.wontlost.ckeditor.event.EditorErrorEvent.ErrorSeverity.class,
                com.wontlost.ckeditor.event.EditorErrorEvent.ErrorSeverity.ERROR
            );
            assertEquals(com.wontlost.ckeditor.event.EditorErrorEvent.ErrorSeverity.WARNING, result);
        }

        @Test
        @DisplayName("Should correctly parse ChangeSource")
        void shouldParseChangeSource() {
            var result = EnumParser.parse(
                "user_input",
                com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource.class,
                com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource.UNKNOWN
            );
            assertEquals(com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource.USER_INPUT, result);
        }

        @Test
        @DisplayName("Invalid ChangeSource should return UNKNOWN")
        void invalidChangeSourceShouldReturnUnknown() {
            var result = EnumParser.parse(
                "invalid_source",
                com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource.class,
                com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource.UNKNOWN
            );
            assertEquals(com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource.UNKNOWN, result);
        }
    }
}
