/**
 * Fallback Renderer Module for VaadinCKEditor
 *
 * Provides graceful degradation rendering when CKEditor fails to initialize.
 * Supports multiple fallback modes: textarea, readonly, error message, and hidden.
 *
 * SECURITY: All methods use DOM API (textContent, value) instead of innerHTML
 * to prevent XSS attacks from editor content.
 */

/**
 * Fallback mode types
 */
export type FallbackMode = 'textarea' | 'readonly' | 'error' | 'hidden';

/**
 * Server communication interface for fallback data sync
 */
export interface FallbackServer {
    setEditorData(data: string): void;
}

/**
 * Fallback Renderer class.
 * Handles graceful degradation when CKEditor fails to load.
 */
export class FallbackRenderer {
    private container: HTMLElement;
    private server: FallbackServer | undefined;
    private textareaInputHandler?: () => void;

    constructor(container: HTMLElement, server?: FallbackServer) {
        this.container = container;
        this.server = server;
    }

    /**
     * Set the server reference for data sync.
     */
    setServer(server: FallbackServer | undefined): void {
        this.server = server;
    }

    /**
     * Render fallback UI based on the specified mode.
     * @param mode - The fallback mode to render
     * @param content - The editor content to display
     * @param errorMessage - Error message for 'error' mode
     */
    render(mode: FallbackMode, content: string, errorMessage?: string): void {
        switch (mode) {
            case 'textarea':
                this.renderTextarea(content);
                break;
            case 'readonly':
                this.renderReadOnly(content);
                break;
            case 'error':
                this.renderError(errorMessage || 'Editor failed to load');
                break;
            case 'hidden':
                this.hide();
                break;
        }
    }

    /**
     * Render fallback textarea.
     * Uses DOM API to prevent XSS attacks.
     */
    private renderTextarea(content: string): void {
        // Clear container safely
        this.container.textContent = '';

        // Create textarea using DOM API
        const textarea = document.createElement('textarea');
        textarea.style.cssText = `
            width: 100%;
            min-height: 200px;
            padding: 10px;
            font-family: inherit;
            font-size: inherit;
            line-height: 1.5;
            border: 1px solid #ccc;
            border-radius: 4px;
            resize: vertical;
        `;
        textarea.value = content || ''; // Safe: value property, not innerHTML

        this.textareaInputHandler = () => {
            if (this.server) {
                this.server.setEditorData(textarea.value);
            }
        };
        textarea.addEventListener('input', this.textareaInputHandler);

        this.container.appendChild(textarea);
    }

    /**
     * Render fallback read-only view.
     * Uses DOM API to prevent XSS attacks.
     */
    private renderReadOnly(content: string): void {
        // Clear container safely
        this.container.textContent = '';

        // Create wrapper div
        const wrapper = document.createElement('div');
        wrapper.style.cssText = `
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            min-height: 200px;
            background-color: #f9f9f9;
            overflow: auto;
        `;

        // Create content div - use textContent for safety
        const contentDiv = document.createElement('div');
        contentDiv.style.cssText = `
            white-space: pre-wrap;
            word-wrap: break-word;
            font-family: inherit;
            line-height: 1.5;
        `;
        contentDiv.textContent = content || ''; // Safe: textContent, not innerHTML

        // Create read-only indicator
        const indicator = document.createElement('div');
        indicator.style.cssText = `
            font-size: 12px;
            color: #666;
            margin-bottom: 8px;
            padding-bottom: 8px;
            border-bottom: 1px solid #ddd;
        `;
        indicator.textContent = '(Read-only mode - Editor failed to load)';

        wrapper.appendChild(indicator);
        wrapper.appendChild(contentDiv);
        this.container.appendChild(wrapper);
    }

    /**
     * Render fallback error message.
     * Uses DOM API to prevent XSS attacks.
     */
    private renderError(reason: string): void {
        // Clear container safely
        this.container.textContent = '';

        // Create error container using DOM API
        const errorDiv = document.createElement('div');
        errorDiv.style.cssText = `
            padding: 20px;
            color: #c00;
            background: #fee;
            border: 1px solid #fcc;
            border-radius: 4px;
            text-align: center;
        `;

        // Create icon (using text emoji for simplicity)
        const icon = document.createElement('div');
        icon.style.cssText = 'font-size: 32px; margin-bottom: 10px;';
        icon.textContent = '⚠️';
        errorDiv.appendChild(icon);

        // Create title
        const title = document.createElement('strong');
        title.style.cssText = 'display: block; margin-bottom: 8px; font-size: 16px;';
        title.textContent = 'Editor Error';
        errorDiv.appendChild(title);

        // Create reason text - use textContent for safety
        const reasonText = document.createElement('p');
        reasonText.style.cssText = 'margin: 0; font-size: 14px;';
        reasonText.textContent = reason; // Safe: textContent, not innerHTML
        errorDiv.appendChild(reasonText);

        // Create retry hint
        const hint = document.createElement('p');
        hint.style.cssText = 'margin: 16px 0 0 0; font-size: 12px; color: #666;';
        hint.textContent = 'Try refreshing the page. If the problem persists, contact support.';
        errorDiv.appendChild(hint);

        this.container.appendChild(errorDiv);
    }

    /**
     * Hide the container completely.
     */
    private hide(): void {
        this.container.style.display = 'none';
    }

    /**
     * Show the container (restore from hidden state).
     */
    show(): void {
        this.container.style.display = '';
    }

    /**
     * Clear the fallback content and remove event listeners.
     */
    clear(): void {
        // Remove textarea input listener before clearing DOM
        if (this.textareaInputHandler) {
            const textarea = this.container.querySelector('textarea');
            if (textarea) {
                textarea.removeEventListener('input', this.textareaInputHandler);
            }
            this.textareaInputHandler = undefined;
        }
        this.container.textContent = '';
    }
}
