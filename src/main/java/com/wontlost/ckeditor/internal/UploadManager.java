package com.wontlost.ckeditor.internal;

import com.wontlost.ckeditor.handler.UploadHandler;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal class for managing file uploads.
 * Handles upload queuing, progress tracking, and result callbacks with thread safety.
 *
 * <p>This class is an internal API and should not be used directly by external code.</p>
 */
public class UploadManager {

    private static final Logger logger = Logger.getLogger(UploadManager.class.getName());

    /**
     * Default upload timeout in seconds.
     * Backend timeout is set to 6 minutes, slightly longer than the frontend default of 5 minutes,
     * to avoid race conditions when both sides time out simultaneously.
     */
    private static final long DEFAULT_UPLOAD_TIMEOUT_SECONDS = 360; // 6 minutes

    /**
     * Upload task status
     */
    public enum UploadStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Upload task information
     */
    public static class UploadTask {
        private final String uploadId;
        private final String fileName;
        private final String mimeType;
        private final long fileSize;
        private final long startTime;
        private volatile UploadStatus status;
        private volatile String resultUrl;
        private volatile String errorMessage;
        /** Flag indicating whether the callback has been notified, to prevent double notification */
        private volatile boolean notified;

        UploadTask(String uploadId, String fileName, String mimeType, long fileSize) {
            this.uploadId = uploadId;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.startTime = System.currentTimeMillis();
            this.status = UploadStatus.PENDING;
            this.notified = false;
        }

        public String getUploadId() { return uploadId; }
        public String getFileName() { return fileName; }
        public String getMimeType() { return mimeType; }
        public long getFileSize() { return fileSize; }
        public long getStartTime() { return startTime; }
        public UploadStatus getStatus() { return status; }
        public String getResultUrl() { return resultUrl; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isNotified() { return notified; }

        private volatile CompletableFuture<?> future;

        void setStatus(UploadStatus status) { this.status = status; }
        void setResultUrl(String url) { this.resultUrl = url; }
        void setErrorMessage(String error) { this.errorMessage = error; }
        void setNotified(boolean notified) { this.notified = notified; }
        void setFuture(CompletableFuture<?> future) { this.future = future; }
        CompletableFuture<?> getFuture() { return future; }

        /**
         * Get the elapsed upload time in milliseconds
         */
        public long getElapsedTimeMs() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Upload result callback interface
     */
    @FunctionalInterface
    public interface UploadResultCallback {
        /**
         * Callback when upload completes
         *
         * @param uploadId upload ID
         * @param url URL on success, null on failure
         * @param error error message on failure, null on success
         */
        void onComplete(String uploadId, String url, String error);
    }

    private final UploadHandler uploadHandler;
    private final UploadHandler.UploadConfig uploadConfig;
    private final UploadResultCallback resultCallback;
    private final Map<String, UploadTask> activeTasks = new ConcurrentHashMap<>();
    private final long uploadTimeoutSeconds;

    /**
     * Create an upload manager with default timeout
     *
     * @param uploadHandler upload handler
     * @param uploadConfig upload configuration, uses default if null
     * @param resultCallback result callback
     */
    public UploadManager(UploadHandler uploadHandler,
                        UploadHandler.UploadConfig uploadConfig,
                        UploadResultCallback resultCallback) {
        this(uploadHandler, uploadConfig, resultCallback, DEFAULT_UPLOAD_TIMEOUT_SECONDS);
    }

    /**
     * Create an upload manager with custom timeout
     *
     * @param uploadHandler upload handler
     * @param uploadConfig upload configuration, uses default if null
     * @param resultCallback result callback
     * @param uploadTimeoutSeconds upload timeout in seconds, 0 means no timeout
     */
    public UploadManager(UploadHandler uploadHandler,
                        UploadHandler.UploadConfig uploadConfig,
                        UploadResultCallback resultCallback,
                        long uploadTimeoutSeconds) {
        this.uploadHandler = uploadHandler;
        this.uploadConfig = uploadConfig != null ? uploadConfig : new UploadHandler.UploadConfig();
        this.resultCallback = resultCallback;
        this.uploadTimeoutSeconds = uploadTimeoutSeconds > 0 ? uploadTimeoutSeconds : 0;
    }

    /**
     * Handle a file upload request
     *
     * @param uploadId upload identifier
     * @param fileName file name
     * @param mimeType MIME type
     * @param base64Data Base64-encoded file content
     */
    public void handleUpload(String uploadId, String fileName, String mimeType, String base64Data) {
        logger.log(Level.FINE, "Starting upload: id={0}, file={1}, type={2}, dataLength={3}",
            new Object[]{uploadId, fileName, mimeType, base64Data != null ? base64Data.length() : 0});

        if (uploadHandler == null) {
            logger.log(Level.WARNING, "Upload {0} rejected: no upload handler configured", uploadId);
            notifyError(uploadId, null, "No upload handler configured");
            return;
        }

        byte[] fileData;
        try {
            fileData = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Upload {0} rejected: invalid base64 data - {1}",
                new Object[]{uploadId, e.getMessage()});
            notifyError(uploadId, null, "Invalid file data: " + e.getMessage());
            return;
        }

        long fileSize = fileData.length;
        logger.log(Level.FINE, "Upload {0} decoded: {1} bytes", new Object[]{uploadId, fileSize});

        UploadHandler.UploadContext context = new UploadHandler.UploadContext(fileName, mimeType, fileSize);

        // Validate upload
        String validationError = uploadConfig.validate(context);
        if (validationError != null) {
            logger.log(Level.INFO, "Upload {0} rejected by validation: {1}",
                new Object[]{uploadId, validationError});
            notifyError(uploadId, null, validationError);
            return;
        }

        // Create and track upload task
        UploadTask task = new UploadTask(uploadId, fileName, mimeType, fileSize);
        activeTasks.put(uploadId, task);
        task.setStatus(UploadStatus.IN_PROGRESS);
        logger.log(Level.FINE, "Upload {0} task created and tracking started", uploadId);

        // Process upload asynchronously (catch synchronous exceptions and null return values)
        CompletableFuture<UploadHandler.UploadResult> future;
        try {
            logger.log(Level.FINE, "Upload {0} invoking handler", uploadId);
            future = uploadHandler.handleUpload(context, new ByteArrayInputStream(fileData));
            if (future == null) {
                task.setStatus(UploadStatus.FAILED);
                task.setErrorMessage("Upload handler returned null");
                activeTasks.remove(uploadId);
                logger.log(Level.WARNING, "Upload {0} failed: handler returned null future", uploadId);
                notifyError(uploadId, task, "Upload handler returned null");
                return;
            }
        } catch (Exception e) {
            task.setStatus(UploadStatus.FAILED);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName() + " occurred during upload initialization";
            }
            task.setErrorMessage(errorMsg);
            activeTasks.remove(uploadId);
            logger.log(Level.WARNING, "Upload {0} failed: handler threw {1} - {2}",
                new Object[]{uploadId, e.getClass().getSimpleName(), errorMsg});
            notifyError(uploadId, task, errorMsg);
            return;
        }

        // Apply timeout if configured
        CompletableFuture<UploadHandler.UploadResult> timedFuture;
        if (uploadTimeoutSeconds > 0) {
            timedFuture = future.orTimeout(uploadTimeoutSeconds, TimeUnit.SECONDS);
        } else {
            timedFuture = future;
        }

        // Save Future reference to support cancellation
        task.setFuture(timedFuture);

        // Use handle instead of thenAccept + exceptionally to ensure a single processing path
        timedFuture.handle((result, ex) -> {
            // Synchronize task state updates
            synchronized (task) {
                // Double notification guard: if already notified or cancelled, return immediately
                if (task.isNotified()) {
                    logger.log(Level.FINE, "Upload {0} already notified, skipping duplicate callback", uploadId);
                    activeTasks.remove(uploadId);
                    return null;
                }

                if (task.getStatus() == UploadStatus.CANCELLED) {
                    // Task was cancelled, ignore result
                    logger.log(Level.FINE, "Upload {0} was cancelled, ignoring result", uploadId);
                    activeTasks.remove(uploadId);
                    return null;
                }

                long elapsedMs = task.getElapsedTimeMs();

                if (ex != null) {
                    // Exception handling - unwrap CompletionException to get root cause
                    Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                        ? ex.getCause() : ex;
                    task.setStatus(UploadStatus.FAILED);
                    String errorMsg;
                    if (cause instanceof TimeoutException) {
                        errorMsg = "Upload timed out after " + uploadTimeoutSeconds + " seconds";
                        logger.log(Level.WARNING, "Upload {0} timed out after {1}ms", new Object[]{uploadId, elapsedMs});
                    } else {
                        errorMsg = cause.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = cause.getClass().getSimpleName() + " occurred during upload";
                        }
                        logger.log(Level.WARNING, "Upload {0} failed after {1}ms: {2}",
                            new Object[]{uploadId, elapsedMs, errorMsg});
                    }
                    task.setErrorMessage(errorMsg);
                    notifyResult(uploadId, task, null, errorMsg);
                } else if (result != null && result.isSuccess()) {
                    // Success
                    task.setStatus(UploadStatus.COMPLETED);
                    task.setResultUrl(result.getUrl());
                    logger.log(Level.INFO, "Upload {0} completed successfully in {1}ms, url={2}",
                        new Object[]{uploadId, elapsedMs, result.getUrl()});
                    notifyResult(uploadId, task, result.getUrl(), null);
                } else {
                    // Failure
                    task.setStatus(UploadStatus.FAILED);
                    String errorMsg = result != null ? result.getErrorMessage() : "Unknown upload error";
                    task.setErrorMessage(errorMsg);
                    logger.log(Level.WARNING, "Upload {0} failed after {1}ms: {2}",
                        new Object[]{uploadId, elapsedMs, errorMsg});
                    notifyResult(uploadId, task, null, errorMsg);
                }

                // Clean up completed task
                activeTasks.remove(uploadId);
            }
            return null;
        });
    }

    /**
     * Cancel an upload task
     *
     * @param uploadId upload ID
     * @return whether the cancellation succeeded
     */
    public boolean cancelUpload(String uploadId) {
        UploadTask task = activeTasks.get(uploadId);
        if (task == null) {
            logger.log(Level.FINE, "Cancel request for unknown upload: {0}", uploadId);
            return false;
        }

        synchronized (task) {
            // Double notification guard: if already notified, do not cancel
            if (task.isNotified()) {
                logger.log(Level.FINE, "Upload {0} already notified, cancel ignored", uploadId);
                return false;
            }

            if (task.getStatus() == UploadStatus.IN_PROGRESS ||
                task.getStatus() == UploadStatus.PENDING) {
                task.setStatus(UploadStatus.CANCELLED);
                task.setErrorMessage("Upload cancelled");

                // Attempt to cancel the underlying Future
                CompletableFuture<?> future = task.getFuture();
                if (future != null) {
                    future.cancel(true);
                }

                logger.log(Level.FINE, "Upload {0} cancelled after {1}ms",
                    new Object[]{uploadId, task.getElapsedTimeMs()});
                notifyResult(uploadId, task, null, "Upload cancelled");
                activeTasks.remove(uploadId);
                return true;
            }

            logger.log(Level.FINE, "Upload {0} cannot be cancelled in status {1}",
                new Object[]{uploadId, task.getStatus()});
        }
        return false;
    }

    /**
     * Get the number of active uploads
     *
     * @return active upload task count
     */
    public int getActiveUploadCount() {
        return activeTasks.size();
    }

    /**
     * Check whether there are any active uploads
     *
     * @return true if there are active uploads
     */
    public boolean hasActiveUploads() {
        return !activeTasks.isEmpty();
    }

    /**
     * Get upload task status
     *
     * @param uploadId upload ID
     * @return upload task, or null if not found
     */
    public UploadTask getUploadTask(String uploadId) {
        return activeTasks.get(uploadId);
    }

    /**
     * Clean up all pending upload tasks
     */
    public void cleanup() {
        // Iterate over a snapshot to avoid ConcurrentModificationException,
        // since cancelUpload() removes entries from activeTasks
        for (String uploadId : new java.util.ArrayList<>(activeTasks.keySet())) {
            cancelUpload(uploadId);
        }
        activeTasks.clear();
    }

    /**
     * Notify upload error (for early failures where the task may not exist)
     */
    private void notifyError(String uploadId, UploadTask task, String error) {
        logger.log(Level.WARNING, "Upload failed for {0}: {1}", new Object[]{uploadId, error});
        notifyResult(uploadId, task, null, error);
    }

    /**
     * Notify upload result with double notification guard
     */
    private void notifyResult(String uploadId, UploadTask task, String url, String error) {
        // Double notification guard
        if (task != null) {
            synchronized (task) {
                if (task.isNotified()) {
                    logger.log(Level.FINE, "Skipping duplicate notification for upload {0}", uploadId);
                    return;
                }
                task.setNotified(true);
            }
        }

        if (resultCallback != null) {
            try {
                resultCallback.onComplete(uploadId, url, error);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in upload result callback for " + uploadId, e);
            }
        }
    }
}
