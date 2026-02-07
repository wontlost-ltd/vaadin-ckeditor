package com.wontlost.ckeditor.handler;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * File upload handler.
 * Handles image and file uploads in the editor.
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Upload to local filesystem
 * editor.setUploadHandler((context, stream) -&gt; {
 *     String filename = context.getFileName();
 *     Path targetPath = uploadDir.resolve(filename);
 *     Files.copy(stream, targetPath);
 *     return CompletableFuture.completedFuture(
 *         new UploadResult("/uploads/" + filename)
 *     );
 * });
 *
 * // Upload to cloud storage
 * editor.setUploadHandler((context, stream) -&gt; {
 *     return cloudStorage.uploadAsync(stream, context.getFileName())
 *         .thenApply(url -&gt; new UploadResult(url));
 * });
 * </pre>
 */
@FunctionalInterface
public interface UploadHandler {

    /**
     * Handle file upload
     *
     * @param context upload context information
     * @param inputStream file input stream
     * @return asynchronous upload result
     */
    CompletableFuture<UploadResult> handleUpload(UploadContext context, InputStream inputStream);

    /**
     * Upload context
     */
    class UploadContext {
        private final String fileName;
        private final String mimeType;
        private final long fileSize;

        /**
         * Create an upload context
         *
         * @param fileName file name
         * @param mimeType MIME type
         * @param fileSize file size in bytes
         */
        public UploadContext(String fileName, String mimeType, long fileSize) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }

        public String getFileName() {
            return fileName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getFileSize() {
            return fileSize;
        }

        /**
         * Check if the file is an image
         *
         * @return true if the file is an image
         */
        public boolean isImage() {
            return mimeType != null && mimeType.startsWith("image/");
        }
    }

    /**
     * Upload result
     */
    class UploadResult {
        private final String url;
        private final boolean success;
        private final String errorMessage;

        /**
         * Create a successful upload result
         *
         * @param url the accessible URL after upload
         */
        public UploadResult(String url) {
            this.url = url;
            this.success = true;
            this.errorMessage = null;
        }

        /**
         * Create a failed upload result
         *
         * @param errorMessage error message
         * @return failure result
         */
        public static UploadResult failure(String errorMessage) {
            return new UploadResult(null, false, errorMessage);
        }

        private UploadResult(String url, boolean success, String errorMessage) {
            this.url = url;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getUrl() {
            return url;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Upload configuration
     */
    class UploadConfig {
        /** Minimum allowed file size: 1 byte */
        public static final long MIN_FILE_SIZE = 1;
        /** Maximum allowed file size: 1GB */
        public static final long MAX_FILE_SIZE_LIMIT = 1024L * 1024 * 1024;
        /** Default maximum file size: 10MB */
        public static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

        private long maxFileSize = DEFAULT_MAX_FILE_SIZE;
        private java.util.Set<String> allowedMimeTypes = new java.util.LinkedHashSet<>(
            java.util.Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp")
        );

        /**
         * Get maximum file size
         *
         * @return maximum file size in bytes
         */
        public long getMaxFileSize() {
            return maxFileSize;
        }

        /**
         * Set maximum file size
         *
         * @param maxFileSize maximum file size in bytes, must be between 1 byte and 1GB
         * @return this
         * @throws IllegalArgumentException if maxFileSize is outside the valid range
         */
        public UploadConfig setMaxFileSize(long maxFileSize) {
            if (maxFileSize < MIN_FILE_SIZE || maxFileSize > MAX_FILE_SIZE_LIMIT) {
                throw new IllegalArgumentException(
                    String.format("maxFileSize must be between %d and %d bytes, got %d",
                        MIN_FILE_SIZE, MAX_FILE_SIZE_LIMIT, maxFileSize));
            }
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * Get allowed MIME types
         *
         * @return a copy of the MIME types array
         */
        public String[] getAllowedMimeTypes() {
            return allowedMimeTypes.toArray(new String[0]);
        }

        /**
         * Set allowed MIME types.
         * Setting to an empty array allows all MIME types.
         *
         * @param allowedMimeTypes MIME types array
         * @return this
         * @throws IllegalArgumentException if the array is null or contains null/empty strings
         */
        public UploadConfig setAllowedMimeTypes(String... allowedMimeTypes) {
            if (allowedMimeTypes == null) {
                throw new IllegalArgumentException("allowedMimeTypes cannot be null");
            }
            this.allowedMimeTypes = new java.util.LinkedHashSet<>();
            for (String mimeType : allowedMimeTypes) {
                if (mimeType == null || mimeType.trim().isEmpty()) {
                    throw new IllegalArgumentException("MIME type cannot be null or empty");
                }
                this.allowedMimeTypes.add(mimeType.trim());
            }
            return this;
        }

        /**
         * Add additional MIME types to the allowed list
         *
         * @param mimeTypes MIME types to add
         * @return this
         */
        public UploadConfig addAllowedMimeTypes(String... mimeTypes) {
            if (mimeTypes != null) {
                for (String mimeType : mimeTypes) {
                    if (mimeType != null && !mimeType.trim().isEmpty()) {
                        this.allowedMimeTypes.add(mimeType.trim());
                    }
                }
            }
            return this;
        }

        /**
         * Reset to the default MIME types list
         *
         * @return this
         */
        public UploadConfig resetAllowedMimeTypes() {
            this.allowedMimeTypes = new java.util.LinkedHashSet<>(
                java.util.Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp")
            );
            return this;
        }

        /**
         * Validate an upload against this configuration
         *
         * @param context upload context
         * @return error message if validation fails, null if validation passes
         */
        public String validate(UploadContext context) {
            if (context == null) {
                return "Upload context cannot be null";
            }

            if (context.getFileSize() > maxFileSize) {
                return String.format("File size %d exceeds maximum allowed %d bytes",
                    context.getFileSize(), maxFileSize);
            }

            // Empty allowed list means all types are permitted
            if (!allowedMimeTypes.isEmpty() && !allowedMimeTypes.contains(context.getMimeType())) {
                return String.format("MIME type '%s' is not allowed. Allowed types: %s",
                    context.getMimeType(), String.join(", ", allowedMimeTypes));
            }

            return null;
        }
    }
}
