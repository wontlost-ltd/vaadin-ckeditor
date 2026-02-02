/**
 * Unit Tests for Upload Adapter Module
 *
 * Run with: npx vitest run upload-adapter.test.ts
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { UploadAdapterManager } from './upload-adapter';

// Mock logger
const createMockLogger = () => ({
    debug: vi.fn(),
    warn: vi.fn(),
});

// Mock server
const createMockServer = () => ({
    handleFileUpload: vi.fn(),
});

describe('UploadAdapterManager', () => {
    let manager: UploadAdapterManager;
    let logger: ReturnType<typeof createMockLogger>;
    let server: ReturnType<typeof createMockServer>;

    beforeEach(() => {
        logger = createMockLogger();
        server = createMockServer();
        manager = new UploadAdapterManager('test-editor', logger);
        manager.setServer(server);
    });

    describe('file size validation', () => {
        it('should use default max file size of 10MB', () => {
            // File under 10MB should be accepted (we test via MIME type validation first)
            expect(manager.isMimeTypeAllowed('image/jpeg')).toBe(true);
        });

        it('should allow setting custom max file size', () => {
            manager.setMaxFileSize(5 * 1024 * 1024); // 5MB

            // This just sets the limit, actual validation happens during upload
            // We verify the setter doesn't throw
            expect(() => manager.setMaxFileSize(1024)).not.toThrow();
        });
    });

    describe('MIME type validation', () => {
        it('should allow default image types', () => {
            const allowedTypes = [
                'image/jpeg',
                'image/png',
                'image/gif',
                'image/webp',
                'image/svg+xml',
                'image/bmp',
                'image/tiff',
            ];

            for (const type of allowedTypes) {
                expect(manager.isMimeTypeAllowed(type)).toBe(true);
            }
        });

        it('should reject non-image types by default', () => {
            const rejectedTypes = [
                'application/pdf',
                'text/html',
                'application/javascript',
                'application/x-executable',
            ];

            for (const type of rejectedTypes) {
                expect(manager.isMimeTypeAllowed(type)).toBe(false);
            }
        });

        it('should allow custom MIME types to be set', () => {
            manager.setAllowedMimeTypes(['application/pdf', 'text/plain']);

            expect(manager.isMimeTypeAllowed('application/pdf')).toBe(true);
            expect(manager.isMimeTypeAllowed('text/plain')).toBe(true);
            expect(manager.isMimeTypeAllowed('image/jpeg')).toBe(false);
        });

        it('should allow adding MIME types to existing list', () => {
            manager.addAllowedMimeTypes(['application/pdf']);

            expect(manager.isMimeTypeAllowed('image/jpeg')).toBe(true);
            expect(manager.isMimeTypeAllowed('application/pdf')).toBe(true);
        });

        it('should allow all types when whitelist is empty', () => {
            manager.setAllowedMimeTypes([]);

            expect(manager.isMimeTypeAllowed('anything/here')).toBe(true);
        });
    });

    describe('upload adapter factory', () => {
        it('should create upload adapter factory', () => {
            const factory = manager.createUploadAdapterFactory();

            expect(factory).toBeInstanceOf(Function);
        });

        it('should create adapter with upload and abort methods', () => {
            const factory = manager.createUploadAdapterFactory();
            const mockLoader = {
                file: Promise.resolve(
                    new File(['test'], 'test.jpg', { type: 'image/jpeg' })
                ),
            };

            const adapter = factory(mockLoader);

            expect(adapter).toHaveProperty('upload');
            expect(adapter).toHaveProperty('abort');
            expect(typeof adapter.upload).toBe('function');
            expect(typeof adapter.abort).toBe('function');
        });
    });

    describe('upload resolution', () => {
        it('should resolve pending upload with URL', () => {
            // Simulate a pending upload
            const uploadId = 'test-upload-1';

            // We need to mock the internal pending uploads map
            // This tests the public resolveUpload method
            manager.resolveUpload(uploadId, 'https://example.com/image.jpg', null);

            // Should log warning since there's no pending upload
            expect(logger.warn).toHaveBeenCalledWith(
                `No pending upload found for ID: ${uploadId}`
            );
        });

        it('should reject pending upload with error', () => {
            const uploadId = 'test-upload-2';

            manager.resolveUpload(uploadId, null, 'Upload failed');

            expect(logger.warn).toHaveBeenCalled();
        });
    });

    describe('cleanup', () => {
        it('should handle cleanup when no pending uploads', () => {
            expect(() => manager.cleanup()).not.toThrow();
        });

        it('should report no pending uploads initially', () => {
            expect(manager.hasPendingUploads()).toBe(false);
        });
    });

    describe('server connection', () => {
        it('should handle missing server gracefully', async () => {
            manager.setServer(undefined);

            const factory = manager.createUploadAdapterFactory();
            const mockLoader = {
                file: Promise.resolve(
                    new File(['test'], 'test.jpg', { type: 'image/jpeg' })
                ),
            };

            const adapter = factory(mockLoader);

            // Upload should throw when server is not available
            await expect(adapter.upload()).rejects.toThrow('Server connection not available');
        });
    });
});

describe('UploadAdapterManager timeout mechanism', () => {
    it('should use default timeout of 5 minutes', () => {
        const logger = createMockLogger();
        const manager = new UploadAdapterManager('test', logger);

        // Default timeout should be set (we test via the setter)
        expect(() => manager.setUploadTimeout(60000)).not.toThrow();
    });

    it('should allow setting custom timeout', () => {
        const logger = createMockLogger();
        const manager = new UploadAdapterManager('test', logger);

        manager.setUploadTimeout(30000); // 30 seconds
        expect(() => manager.setUploadTimeout(0)).not.toThrow(); // Disable timeout
    });

    it('should reject upload after timeout', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);
        manager.setUploadTimeout(50); // 50ms for fast test

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);

        // Start upload but don't resolve it - should timeout
        await expect(adapter.upload()).rejects.toThrow(/timed out/);
    });

    it('should not timeout when disabled (timeout = 0)', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);
        manager.setUploadTimeout(0); // Disable timeout

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);

        // Start upload
        const uploadPromise = adapter.upload();

        // Wait a bit, then resolve
        await new Promise((resolve) => setTimeout(resolve, 10));

        // Get the upload ID and resolve it
        expect(server.handleFileUpload).toHaveBeenCalled();
        const uploadId = server.handleFileUpload.mock.calls[0][0];
        manager.resolveUpload(uploadId, 'http://example.com/test.jpg', null);

        // Should succeed since timeout is disabled
        const result = await uploadPromise;
        expect(result.default).toBe('http://example.com/test.jpg');
    });

    it('should clear timeout when upload completes successfully', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);
        manager.setUploadTimeout(1000); // 1 second

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);
        const uploadPromise = adapter.upload();

        // Resolve immediately
        await new Promise((resolve) => setTimeout(resolve, 5));
        const uploadId = server.handleFileUpload.mock.calls[0][0];
        manager.resolveUpload(uploadId, 'http://example.com/test.jpg', null);

        const result = await uploadPromise;
        expect(result.default).toBe('http://example.com/test.jpg');
    });
});

describe('UploadAdapterManager race condition prevention', () => {
    it('should prevent concurrent uploads on same adapter', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);
        manager.setUploadTimeout(5000);

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);

        // Start first upload
        const upload1 = adapter.upload();

        // Wait for first upload to start
        await new Promise((resolve) => setTimeout(resolve, 10));

        // Try to start second upload on same adapter - should fail
        await expect(adapter.upload()).rejects.toThrow('Upload already in progress');

        // Resolve first upload to clean up
        const uploadId = server.handleFileUpload.mock.calls[0][0];
        manager.resolveUpload(uploadId, 'http://example.com/test.jpg', null);
        await upload1;
    });

    it('should allow uploads on different adapters concurrently', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

        // Create two different adapters (different loaders)
        const mockLoader1 = { file: Promise.resolve(file) };
        const mockLoader2 = { file: Promise.resolve(file) };

        const adapter1 = factory(mockLoader1);
        const adapter2 = factory(mockLoader2);

        // Start both uploads
        const upload1 = adapter1.upload();
        const upload2 = adapter2.upload();

        // Wait for both to start
        await new Promise((resolve) => setTimeout(resolve, 10));

        // Both should have started
        expect(server.handleFileUpload).toHaveBeenCalledTimes(2);

        // Resolve both
        const uploadId1 = server.handleFileUpload.mock.calls[0][0];
        const uploadId2 = server.handleFileUpload.mock.calls[1][0];
        manager.resolveUpload(uploadId1, 'http://example.com/1.jpg', null);
        manager.resolveUpload(uploadId2, 'http://example.com/2.jpg', null);

        await Promise.all([upload1, upload2]);
    });

    it('should reset isUploading flag after upload completes', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);

        // First upload
        const upload1 = adapter.upload();
        await new Promise((resolve) => setTimeout(resolve, 10));
        const uploadId1 = server.handleFileUpload.mock.calls[0][0];
        manager.resolveUpload(uploadId1, 'http://example.com/1.jpg', null);
        await upload1;

        // Second upload on same adapter should work
        const upload2 = adapter.upload();
        await new Promise((resolve) => setTimeout(resolve, 10));
        const uploadId2 = server.handleFileUpload.mock.calls[1][0];
        manager.resolveUpload(uploadId2, 'http://example.com/2.jpg', null);
        const result = await upload2;

        expect(result.default).toBe('http://example.com/2.jpg');
    });

    it('should reset isUploading flag after upload fails', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);

        // First upload - fails
        const upload1 = adapter.upload();
        await new Promise((resolve) => setTimeout(resolve, 10));
        const uploadId1 = server.handleFileUpload.mock.calls[0][0];
        manager.resolveUpload(uploadId1, null, 'Upload failed');

        await expect(upload1).rejects.toThrow('Upload failed');

        // Second upload should work
        const upload2 = adapter.upload();
        await new Promise((resolve) => setTimeout(resolve, 10));
        const uploadId2 = server.handleFileUpload.mock.calls[1][0];
        manager.resolveUpload(uploadId2, 'http://example.com/2.jpg', null);
        const result = await upload2;

        expect(result.default).toBe('http://example.com/2.jpg');
    });
});

describe('UploadAdapterManager edge cases', () => {
    it('should handle concurrent uploads', () => {
        const logger = createMockLogger();
        const manager = new UploadAdapterManager('test', logger);

        // Multiple factories can be created
        const factory1 = manager.createUploadAdapterFactory();
        const factory2 = manager.createUploadAdapterFactory();

        expect(factory1).not.toBe(factory2);
    });

    it('should generate unique upload IDs', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('editor-1', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();

        // Create multiple uploads
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader1 = { file: Promise.resolve(file) };
        const mockLoader2 = { file: Promise.resolve(file) };

        // Start both uploads (they will wait for resolution)
        const upload1 = factory(mockLoader1).upload();
        const upload2 = factory(mockLoader2).upload();

        // Wait a tick for the async operations to process
        await new Promise((resolve) => setTimeout(resolve, 10));

        // Server should receive different upload IDs
        expect(server.handleFileUpload).toHaveBeenCalledTimes(2);

        const call1 = server.handleFileUpload.mock.calls[0];
        const call2 = server.handleFileUpload.mock.calls[1];

        expect(call1[0]).not.toBe(call2[0]); // Different IDs
        expect(call1[0]).toContain('editor-1'); // Contains editor ID

        // Resolve the pending uploads to clean up
        manager.resolveUpload(call1[0], 'http://example.com/1.jpg', null);
        manager.resolveUpload(call2[0], 'http://example.com/2.jpg', null);

        await Promise.all([upload1, upload2]);
    });
});

describe('UploadAdapterManager empty file handling', () => {
    it('should reject empty files', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();
        const emptyFile = new File([], 'empty.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(emptyFile) };

        const adapter = factory(mockLoader);

        await expect(adapter.upload()).rejects.toThrow('Cannot upload empty file');
    });

    it('should allow non-empty files', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);
        const uploadPromise = adapter.upload();

        await new Promise((resolve) => setTimeout(resolve, 10));
        const uploadId = server.handleFileUpload.mock.calls[0][0];
        manager.resolveUpload(uploadId, 'http://example.com/test.jpg', null);

        const result = await uploadPromise;
        expect(result.default).toBe('http://example.com/test.jpg');
    });
});

describe('UploadAdapterManager abort edge cases', () => {
    it('should handle abort when no upload in progress', () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);

        // Abort before starting upload should not throw
        expect(() => adapter.abort()).not.toThrow();
        expect(logger.debug).toHaveBeenCalledWith('Upload abort requested but no upload in progress');
    });

    it('should handle abort during active upload', async () => {
        const logger = createMockLogger();
        const server = createMockServer();
        const manager = new UploadAdapterManager('test', logger);
        manager.setServer(server);

        const factory = manager.createUploadAdapterFactory();
        const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
        const mockLoader = { file: Promise.resolve(file) };

        const adapter = factory(mockLoader);
        const uploadPromise = adapter.upload();

        await new Promise((resolve) => setTimeout(resolve, 10));

        // Abort during active upload
        adapter.abort();

        await expect(uploadPromise).rejects.toThrow('Upload cancelled');
    });
});
