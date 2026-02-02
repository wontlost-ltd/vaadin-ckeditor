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
     * 默认上传超时时间（秒）
     */
    private static final long DEFAULT_UPLOAD_TIMEOUT_SECONDS = 300; // 5 minutes

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

        UploadTask(String uploadId, String fileName, String mimeType, long fileSize) {
            this.uploadId = uploadId;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
            this.startTime = System.currentTimeMillis();
            this.status = UploadStatus.PENDING;
        }

        public String getUploadId() { return uploadId; }
        public String getFileName() { return fileName; }
        public String getMimeType() { return mimeType; }
        public long getFileSize() { return fileSize; }
        public long getStartTime() { return startTime; }
        public UploadStatus getStatus() { return status; }
        public String getResultUrl() { return resultUrl; }
        public String getErrorMessage() { return errorMessage; }

        private volatile CompletableFuture<?> future;

        void setStatus(UploadStatus status) { this.status = status; }
        void setResultUrl(String url) { this.resultUrl = url; }
        void setErrorMessage(String error) { this.errorMessage = error; }
        void setFuture(CompletableFuture<?> future) { this.future = future; }
        CompletableFuture<?> getFuture() { return future; }
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
        if (uploadHandler == null) {
            notifyError(uploadId, "No upload handler configured");
            return;
        }

        byte[] fileData;
        try {
            fileData = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            notifyError(uploadId, "Invalid file data: " + e.getMessage());
            return;
        }

        long fileSize = fileData.length;
        UploadHandler.UploadContext context = new UploadHandler.UploadContext(fileName, mimeType, fileSize);

        // 验证上传
        String validationError = uploadConfig.validate(context);
        if (validationError != null) {
            notifyError(uploadId, validationError);
            return;
        }

        // 创建并跟踪上传任务
        UploadTask task = new UploadTask(uploadId, fileName, mimeType, fileSize);
        activeTasks.put(uploadId, task);
        task.setStatus(UploadStatus.IN_PROGRESS);

        // 异步处理上传
        CompletableFuture<UploadHandler.UploadResult> future = uploadHandler.handleUpload(
            context,
            new ByteArrayInputStream(fileData)
        );

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
                if (task.getStatus() == UploadStatus.CANCELLED) {
                    // 任务已取消，忽略结果
                    activeTasks.remove(uploadId);
                    return null;
                }

                if (ex != null) {
                    // 异常处理 - 解包 CompletionException 获取根本原因
                    Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                        ? ex.getCause() : ex;
                    task.setStatus(UploadStatus.FAILED);
                    String errorMsg;
                    if (cause instanceof TimeoutException) {
                        errorMsg = "Upload timed out after " + uploadTimeoutSeconds + " seconds";
                        logger.log(Level.WARNING, "Upload {0} timed out", uploadId);
                    } else {
                        errorMsg = cause.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = cause.getClass().getSimpleName() + " occurred during upload";
                        }
                    }
                    task.setErrorMessage(errorMsg);
                    notifyResult(uploadId, null, errorMsg);
                } else if (result != null && result.isSuccess()) {
                    // 成功
                    task.setStatus(UploadStatus.COMPLETED);
                    task.setResultUrl(result.getUrl());
                    notifyResult(uploadId, result.getUrl(), null);
                } else {
                    // 失败
                    task.setStatus(UploadStatus.FAILED);
                    String errorMsg = result != null ? result.getErrorMessage() : "Unknown upload error";
                    task.setErrorMessage(errorMsg);
                    notifyResult(uploadId, null, errorMsg);
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
        if (task != null) {
            synchronized (task) {
                if (task.getStatus() == UploadStatus.IN_PROGRESS ||
                    task.getStatus() == UploadStatus.PENDING) {
                    task.setStatus(UploadStatus.CANCELLED);
                    task.setErrorMessage("Upload cancelled");

                    // 尝试取消底层的 Future
                    CompletableFuture<?> future = task.getFuture();
                    if (future != null) {
                        future.cancel(true);
                    }

                    notifyResult(uploadId, null, "Upload cancelled");
                    activeTasks.remove(uploadId);
                    return true;
                }
            }
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

    private void notifyError(String uploadId, String error) {
        logger.log(Level.WARNING, "Upload failed for {0}: {1}", new Object[]{uploadId, error});
        notifyResult(uploadId, null, error);
    }

    private void notifyResult(String uploadId, String url, String error) {
        if (resultCallback != null) {
            try {
                resultCallback.onComplete(uploadId, url, error);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in upload result callback", e);
            }
        }
    }
}
