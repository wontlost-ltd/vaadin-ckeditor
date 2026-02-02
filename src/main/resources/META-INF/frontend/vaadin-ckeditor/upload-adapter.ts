/**
 * Upload Adapter Module for VaadinCKEditor
 *
 * Provides a custom CKEditor upload adapter that sends files to the Vaadin backend
 * via @ClientCallable methods, enabling server-side file handling through UploadHandler.
 */

/**
 * Upload promise resolver interface
 */
export interface UploadResolver {
    resolve: (url: string) => void;
    reject: (error: Error) => void;
}

/**
 * Server communication interface for upload operations.
 * Matches the @ClientCallable methods in VaadinCKEditor.java.
 */
export interface UploadServer {
    /**
     * Send file upload data to server.
     * Server will process via UploadHandler and call _resolveUpload with result.
     */
    handleFileUpload(uploadId: string, fileName: string, mimeType: string, base64Data: string): void;

    /**
     * Cancel an in-progress upload on the server side.
     * Optional - may not be implemented in older versions.
     */
    cancelUploadFromClient?(uploadId: string): void;
}

/**
 * CKEditor file loader interface
 */
export interface FileLoader {
    file: Promise<File>;
}

/**
 * CKEditor upload adapter interface
 */
export interface UploadAdapter {
    upload: () => Promise<{ default: string }>;
    abort: () => void;
}

/**
 * Logger interface for debugging
 */
interface Logger {
    debug: (...args: unknown[]) => void;
    warn: (...args: unknown[]) => void;
}

/**
 * Default allowed MIME types for image uploads.
 * These are the standard image formats supported by browsers.
 */
const DEFAULT_ALLOWED_MIME_TYPES = new Set([
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'image/svg+xml',
    'image/bmp',
    'image/tiff',
]);

/**
 * Upload Adapter Manager class.
 * Manages file uploads from CKEditor to the Vaadin backend.
 */
/**
 * Default upload timeout in milliseconds (5 minutes).
 * Prevents uploads from hanging indefinitely if server doesn't respond.
 */
const DEFAULT_UPLOAD_TIMEOUT_MS = 5 * 60 * 1000;

export class UploadAdapterManager {
    private pendingUploads: Map<string, UploadResolver> = new Map();
    private uploadIdCounter = 0;
    private editorId: string;
    private server: UploadServer | undefined;
    private logger: Logger;
    private maxFileSize: number = 10 * 1024 * 1024; // 默认 10MB
    private allowedMimeTypes: Set<string> = new Set(DEFAULT_ALLOWED_MIME_TYPES);
    private uploadTimeoutMs: number = DEFAULT_UPLOAD_TIMEOUT_MS;

    constructor(editorId: string, logger: Logger) {
        this.editorId = editorId;
        this.logger = logger;
    }

    /**
     * Set the upload timeout in milliseconds.
     * @param timeoutMs - Timeout in milliseconds (0 to disable)
     */
    setUploadTimeout(timeoutMs: number): void {
        this.uploadTimeoutMs = timeoutMs;
    }

    /**
     * Set the maximum allowed file size for uploads.
     * @param bytes - Maximum file size in bytes
     */
    setMaxFileSize(bytes: number): void {
        this.maxFileSize = bytes;
    }

    /**
     * Set the allowed MIME types for uploads.
     * @param mimeTypes - Array of allowed MIME type strings
     */
    setAllowedMimeTypes(mimeTypes: string[]): void {
        this.allowedMimeTypes = new Set(mimeTypes);
    }

    /**
     * Add additional MIME types to the allowed list.
     * @param mimeTypes - Array of MIME type strings to add
     */
    addAllowedMimeTypes(mimeTypes: string[]): void {
        for (const type of mimeTypes) {
            this.allowedMimeTypes.add(type);
        }
    }

    /**
     * Check if a MIME type is allowed for upload.
     * @param mimeType - The MIME type to check
     * @returns true if allowed, false otherwise
     */
    isMimeTypeAllowed(mimeType: string): boolean {
        // Allow all types if whitelist is empty (disabled)
        if (this.allowedMimeTypes.size === 0) {
            return true;
        }
        return this.allowedMimeTypes.has(mimeType);
    }

    /**
     * Set the server reference for upload operations.
     */
    setServer(server: UploadServer | undefined): void {
        this.server = server;
    }

    /**
     * Create an upload adapter factory for CKEditor configuration.
     * Returns a function that creates upload adapters for each file upload.
     */
    createUploadAdapterFactory(): (loader: FileLoader) => UploadAdapter {
        const manager = this;

        return (loader: FileLoader): UploadAdapter => {
            // 在 adapter 级别跟踪上传状态，以便 abort 可以访问
            // 使用对象包装以便在闭包中安全地共享和更新
            // isUploading 标志防止在 fileToBase64 期间的竞态条件
            const uploadState = {
                currentId: null as string | null,
                isUploading: false
            };

            return {
                upload: async (): Promise<{ default: string }> => {
                    // 防止同一 adapter 实例的并发上传
                    if (uploadState.isUploading) {
                        throw new Error('Upload already in progress for this adapter');
                    }
                    uploadState.isUploading = true;

                    try {
                        const file = await loader.file;
                        if (!file) {
                            throw new Error('No file provided');
                        }

                        // 空文件校验
                        if (file.size === 0) {
                            manager.logger.debug('Empty file rejected', { fileName: file.name });
                            throw new Error('Cannot upload empty file');
                        }

                        // 文件大小校验
                        if (file.size > manager.maxFileSize) {
                            throw new Error(`File size ${file.size} exceeds maximum allowed ${manager.maxFileSize} bytes`);
                        }

                        // MIME 类型白名单校验
                        if (!manager.isMimeTypeAllowed(file.type)) {
                            throw new Error(`File type '${file.type}' is not allowed. Allowed types: ${Array.from(manager.allowedMimeTypes).join(', ')}`);
                        }

                        // 生成唯一上传 ID 并立即捕获到本地变量
                        const uploadId = `upload-${manager.editorId}-${++manager.uploadIdCounter}`;
                        uploadState.currentId = uploadId;

                        // fileToBase64 是异步操作，但 isUploading 标志确保不会有竞态
                        const base64Data = await manager.fileToBase64(file);

                        const uploadPromise = new Promise<string>((resolve, reject) => {
                            manager.pendingUploads.set(uploadId, { resolve, reject });
                        });

                        if (manager.server) {
                            manager.server.handleFileUpload(uploadId, file.name, file.type, base64Data);
                        } else {
                            manager.pendingUploads.delete(uploadId);
                            uploadState.currentId = null;
                            throw new Error('Server connection not available');
                        }

                        // 应用超时机制
                        const url = await manager.withTimeout(uploadPromise, uploadId);
                        uploadState.currentId = null;
                        return { default: url };
                    } finally {
                        uploadState.isUploading = false;
                    }
                },
                abort: () => {
                    // 检查是否有活跃上传，防止取消错误的上传
                    if (!uploadState.isUploading) {
                        manager.logger.debug('Upload abort requested but no upload in progress');
                        return;
                    }

                    // 立即捕获当前 uploadId 到本地变量，避免竞态条件
                    const idToCancel = uploadState.currentId;
                    manager.logger.debug('Upload abort requested', { uploadId: idToCancel });

                    if (idToCancel) {
                        // 立即清除状态，防止重复取消
                        uploadState.currentId = null;

                        // 清理前端 pending promise
                        const resolver = manager.pendingUploads.get(idToCancel);
                        if (resolver) {
                            manager.pendingUploads.delete(idToCancel);
                            resolver.reject(new Error('Upload cancelled'));
                        }
                        // 通知服务器取消上传（类型安全检查）
                        if (manager.server?.cancelUploadFromClient) {
                            manager.server.cancelUploadFromClient(idToCancel);
                        }
                    }
                }
            };
        };
    }

    /**
     * Resolve a pending upload from server callback.
     * @param uploadId - The upload ID returned from handleFileUpload
     * @param url - The URL of the uploaded file (null if error)
     * @param errorMessage - Error message if upload failed (null if success)
     */
    resolveUpload(uploadId: string, url: string | null, errorMessage: string | null): void {
        const resolver = this.pendingUploads.get(uploadId);
        if (!resolver) {
            this.logger.warn(`No pending upload found for ID: ${uploadId}`);
            return;
        }

        this.pendingUploads.delete(uploadId);

        if (url) {
            resolver.resolve(url);
        } else {
            resolver.reject(new Error(errorMessage || 'Upload failed'));
        }
    }

    /**
     * Clean up all pending uploads.
     * Called when the component is disconnected.
     */
    cleanup(): void {
        if (this.pendingUploads.size > 0) {
            const disconnectError = new Error('Component disconnected');
            this.pendingUploads.forEach(resolver => resolver.reject(disconnectError));
            this.pendingUploads.clear();
        }
    }

    /**
     * Wrap a promise with a timeout.
     * If timeout is 0 or negative, no timeout is applied.
     *
     * @param promise - The promise to wrap
     * @param uploadId - Upload ID for cleanup on timeout
     * @returns The wrapped promise
     */
    private withTimeout<T>(promise: Promise<T>, uploadId: string): Promise<T> {
        if (this.uploadTimeoutMs <= 0) {
            return promise;
        }

        return new Promise<T>((resolve, reject) => {
            const timeoutId = setTimeout(() => {
                // 清理 pending upload
                const resolver = this.pendingUploads.get(uploadId);
                if (resolver) {
                    this.pendingUploads.delete(uploadId);
                }

                // 通知服务器取消上传
                if (this.server?.cancelUploadFromClient) {
                    this.server.cancelUploadFromClient(uploadId);
                }

                reject(new Error(`Upload timed out after ${this.uploadTimeoutMs / 1000} seconds`));
            }, this.uploadTimeoutMs);

            promise
                .then((result) => {
                    clearTimeout(timeoutId);
                    resolve(result);
                })
                .catch((error) => {
                    clearTimeout(timeoutId);
                    reject(error);
                });
        });
    }

    /**
     * Convert a file to Base64 string.
     */
    private fileToBase64(file: File): Promise<string> {
        return new Promise<string>((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                const result = reader.result as string;
                // Remove data URL prefix (e.g., "data:image/png;base64,")
                const base64 = result.split(',')[1];
                resolve(base64);
            };
            reader.onerror = () => reject(new Error(`Failed to read file: ${reader.error?.message || 'Unknown error'}`));
            reader.readAsDataURL(file);
        });
    }

    /**
     * Check if there are pending uploads.
     */
    hasPendingUploads(): boolean {
        return this.pendingUploads.size > 0;
    }
}
