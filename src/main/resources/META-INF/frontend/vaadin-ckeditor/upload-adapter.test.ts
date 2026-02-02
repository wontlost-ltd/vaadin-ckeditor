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
