package com.wontlost.ckeditor;

import com.wontlost.ckeditor.handler.UploadHandler;
import com.wontlost.ckeditor.internal.UploadManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UploadManager
 */
class UploadManagerTest {

    private UploadManager manager;
    private AtomicReference<String> lastUploadId;
    private AtomicReference<String> lastUrl;
    private AtomicReference<String> lastError;
    private CountDownLatch latch;

    @BeforeEach
    void setUp() {
        lastUploadId = new AtomicReference<>();
        lastUrl = new AtomicReference<>();
        lastError = new AtomicReference<>();
        latch = new CountDownLatch(1);
    }

    private UploadManager.UploadResultCallback createCallback() {
        return (uploadId, url, error) -> {
            lastUploadId.set(uploadId);
            lastUrl.set(url);
            lastError.set(error);
            latch.countDown();
        };
    }

    private UploadHandler createSuccessHandler(String resultUrl) {
        return (context, stream) -> CompletableFuture.completedFuture(
            new UploadHandler.UploadResult(resultUrl)
        );
    }

    private UploadHandler createFailureHandler(String errorMessage) {
        return (context, stream) -> CompletableFuture.completedFuture(
            UploadHandler.UploadResult.failure(errorMessage)
        );
    }

    private UploadHandler createExceptionHandler() {
        return (context, stream) -> {
            CompletableFuture<UploadHandler.UploadResult> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Upload exception"));
            return future;
        };
    }

    private String createBase64Data(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes());
    }

    @Test
    @DisplayName("handleUpload should succeed with valid data")
    void handleUploadSucceeds() throws Exception {
        manager = new UploadManager(
            createSuccessHandler("https://example.com/image.jpg"),
            null,
            createCallback()
        );

        manager.handleUpload("upload-1", "test.jpg", "image/jpeg", createBase64Data("test data"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("upload-1", lastUploadId.get());
        assertEquals("https://example.com/image.jpg", lastUrl.get());
        assertNull(lastError.get());
    }

    @Test
    @DisplayName("handleUpload should report failure from handler")
    void handleUploadReportsFailure() throws Exception {
        manager = new UploadManager(
            createFailureHandler("Storage full"),
            null,
            createCallback()
        );

        manager.handleUpload("upload-2", "test.jpg", "image/jpeg", createBase64Data("test data"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("upload-2", lastUploadId.get());
        assertNull(lastUrl.get());
        assertEquals("Storage full", lastError.get());
    }

    @Test
    @DisplayName("handleUpload should report exception from handler")
    void handleUploadReportsException() throws Exception {
        manager = new UploadManager(
            createExceptionHandler(),
            null,
            createCallback()
        );

        manager.handleUpload("upload-3", "test.jpg", "image/jpeg", createBase64Data("test data"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("upload-3", lastUploadId.get());
        assertNull(lastUrl.get());
        assertNotNull(lastError.get());
        assertTrue(lastError.get().contains("exception") || lastError.get().contains("RuntimeException"));
    }

    @Test
    @DisplayName("handleUpload should fail without handler")
    void handleUploadFailsWithoutHandler() throws Exception {
        manager = new UploadManager(null, null, createCallback());

        manager.handleUpload("upload-4", "test.jpg", "image/jpeg", createBase64Data("test data"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("upload-4", lastUploadId.get());
        assertNull(lastUrl.get());
        assertTrue(lastError.get().contains("No upload handler"));
    }

    @Test
    @DisplayName("handleUpload should fail with invalid base64")
    void handleUploadFailsWithInvalidBase64() throws Exception {
        manager = new UploadManager(
            createSuccessHandler("https://example.com/image.jpg"),
            null,
            createCallback()
        );

        manager.handleUpload("upload-5", "test.jpg", "image/jpeg", "not-valid-base64!!!");

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("upload-5", lastUploadId.get());
        assertNull(lastUrl.get());
        assertTrue(lastError.get().contains("Invalid file data"));
    }

    @Test
    @DisplayName("handleUpload should validate MIME type")
    void handleUploadValidatesMimeType() throws Exception {
        manager = new UploadManager(
            createSuccessHandler("https://example.com/file.exe"),
            new UploadHandler.UploadConfig().setAllowedMimeTypes("image/jpeg", "image/png"),
            createCallback()
        );

        manager.handleUpload("upload-6", "test.exe", "application/x-executable", createBase64Data("binary"));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("upload-6", lastUploadId.get());
        assertNull(lastUrl.get());
        assertTrue(lastError.get().contains("not allowed"));
    }

    @Test
    @DisplayName("hasActiveUploads should return false initially")
    void hasActiveUploadsReturnsFalseInitially() {
        manager = new UploadManager(createSuccessHandler("url"), null, (id, url, err) -> {});
        assertFalse(manager.hasActiveUploads());
    }

    @Test
    @DisplayName("getActiveUploadCount should return 0 initially")
    void getActiveUploadCountReturnsZeroInitially() {
        manager = new UploadManager(createSuccessHandler("url"), null, (id, url, err) -> {});
        assertEquals(0, manager.getActiveUploadCount());
    }

    @Test
    @DisplayName("cancelUpload should cancel pending upload")
    void cancelUploadCancelsPending() throws Exception {
        // Use delayed handler
        UploadHandler delayedHandler = (context, stream) -> {
            CompletableFuture<UploadHandler.UploadResult> future = new CompletableFuture<>();
            // 不立即完成，模拟长时间上传
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    future.complete(new UploadHandler.UploadResult("url"));
                } catch (InterruptedException e) {
                    future.completeExceptionally(e);
                }
            }).start();
            return future;
        };

        manager = new UploadManager(delayedHandler, null, createCallback());
        manager.handleUpload("upload-7", "test.jpg", "image/jpeg", createBase64Data("test"));

        // 等待一小段时间让上传开始
        Thread.sleep(100);

        // 取消上传
        boolean cancelled = manager.cancelUpload("upload-7");
        assertTrue(cancelled);

        // 等待回调
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals("upload-7", lastUploadId.get());
        assertNull(lastUrl.get());
        assertEquals("Upload cancelled", lastError.get());
    }

    @Test
    @DisplayName("cancelUpload should return false for non-existent upload")
    void cancelUploadReturnsFalseForNonExistent() {
        manager = new UploadManager(createSuccessHandler("url"), null, (id, url, err) -> {});
        assertFalse(manager.cancelUpload("non-existent"));
    }

    @Test
    @DisplayName("cleanup should cancel all active uploads")
    void cleanupCancelsAllUploads() {
        manager = new UploadManager(createSuccessHandler("url"), null, (id, url, err) -> {});
        // Calling cleanup with no active uploads should not throw exception
        assertDoesNotThrow(() -> manager.cleanup());
    }

    @Test
    @DisplayName("getUploadTask should return null for non-existent upload")
    void getUploadTaskReturnsNullForNonExistent() {
        manager = new UploadManager(createSuccessHandler("url"), null, (id, url, err) -> {});
        assertNull(manager.getUploadTask("non-existent"));
    }
}
