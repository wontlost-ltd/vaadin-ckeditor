package com.wontlost.ckeditor;

import com.wontlost.ckeditor.VaadinCKEditorPremium.PremiumPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VaadinCKEditorPremium class.
 */
class VaadinCKEditorPremiumTest {

    @BeforeEach
    void setUp() {
        VaadinCKEditorPremium.resetForTesting();
    }

    @Test
    void testEnable() {
        assertFalse(VaadinCKEditorPremium.isEnabled());
        // First call should return true (enabling)
        assertTrue(VaadinCKEditorPremium.enable());
        assertTrue(VaadinCKEditorPremium.isEnabled());
        // Second call should return false (already enabled)
        assertFalse(VaadinCKEditorPremium.enable());
    }

    @Test
    void testGetVersion() {
        String version = VaadinCKEditorPremium.getVersion();
        assertNotNull(version);
        assertFalse(version.isEmpty());
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"));
    }

    @Test
    void testPremiumPluginExportPdf() {
        PremiumPlugin plugin = PremiumPlugin.EXPORT_PDF;
        assertEquals("ExportPdf", plugin.getPluginName());
        assertArrayEquals(new String[]{"exportPdf"}, plugin.getToolbarItems());

        CustomPlugin customPlugin = plugin.toCustomPlugin();
        assertNotNull(customPlugin);
        assertEquals("ExportPdf", customPlugin.getJsName());
        assertTrue(customPlugin.isPremium());
    }

    @Test
    void testPremiumPluginExportWord() {
        PremiumPlugin plugin = PremiumPlugin.EXPORT_WORD;
        assertEquals("ExportWord", plugin.getPluginName());
        assertArrayEquals(new String[]{"exportWord"}, plugin.getToolbarItems());
    }

    @Test
    void testPremiumPluginFormatPainter() {
        PremiumPlugin plugin = PremiumPlugin.FORMAT_PAINTER;
        assertEquals("FormatPainter", plugin.getPluginName());
        assertArrayEquals(new String[]{"formatPainter"}, plugin.getToolbarItems());

        CustomPlugin customPlugin = plugin.toCustomPlugin();
        assertTrue(customPlugin.isPremium());
    }

    @Test
    void testPremiumPluginCaseChange() {
        PremiumPlugin plugin = PremiumPlugin.CASE_CHANGE;
        assertEquals("CaseChange", plugin.getPluginName());
        assertArrayEquals(new String[]{"caseChange"}, plugin.getToolbarItems());
    }

    @Test
    void testPremiumPluginSlashCommand() {
        PremiumPlugin plugin = PremiumPlugin.SLASH_COMMAND;
        assertEquals("SlashCommand", plugin.getPluginName());
        // SlashCommand has no toolbar items (triggered by typing /)
        assertEquals(0, plugin.getToolbarItems().length);
    }

    @Test
    void testPremiumPluginAIAssistant() {
        PremiumPlugin plugin = PremiumPlugin.AI_ASSISTANT;
        assertEquals("AIAssistant", plugin.getPluginName());
        // AI Assistant has multiple toolbar items
        assertArrayEquals(new String[]{"aiCommands", "aiAssistant"}, plugin.getToolbarItems());
    }

    @Test
    void testPremiumPluginTrackChanges() {
        PremiumPlugin plugin = PremiumPlugin.TRACK_CHANGES;
        assertEquals("TrackChanges", plugin.getPluginName());
        assertArrayEquals(new String[]{"trackChanges"}, plugin.getToolbarItems());
    }

    @Test
    void testPremiumPluginComments() {
        PremiumPlugin plugin = PremiumPlugin.COMMENTS;
        assertEquals("Comments", plugin.getPluginName());
        assertArrayEquals(new String[]{"comment"}, plugin.getToolbarItems());
    }

    @Test
    void testAllPremiumPluginsCreateValidCustomPlugins() {
        for (PremiumPlugin plugin : PremiumPlugin.values()) {
            CustomPlugin customPlugin = plugin.toCustomPlugin();
            assertNotNull(customPlugin, "Plugin " + plugin.name() + " should create a valid CustomPlugin");
            assertEquals(plugin.getPluginName(), customPlugin.getJsName());
            assertTrue(customPlugin.isPremium(), "Plugin " + plugin.name() + " should be marked as premium");
        }
    }

    @Test
    void testToolbarItemsDefensiveCopy() {
        PremiumPlugin plugin = PremiumPlugin.EXPORT_PDF;
        String[] items1 = plugin.getToolbarItems();
        String[] items2 = plugin.getToolbarItems();

        // Should return a defensive copy, not the same array
        assertNotSame(items1, items2);
        assertArrayEquals(items1, items2);
    }
}
