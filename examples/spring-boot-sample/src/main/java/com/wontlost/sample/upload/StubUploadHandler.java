package com.wontlost.sample.upload;

import com.wontlost.ckeditor.handler.UploadHandler;

import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Stub upload handler for Playwright fixtures.
 *
 * <p>Reads the stream into memory (small test files only) and returns a
 * {@code data:} URL so the editor can render the uploaded image without a
 * real backend file store.</p>
 */
public class StubUploadHandler implements UploadHandler {

    private static final int MAX_BYTES = 1_000_000;

    @Override
    public CompletableFuture<UploadResult> handleUpload(UploadContext context, InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] data = inputStream.readNBytes(MAX_BYTES);
                String mime = context.getMimeType() != null ? context.getMimeType() : "application/octet-stream";
                String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(data);
                return new UploadResult(dataUrl);
            } catch (Exception e) {
                return UploadResult.failure("Stub upload failed: " + e.getMessage());
            }
        });
    }
}
