package com.wontlost.ckeditor;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.wontlost.ckeditor.event.WordCountChangeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 实时字数事件与字数 / 字符数上限（#70）：事件分发、属性同步、自校验、Binder 默认校验器与重新校验。
 */
class WordCountLimitTest {

    /** Binder 测试用 bean */
    public static class Post implements Serializable {
        private String body = "";
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    private static VaadinCKEditor editor() {
        return VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).build();
    }

    /** 模拟浏览器输入（与 syncContent RPC 同路径） */
    private static void type(VaadinCKEditor editor, String html) {
        editor.syncContentInternal(html, null, "USER_INPUT", false, true);
    }

    private static void fireFromClient(VaadinCKEditor editor, int words, int characters) throws Exception {
        Method m = VaadinCKEditor.class.getDeclaredMethod("fireWordCountChange", int.class, int.class);
        m.setAccessible(true);
        m.invoke(editor, words, characters);
    }

    @Test
    @DisplayName("client counts reach listeners; registering toggles the wordCountEvents property")
    void listenerReceivesClientCounts() throws Exception {
        VaadinCKEditor editor = editor();
        assertThat(editor.getElement().getProperty("wordCountEvents", false)).isFalse();

        List<WordCountChangeEvent> events = new ArrayList<>();
        var registration = editor.addWordCountChangeListener(events::add);
        assertThat(editor.getElement().getProperty("wordCountEvents", false)).isTrue();
        assertThat(editor.getListenerStats().wordCount).isEqualTo(1);

        fireFromClient(editor, 3, 17);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getWords()).isEqualTo(3);
        assertThat(events.get(0).getCharacters()).isEqualTo(17);
        assertThat(events.get(0).isFromClient()).isTrue();
        assertThat(events.get(0).getSource()).isSameAs(editor);

        registration.remove();
        assertThat(editor.getElement().getProperty("wordCountEvents", true)).isFalse();
        fireFromClient(editor, 4, 20);
        assertThat(events).hasSize(1);
    }

    @Test
    @DisplayName("listener and counter load the VaadinWordCount bridge as a registry custom plugin")
    void bridgePluginAddedThroughCustomPluginMechanism() {
        VaadinCKEditor editor = editor();
        assertThat(editor.getElement().getPropertyRaw("plugins").toString()).doesNotContain("VaadinWordCount");

        editor.addWordCountChangeListener(e -> { });
        String plugins = editor.getElement().getPropertyRaw("plugins").toString();
        assertThat(plugins).contains("\"name\":\"VaadinWordCount\"").contains("\"premium\":false");
        assertThat(plugins).doesNotContain("importPath");

        VaadinCKEditor visible = VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC)
            .withWordCountVisible(true).build();
        assertThat(visible.getElement().getPropertyRaw("plugins").toString()).contains("VaadinWordCount");
        assertThat(VaadinCKEditor.WORD_COUNT_PLUGIN).isEqualTo(CustomPlugin.fromRegistry("VaadinWordCount"));
    }

    @Test
    @DisplayName("cleanupListeners removes word count listeners and resets the property")
    void cleanupRemovesListeners() {
        VaadinCKEditor editor = editor();
        editor.addWordCountChangeListener(e -> { });
        editor.cleanupListeners();
        assertThat(editor.getListenerStats().wordCount).isZero();
        assertThat(editor.getElement().getProperty("wordCountEvents", true)).isFalse();
    }

    @Test
    @DisplayName("limits and counter visibility are synced to the client")
    void propertiesSynced() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withMaxWords(100)
            .withMaxCharacters(500)
            .withWordCountVisible(true)
            .build();
        assertThat(editor.getMaxWords()).isEqualTo(100);
        assertThat(editor.getMaxCharacters()).isEqualTo(500);
        assertThat(editor.isWordCountVisible()).isTrue();
        assertThat(editor.getElement().getProperty("maxWords", 0)).isEqualTo(100);
        assertThat(editor.getElement().getProperty("maxCharacters", 0)).isEqualTo(500);
        assertThat(editor.getElement().getProperty("wordCountVisible", false)).isTrue();
    }

    @Test
    @DisplayName("negative limits are rejected, 0 means no limit")
    void negativeLimitsRejected() {
        VaadinCKEditor editor = editor();
        assertThatThrownBy(() -> editor.setMaxWords(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> editor.setMaxCharacters(-5)).isInstanceOf(IllegalArgumentException.class);
        editor.setMaxWords(0);
        type(editor, "<p>" + "word ".repeat(1000) + "</p>");
        assertThat(editor.isInvalid()).isFalse();
    }

    @Test
    @DisplayName("field marks itself invalid over the word limit and valid again under it")
    void selfValidationWords() {
        VaadinCKEditor editor = editor();
        editor.setMaxWords(3);

        type(editor, "<p>one two three</p>");
        assertThat(editor.isInvalid()).isFalse();

        type(editor, "<p>one two three four</p>");
        assertThat(editor.isInvalid()).isTrue();
        assertThat(editor.getErrorMessage()).isEqualTo("Maximum 3 words allowed (4 entered)");

        type(editor, "<p>one two</p>");
        assertThat(editor.isInvalid()).isFalse();
    }

    @Test
    @DisplayName("character limit counts like CKEditor (block boundaries are not characters)")
    void selfValidationCharacters() {
        VaadinCKEditor editor = editor();
        editor.setMaxCharacters(10);
        type(editor, "<p>Hello</p><p>world</p>");          // 10
        assertThat(editor.isInvalid()).isFalse();
        type(editor, "<p>Hello</p><p>world!</p>");         // 11
        assertThat(editor.isInvalid()).isTrue();
        assertThat(editor.getErrorMessage()).isEqualTo("Maximum 10 characters allowed (11 entered)");
    }

    @Test
    @DisplayName("custom error messages replace {0} and {1}, apostrophes are kept")
    void customErrorMessages() {
        VaadinCKEditor editor = editor();
        editor.setMaxWords(1);
        editor.setMaxWordsErrorMessage("You can't use more than {0} words, you have {1}");
        type(editor, "<p>a b</p>");
        assertThat(editor.getErrorMessage()).isEqualTo("You can't use more than 1 words, you have 2");

        editor.setMaxWords(0);
        editor.setMaxCharacters(2);
        editor.setMaxCharactersErrorMessage("Zu lang: {1}/{0}");
        assertThat(editor.getErrorMessage()).isEqualTo("Zu lang: 3/2");
    }

    @Test
    @DisplayName("changing the limit re-validates the current value")
    void limitChangeRevalidates() {
        VaadinCKEditor editor = editor();
        type(editor, "<p>one two three</p>");
        editor.setMaxWords(2);
        assertThat(editor.isInvalid()).isTrue();
        editor.setMaxWords(0);
        assertThat(editor.isInvalid()).isFalse();
    }

    @Test
    @DisplayName("initial builder value over the limit is invalid right away")
    void initialValueValidated() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withValue("<p>one two three</p>")
            .withMaxWords(2)
            .build();
        assertThat(editor.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("an invalid state set by the application is not cleared by the limit check")
    void doesNotClearForeignInvalidState() {
        VaadinCKEditor editor = editor();
        editor.setMaxWords(5);
        editor.setInvalid(true);
        editor.setErrorMessage("server says no");
        type(editor, "<p>short</p>");
        assertThat(editor.isInvalid()).isTrue();
        assertThat(editor.getErrorMessage()).isEqualTo("server says no");
    }

    @Test
    @DisplayName("without limits the editor never touches the invalid state (backward compatible)")
    void noLimitsNoValidation() {
        VaadinCKEditor editor = editor();
        editor.setInvalid(true);
        type(editor, "<p>text</p>");
        assertThat(editor.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("manual validation disables self-validation but keeps the default validator")
    void manualValidation() {
        VaadinCKEditor editor = editor();
        editor.setManualValidation(true);
        editor.setMaxWords(1);
        type(editor, "<p>a b c</p>");
        assertThat(editor.isInvalid()).isFalse();
        assertThat(editor.getDefaultValidator().apply("<p>a b c</p>", null).isError()).isTrue();
    }

    @Test
    @DisplayName("Binder picks up the default validator: validate() and writeBean fail over the limit")
    void binderUsesDefaultValidator() {
        VaadinCKEditor editor = editor();
        editor.setMaxWords(2);
        Binder<Post> binder = new Binder<>(Post.class);
        binder.forField(editor).bind(Post::getBody, Post::setBody);
        Post post = new Post();
        binder.setBean(post);

        type(editor, "<p>one two</p>");
        assertThat(binder.validate().isOk()).isTrue();
        assertThat(post.getBody()).isEqualTo("<p>one two</p>");

        type(editor, "<p>one two three</p>");
        BinderValidationStatus<Post> status = binder.validate();
        assertThat(status.isOk()).isFalse();
        assertThat(status.getFieldValidationErrors().get(0).getMessage())
            .contains("Maximum 2 words allowed (3 entered)");
        assertThat(editor.isInvalid()).isTrue();
        assertThat(post.getBody()).as("setBean 不写回无效值").isEqualTo("<p>one two</p>");

    }

    @Test
    @DisplayName("writeBean fails over the limit")
    void writeBeanFailsOverLimit() {
        VaadinCKEditor editor = editor();
        editor.setMaxCharacters(5);
        Binder<Post> binder = new Binder<>(Post.class);
        binder.forField(editor).bind(Post::getBody, Post::setBody);
        binder.readBean(new Post());
        type(editor, "<p>too long</p>");
        Post target = new Post();
        assertThatThrownBy(() -> binder.writeBean(target)).isInstanceOf(ValidationException.class);
        assertThat(target.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Binder re-validates when only the limit changes")
    void binderRevalidatesOnLimitChange() {
        VaadinCKEditor editor = editor();
        Binder<Post> binder = new Binder<>(Post.class);
        binder.forField(editor).bind(Post::getBody, Post::setBody);
        binder.setBean(new Post());
        type(editor, "<p>one two three</p>");
        assertThat(binder.isValid()).isTrue();

        List<Boolean> statuses = new ArrayList<>();
        binder.addStatusChangeListener(e -> statuses.add(e.hasValidationErrors()));
        editor.setMaxWords(2);

        assertThat(statuses).contains(true);
        assertThat(editor.isInvalid()).isTrue();
        assertThat(binder.isValid()).isFalse();
    }

    @Test
    @DisplayName("other Binder validators are not overwritten by the limit check")
    void otherBinderValidatorsWin() {
        VaadinCKEditor editor = editor();
        editor.setMaxWords(10);
        Binder<Post> binder = new Binder<>(Post.class);
        binder.forField(editor)
            .withValidator(v -> !v.contains("forbidden"), "No forbidden words")
            .bind(Post::getBody, Post::setBody);
        binder.setBean(new Post());

        type(editor, "<p>forbidden</p>");
        assertThat(editor.isInvalid()).isTrue();
        assertThat(editor.getErrorMessage()).isEqualTo("No forbidden words");

        type(editor, "<p>fine</p>");
        assertThat(editor.isInvalid()).isFalse();
    }

    @Test
    @DisplayName("limits survive session serialization")
    void serialization() throws Exception {
        VaadinCKEditor editor = editor();
        editor.setMaxWords(7);
        editor.setMaxCharactersErrorMessage("{1}>{0}");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(editor);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            VaadinCKEditor copy = (VaadinCKEditor) in.readObject();
            assertThat(copy.getMaxWords()).isEqualTo(7);
            copy.setMaxCharacters(1);
            type(copy, "<p>ab</p>");
            assertThat(copy.getErrorMessage()).isEqualTo("2>1");
        }
    }
}
