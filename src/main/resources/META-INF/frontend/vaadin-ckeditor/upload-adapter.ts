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
    private maxFileSize: number = 10 * 1024 * 1024; // Default 10MB
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
            // Track upload state at the adapter level so abort() can access it.
            // Use an object wrapper for safe sharing and updating within closures.
            // The isUploading flag prevents race conditions during fileToBase64.
            const uploadState = {
                currentId: null as string | null,
                isUploading: false,
                abortController: null as AbortController | null,
            };

            return {
                upload: async (): Promise<{ default: string }> => {
                    // Prevent concurrent uploads for the same adapter instance
                    if (uploadState.isUploading) {
                        throw new Error('Upload already in progress for this adapter');
                    }
                    uploadState.isUploading = true;

                    // Generate upload ID and assign to state BEFORE any async gap,
                    // so abort() can always find the current upload ID
                    const uploadId = `upload-${manager.editorId}-${++manager.uploadIdCounter}`;
                    uploadState.currentId = uploadId;

                    // Create AbortController for cancelling FileReader if abort() is called
                    const abortController = new AbortController();
                    uploadState.abortController = abortController;

                    try {
                        const file = await loader.file;
                        if (!file) {
                            throw new Error('No file provided');
                        }

                        // Empty file validation
                        if (file.size === 0) {
                            manager.logger.debug('Empty file rejected', { fileName: file.name });
                            throw new Error('Cannot upload empty file');
                        }

                        // File size validation
                        if (file.size > manager.maxFileSize) {
                            throw new Error(`File size ${file.size} exceeds maximum allowed ${manager.maxFileSize} bytes`);
                        }

                        // MIME type whitelist validation
                        if (!manager.isMimeTypeAllowed(file.type)) {
                            throw new Error(`File type '${file.type}' is not allowed. Allowed types: ${Array.from(manager.allowedMimeTypes).join(', ')}`);
                        }

                        // Pass AbortSignal so FileReader can be aborted on cancel
                        const base64Data = await manager.fileToBase64(file, abortController.signal);

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

                        // Apply timeout mechanism
                        const url = await manager.withTimeout(uploadPromise, uploadId);
                        uploadState.currentId = null;
                        return { default: url };
                    } finally {
                        uploadState.isUploading = false;
                        uploadState.abortController = null;
                    }
                },
                abort: () => {
                    // Check if there is an active upload, prevent cancelling the wrong one
                    if (!uploadState.isUploading) {
                        manager.logger.debug('Upload abort requested but no upload in progress');
                        return;
                    }

                    // Capture current uploadId in local variable immediately to avoid race condition
                    const idToCancel = uploadState.currentId;
                    manager.logger.debug('Upload abort requested', { uploadId: idToCancel });

                    // Abort FileReader if still in progress
                    if (uploadState.abortController) {
                        uploadState.abortController.abort();
                        uploadState.abortController = null;
                    }

                    if (idToCancel) {
                        // Clear state immediately to prevent duplicate cancellation
                        uploadState.currentId = null;

                        // Clean up frontend pending promise
                        const resolver = manager.pendingUploads.get(idToCancel);
                        if (resolver) {
                            manager.pendingUploads.delete(idToCancel);
                            resolver.reject(new Error('Upload cancelled'));
                        }
                        // Notify server to cancel upload (type-safe check)
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
                // Clean up pending upload
                const resolver = this.pendingUploads.get(uploadId);
                if (resolver) {
                    this.pendingUploads.delete(uploadId);
                }

                // Notify server to cancel upload
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
     * Accepts an optional AbortSignal to abort the FileReader when upload is cancelled.
     */
    private fileToBase64(file: File, signal?: AbortSignal): Promise<string> {
        return new Promise<string>((resolve, reject) => {
            const reader = new FileReader();

            const cleanup = (): void => {
                reader.onload = null;
                reader.onerror = null;
                reader.onabort = null;
            };

            // Listen for abort signal to cancel the FileReader
            if (signal) {
                signal.addEventListener('abort', () => {
                    reader.abort();
                    cleanup();
                    reject(new Error('File reading cancelled'));
                }, { once: true });
            }

            reader.onload = () => {
                cleanup();
                const result = reader.result as string;
                // Remove data URL prefix (e.g., "data:image/png;base64,")
                const base64 = result.split(',')[1];
                if (!base64) {
                    reject(new Error('Failed to extract Base64 data from file'));
                    return;
                }
                resolve(base64);
            };
            reader.onerror = () => {
                cleanup();
                reject(new Error(`Failed to read file: ${reader.error?.message || 'Unknown error'}`));
            };
            reader.onabort = () => {
                cleanup();
                reject(new Error('File reading cancelled'));
            };
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
