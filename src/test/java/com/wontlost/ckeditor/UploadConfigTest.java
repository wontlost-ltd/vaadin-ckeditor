package com.wontlost.ckeditor;

import com.wontlost.ckeditor.handler.UploadHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UploadHandler.UploadConfig
 */
class UploadConfigTest {

    private UploadHandler.UploadConfig config;

    @BeforeEach
    void setUp() {
        config = new UploadHandler.UploadConfig();
    }

    @Test
    @DisplayName("Default max file size should be 10MB")
    void defaultMaxFileSize() {
        assertEquals(10 * 1024 * 1024, config.getMaxFileSize());
    }

    @Test
    @DisplayName("Default allowed MIME types should include common image formats")
    void defaultAllowedMimeTypes() {
        String[] mimeTypes = config.getAllowedMimeTypes();
        assertEquals(4, mimeTypes.length);
        assertArrayEquals(new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"}, mimeTypes);
    }

    @Test
    @DisplayName("setMaxFileSize should accept valid values")
    void setMaxFileSizeValid() {
        config.setMaxFileSize(5 * 1024 * 1024);
        assertEquals(5 * 1024 * 1024, config.getMaxFileSize());
    }

    @Test
    @DisplayName("setMaxFileSize should reject zero")
    void setMaxFileSizeZero() {
        assertThrows(IllegalArgumentException.class, () -> config.setMaxFileSize(0));
    }

    @Test
    @DisplayName("setMaxFileSize should reject negative values")
    void setMaxFileSizeNegative() {
        assertThrows(IllegalArgumentException.class, () -> config.setMaxFileSize(-1));
    }

    @Test
    @DisplayName("setMaxFileSize should reject values exceeding 1GB")
    void setMaxFileSizeExceedsLimit() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setMaxFileSize(UploadHandler.UploadConfig.MAX_FILE_SIZE_LIMIT + 1));
    }

    @Test
    @DisplayName("setMaxFileSize should accept 1GB exactly")
    void setMaxFileSizeAtLimit() {
        config.setMaxFileSize(UploadHandler.UploadConfig.MAX_FILE_SIZE_LIMIT);
        assertEquals(UploadHandler.UploadConfig.MAX_FILE_SIZE_LIMIT, config.getMaxFileSize());
    }

    @Test
    @DisplayName("setAllowedMimeTypes should reject null array")
    void setAllowedMimeTypesNull() {
        assertThrows(IllegalArgumentException.class, () -> config.setAllowedMimeTypes((String[]) null));
    }

    @Test
    @DisplayName("setAllowedMimeTypes should reject null element")
    void setAllowedMimeTypesNullElement() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setAllowedMimeTypes("image/jpeg", null, "image/png"));
    }

    @Test
    @DisplayName("setAllowedMimeTypes should reject empty string element")
    void setAllowedMimeTypesEmptyElement() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setAllowedMimeTypes("image/jpeg", "", "image/png"));
    }

    @Test
    @DisplayName("setAllowedMimeTypes should accept valid values")
    void setAllowedMimeTypesValid() {
        config.setAllowedMimeTypes("application/pdf", "text/plain");
        String[] mimeTypes = config.getAllowedMimeTypes();
        assertEquals(2, mimeTypes.length);
        assertArrayEquals(new String[]{"application/pdf", "text/plain"}, mimeTypes);
    }

    @Test
    @DisplayName("setAllowedMimeTypes should trim whitespace")
    void setAllowedMimeTypesTrimWhitespace() {
        config.setAllowedMimeTypes("  image/jpeg  ", "image/png");
        String[] mimeTypes = config.getAllowedMimeTypes();
        assertEquals("image/jpeg", mimeTypes[0]);
    }

    @Test
    @DisplayName("addAllowedMimeTypes should add to existing list")
    void addAllowedMimeTypes() {
        config.addAllowedMimeTypes("application/pdf");
        String[] mimeTypes = config.getAllowedMimeTypes();
        assertEquals(5, mimeTypes.length);
        assertTrue(java.util.Arrays.asList(mimeTypes).contains("application/pdf"));
    }

    @Test
    @DisplayName("resetAllowedMimeTypes should restore defaults")
    void resetAllowedMimeTypes() {
        config.setAllowedMimeTypes("application/pdf");
        config.resetAllowedMimeTypes();
        String[] mimeTypes = config.getAllowedMimeTypes();
        assertEquals(4, mimeTypes.length);
    }

    @Test
    @DisplayName("validate should return null for valid upload")
    void validateValid() {
        UploadHandler.UploadContext context = createMockContext("image/jpeg", 1024);
        assertNull(config.validate(context));
    }

    @Test
    @DisplayName("validate should reject file exceeding max size")
    void validateFileTooLarge() {
        UploadHandler.UploadContext context = createMockContext("image/jpeg", 20 * 1024 * 1024);
        String error = config.validate(context);
        assertNotNull(error);
        assertTrue(error.contains("exceeds maximum"));
    }

    @Test
    @DisplayName("validate should reject disallowed MIME type")
    void validateDisallowedMimeType() {
        UploadHandler.UploadContext context = createMockContext("application/pdf", 1024);
        String error = config.validate(context);
        assertNotNull(error);
        assertTrue(error.contains("not allowed"));
    }

    @Test
    @DisplayName("validate should allow any MIME type when list is empty")
    void validateEmptyMimeTypeList() {
        config.setAllowedMimeTypes(); // Empty array
        UploadHandler.UploadContext context = createMockContext("application/octet-stream", 1024);
        assertNull(config.validate(context));
    }

    @Test
    @DisplayName("validate should reject null context")
    void validateNullContext() {
        String error = config.validate(null);
        assertNotNull(error);
        assertTrue(error.contains("null"));
    }

    // review: null mimeType 应被拒绝，且错误信息显示 "unknown" 而非字面 "null"
    @Test
    @DisplayName("validate should reject null MIME type with a meaningful 'unknown' message")
    void validateNullMimeType() {
        config.setAllowedMimeTypes("image/jpeg", "image/png");
        UploadHandler.UploadContext context = createMockContext(null, 1024);

        String error = config.validate(context);

        assertNotNull(error, "null mime type must be rejected when a whitelist is set");
        assertTrue(error.contains("not allowed"), "should report disallowed: " + error);
        assertTrue(error.contains("unknown"), "should show 'unknown' not 'null': " + error);
        assertFalse(error.contains("'null'"), "must not print the literal 'null': " + error);
    }

    @Test
    @DisplayName("validate should allow null MIME type when whitelist is empty")
    void validateNullMimeTypeEmptyWhitelist() {
        config.setAllowedMimeTypes(); // empty = allow all
        UploadHandler.UploadContext context = createMockContext(null, 1024);
        assertNull(config.validate(context));
    }

    private UploadHandler.UploadContext createMockContext(String mimeType, long fileSize) {
        return new UploadHandler.UploadContext("test.file", mimeType, fileSize);
    }
}
