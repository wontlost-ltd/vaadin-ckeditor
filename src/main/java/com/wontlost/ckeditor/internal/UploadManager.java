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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 管理文件上传的内部类。
 * 处理上传队列、进度跟踪和结果回调，确保线程安全。
 *
 * <p>此类是内部 API，不应直接由外部代码使用。</p>
 */
public class UploadManager {

    private static final Logger logger = Logger.getLogger(UploadManager.class.getName());

    /**
     * 默认上传超时时间（秒）。
     * 后端超时设为 6 分钟，略长于前端默认的 5 分钟，
     * 避免前后端同时超时导致的竞争条件。
     */
    private static final long DEFAULT_UPLOAD_TIMEOUT_SECONDS = 360; // 6 minutes

    /**
     * 上传任务状态
     */
    public enum UploadStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * 上传任务信息
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
        /** 标记是否已通知回调，防止双重通知 */
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
         * 获取上传耗时（毫秒）
         */
        public long getElapsedTimeMs() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 上传结果回调接口
     */
    @FunctionalInterface
    public interface UploadResultCallback {
        /**
         * 上传完成时的回调
         *
         * @param uploadId 上传 ID
         * @param url 成功时的 URL，失败时为 null
         * @param error 失败时的错误消息，成功时为 null
         */
        void onComplete(String uploadId, String url, String error);
    }

    private final UploadHandler uploadHandler;
    private final UploadHandler.UploadConfig uploadConfig;
    private final UploadResultCallback resultCallback;
    private final Map<String, UploadTask> activeTasks = new ConcurrentHashMap<>();
    private final AtomicLong uploadCounter = new AtomicLong(0);
    private final long uploadTimeoutSeconds;

    /**
     * 创建上传管理器（使用默认超时）
     *
     * @param uploadHandler 上传处理器
     * @param uploadConfig 上传配置，为 null 时使用默认配置
     * @param resultCallback 结果回调
     */
    public UploadManager(UploadHandler uploadHandler,
                        UploadHandler.UploadConfig uploadConfig,
                        UploadResultCallback resultCallback) {
        this(uploadHandler, uploadConfig, resultCallback, DEFAULT_UPLOAD_TIMEOUT_SECONDS);
    }

    /**
     * 创建上传管理器（自定义超时）
     *
     * @param uploadHandler 上传处理器
     * @param uploadConfig 上传配置，为 null 时使用默认配置
     * @param resultCallback 结果回调
     * @param uploadTimeoutSeconds 上传超时时间（秒），0 表示无超时
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
     * 处理文件上传请求
     *
     * @param uploadId 上传标识符
     * @param fileName 文件名
     * @param mimeType MIME 类型
     * @param base64Data Base64 编码的文件内容
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

        // 验证上传
        String validationError = uploadConfig.validate(context);
        if (validationError != null) {
            logger.log(Level.INFO, "Upload {0} rejected by validation: {1}",
                new Object[]{uploadId, validationError});
            notifyError(uploadId, null, validationError);
            return;
        }

        // 创建并跟踪上传任务
        UploadTask task = new UploadTask(uploadId, fileName, mimeType, fileSize);
        activeTasks.put(uploadId, task);
        task.setStatus(UploadStatus.IN_PROGRESS);
        logger.log(Level.FINE, "Upload {0} task created and tracking started", uploadId);

        // 异步处理上传（捕获同步异常和 null 返回值）
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

        // 应用超时机制（如果配置了超时）
        CompletableFuture<UploadHandler.UploadResult> timedFuture;
        if (uploadTimeoutSeconds > 0) {
            timedFuture = future.orTimeout(uploadTimeoutSeconds, TimeUnit.SECONDS);
        } else {
            timedFuture = future;
        }

        // 保存 Future 引用以支持取消
        task.setFuture(timedFuture);

        // 使用 handle 而不是 thenAccept + exceptionally，确保单一处理路径
        timedFuture.handle((result, ex) -> {
            // 同步更新任务状态
            synchronized (task) {
                // 双重通知防护：如果已通知或已取消，直接返回
                if (task.isNotified()) {
                    logger.log(Level.FINE, "Upload {0} already notified, skipping duplicate callback", uploadId);
                    activeTasks.remove(uploadId);
                    return null;
                }

                if (task.getStatus() == UploadStatus.CANCELLED) {
                    // 任务已取消，忽略结果
                    logger.log(Level.FINE, "Upload {0} was cancelled, ignoring result", uploadId);
                    activeTasks.remove(uploadId);
                    return null;
                }

                long elapsedMs = task.getElapsedTimeMs();

                if (ex != null) {
                    // 异常处理 - 解包 CompletionException 获取根本原因
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
                    // 成功
                    task.setStatus(UploadStatus.COMPLETED);
                    task.setResultUrl(result.getUrl());
                    logger.log(Level.INFO, "Upload {0} completed successfully in {1}ms, url={2}",
                        new Object[]{uploadId, elapsedMs, result.getUrl()});
                    notifyResult(uploadId, task, result.getUrl(), null);
                } else {
                    // 失败
                    task.setStatus(UploadStatus.FAILED);
                    String errorMsg = result != null ? result.getErrorMessage() : "Unknown upload error";
                    task.setErrorMessage(errorMsg);
                    logger.log(Level.WARNING, "Upload {0} failed after {1}ms: {2}",
                        new Object[]{uploadId, elapsedMs, errorMsg});
                    notifyResult(uploadId, task, null, errorMsg);
                }

                // 清理完成的任务
                activeTasks.remove(uploadId);
            }
            return null;
        });
    }

    /**
     * 取消上传任务
     *
     * @param uploadId 上传 ID
     * @return 是否成功取消
     */
    public boolean cancelUpload(String uploadId) {
        UploadTask task = activeTasks.get(uploadId);
        if (task == null) {
            logger.log(Level.FINE, "Cancel request for unknown upload: {0}", uploadId);
            return false;
        }

        synchronized (task) {
            // 双重通知防护：如果已通知，不再取消
            if (task.isNotified()) {
                logger.log(Level.FINE, "Upload {0} already notified, cancel ignored", uploadId);
                return false;
            }

            if (task.getStatus() == UploadStatus.IN_PROGRESS ||
                task.getStatus() == UploadStatus.PENDING) {
                task.setStatus(UploadStatus.CANCELLED);
                task.setErrorMessage("Upload cancelled");

                // 尝试取消底层的 Future
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
     * 获取活跃上传数量
     *
     * @return 活跃上传任务数
     */
    public int getActiveUploadCount() {
        return activeTasks.size();
    }

    /**
     * 检查是否有活跃上传
     *
     * @return 是否有活跃上传
     */
    public boolean hasActiveUploads() {
        return !activeTasks.isEmpty();
    }

    /**
     * 获取上传任务状态
     *
     * @param uploadId 上传 ID
     * @return 上传任务，不存在时返回 null
     */
    public UploadTask getUploadTask(String uploadId) {
        return activeTasks.get(uploadId);
    }

    /**
     * 清理所有待处理的上传任务
     */
    public void cleanup() {
        for (String uploadId : activeTasks.keySet()) {
            cancelUpload(uploadId);
        }
        activeTasks.clear();
    }

    /**
     * 通知上传错误（用于早期失败，任务可能不存在）
     */
    private void notifyError(String uploadId, UploadTask task, String error) {
        logger.log(Level.WARNING, "Upload failed for {0}: {1}", new Object[]{uploadId, error});
        notifyResult(uploadId, task, null, error);
    }

    /**
     * 通知上传结果，带双重通知防护
     */
    private void notifyResult(String uploadId, UploadTask task, String url, String error) {
        // 双重通知防护
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
