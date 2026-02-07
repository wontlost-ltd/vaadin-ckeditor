package com.wontlost.ckeditor;

import com.wontlost.ckeditor.internal.EventDispatcher;
import com.wontlost.ckeditor.handler.ErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventDispatcher.
 * Note: Since ComponentEvent requires a non-null source component,
 * and creating VaadinCKEditor instances requires Vaadin test environment,
 * this test class only tests features that don't depend on event firing.
 */
class EventDispatcherTest {

    private EventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // Use null as source
        // Note: Event firing tests should be done in integration tests
        dispatcher = new EventDispatcher(null);
    }

    // ==================== ErrorHandler Tests ====================

    @Test
    @DisplayName("setErrorHandler and getErrorHandler should work")
    void setAndGetErrorHandler() {
        assertNull(dispatcher.getErrorHandler());

        ErrorHandler handler = err -> false;
        dispatcher.setErrorHandler(handler);

        assertEquals(handler, dispatcher.getErrorHandler());
    }

    @Test
    @DisplayName("setErrorHandler should allow null")
    void setErrorHandlerAllowsNull() {
        ErrorHandler handler = err -> false;
        dispatcher.setErrorHandler(handler);
        assertNotNull(dispatcher.getErrorHandler());

        dispatcher.setErrorHandler(null);
        assertNull(dispatcher.getErrorHandler());
    }

    // ==================== Listener Registration Tests ====================

    @Test
    @DisplayName("addEditorReadyListener should register and return registration")
    void addEditorReadyListenerReturnsRegistration() {
        var registration = dispatcher.addEditorReadyListener(event -> {});
        assertNotNull(registration);

        assertEquals(1, dispatcher.getListenerStats().ready);

        registration.remove();
        assertEquals(0, dispatcher.getListenerStats().ready);
    }

    @Test
    @DisplayName("addEditorErrorListener should register and return registration")
    void addEditorErrorListenerReturnsRegistration() {
        var registration = dispatcher.addEditorErrorListener(event -> {});
        assertNotNull(registration);

        assertEquals(1, dispatcher.getListenerStats().error);

        registration.remove();
        assertEquals(0, dispatcher.getListenerStats().error);
    }

    @Test
    @DisplayName("addAutosaveListener should register and return registration")
    void addAutosaveListenerReturnsRegistration() {
        var registration = dispatcher.addAutosaveListener(event -> {});
        assertNotNull(registration);

        assertEquals(1, dispatcher.getListenerStats().autosave);

        registration.remove();
        assertEquals(0, dispatcher.getListenerStats().autosave);
    }

    @Test
    @DisplayName("addContentChangeListener should register and return registration")
    void addContentChangeListenerReturnsRegistration() {
        var registration = dispatcher.addContentChangeListener(event -> {});
        assertNotNull(registration);

        assertEquals(1, dispatcher.getListenerStats().contentChange);

        registration.remove();
        assertEquals(0, dispatcher.getListenerStats().contentChange);
    }

    @Test
    @DisplayName("addFallbackListener should register and return registration")
    void addFallbackListenerReturnsRegistration() {
        var registration = dispatcher.addFallbackListener(event -> {});
        assertNotNull(registration);

        assertEquals(1, dispatcher.getListenerStats().fallback);

        registration.remove();
        assertEquals(0, dispatcher.getListenerStats().fallback);
    }

    // ==================== Multiple Listeners Tests ====================

    @Test
    @DisplayName("Multiple listeners of same type should be tracked")
    void multipleListenersTracked() {
        dispatcher.addEditorReadyListener(event -> {});
        dispatcher.addEditorReadyListener(event -> {});
        dispatcher.addEditorReadyListener(event -> {});

        assertEquals(3, dispatcher.getListenerStats().ready);
    }

    @Test
    @DisplayName("Listeners of different types should be tracked separately")
    void differentListenerTypesTracked() {
        dispatcher.addEditorReadyListener(event -> {});
        dispatcher.addEditorErrorListener(event -> {});
        dispatcher.addAutosaveListener(event -> {});

        var stats = dispatcher.getListenerStats();
        assertEquals(1, stats.ready);
        assertEquals(1, stats.error);
        assertEquals(1, stats.autosave);
        assertEquals(0, stats.contentChange);
        assertEquals(0, stats.fallback);
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("getListenerStats should return correct totals")
    void getListenerStatsReturnsCorrectTotals() {
        dispatcher.addEditorReadyListener(event -> {});
        dispatcher.addEditorReadyListener(event -> {});
        dispatcher.addEditorErrorListener(event -> {});
        dispatcher.addAutosaveListener(event -> {});
        dispatcher.addContentChangeListener(event -> {});
        dispatcher.addFallbackListener(event -> {});

        EventDispatcher.ListenerStats stats = dispatcher.getListenerStats();

        assertEquals(2, stats.ready);
        assertEquals(1, stats.error);
        assertEquals(1, stats.autosave);
        assertEquals(1, stats.contentChange);
        assertEquals(1, stats.fallback);
        assertEquals(6, stats.total());
    }

    @Test
    @DisplayName("ListenerStats.total should sum all listeners")
    void listenerStatsTotalSumsAll() {
        // Initial state
        assertEquals(0, dispatcher.getListenerStats().total());

        // Add listeners
        dispatcher.addEditorReadyListener(event -> {});
        assertEquals(1, dispatcher.getListenerStats().total());

        dispatcher.addEditorErrorListener(event -> {});
        assertEquals(2, dispatcher.getListenerStats().total());
    }

    // ==================== Cleanup Tests ====================

    @Test
    @DisplayName("cleanup should remove all listeners")
    void cleanupRemovesAllListeners() {
        dispatcher.addEditorReadyListener(event -> {});
        dispatcher.addEditorErrorListener(event -> {});
        dispatcher.addAutosaveListener(event -> {});
        dispatcher.addContentChangeListener(event -> {});
        dispatcher.addFallbackListener(event -> {});

        assertEquals(5, dispatcher.getListenerStats().total());

        dispatcher.cleanup();

        assertEquals(0, dispatcher.getListenerStats().total());
        assertEquals(0, dispatcher.getListenerStats().ready);
        assertEquals(0, dispatcher.getListenerStats().error);
        assertEquals(0, dispatcher.getListenerStats().autosave);
        assertEquals(0, dispatcher.getListenerStats().contentChange);
        assertEquals(0, dispatcher.getListenerStats().fallback);
    }

    @Test
    @DisplayName("cleanup should be safe to call multiple times")
    void cleanupSafeToCallMultipleTimes() {
        dispatcher.addEditorReadyListener(event -> {});

        assertDoesNotThrow(() -> {
            dispatcher.cleanup();
            dispatcher.cleanup();
            dispatcher.cleanup();
        });
    }

    @Test
    @DisplayName("cleanup on empty dispatcher should not throw")
    void cleanupOnEmptyDoesNotThrow() {
        assertDoesNotThrow(() -> dispatcher.cleanup());
    }

    // ==================== Registration Tests ====================

    @Test
    @DisplayName("Registration.remove should be idempotent")
    void registrationRemoveIdempotent() {
        var registration = dispatcher.addEditorReadyListener(event -> {});
        assertEquals(1, dispatcher.getListenerStats().ready);

        registration.remove();
        assertEquals(0, dispatcher.getListenerStats().ready);

        // Multiple calls should not throw exception
        assertDoesNotThrow(() -> registration.remove());
        assertEquals(0, dispatcher.getListenerStats().ready);
    }

    @Test
    @DisplayName("Multiple registrations should be independent")
    void multipleRegistrationsIndependent() {
        var reg1 = dispatcher.addEditorReadyListener(event -> {});
        var reg2 = dispatcher.addEditorReadyListener(event -> {});

        assertEquals(2, dispatcher.getListenerStats().ready);

        reg1.remove();
        assertEquals(1, dispatcher.getListenerStats().ready);

        reg2.remove();
        assertEquals(0, dispatcher.getListenerStats().ready);
    }

    // ==================== Error Handler Tests (without event firing) ====================
    // Note: fireEditorError tests require a non-null VaadinCKEditor source
    // since ComponentEvent constructor rejects null. These tests belong in
    // integration tests with a proper Vaadin test environment.
    // The error handler isolation behavior is tested in EnterpriseEventTest.
}
