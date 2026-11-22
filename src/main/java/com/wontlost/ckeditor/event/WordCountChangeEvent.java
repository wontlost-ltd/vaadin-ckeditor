package com.wontlost.ckeditor.event;

import com.vaadin.flow.component.ComponentEvent;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Live word and character count of the editor, reported by CKEditor's
 * <a href="https://ckeditor.com/docs/ckeditor5/latest/features/word-count.html">WordCount</a> plugin.
 *
 * <p>Fired while the user types, at most once per 500 ms (the plugin itself updates at most every
 * 250 ms), only when a count actually changed, and once with the initial counts after the editor is
 * created. The numbers are the ones CKEditor's counter shows.</p>
 *
 * <pre>
 * editor.addWordCountChangeListener(e -&gt;
 *     counter.setText(e.getWords() + " words, " + e.getCharacters() + " characters"));
 * </pre>
 *
 * @see VaadinCKEditor#addWordCountChangeListener(com.vaadin.flow.component.ComponentEventListener)
 */
public class WordCountChangeEvent extends ComponentEvent<VaadinCKEditor> {

    private final int words;
    private final int characters;

    /**
     * Create a word count change event.
     *
     * @param source     the editor
     * @param fromClient whether the event originated from the client
     * @param words      number of words
     * @param characters number of characters (line breaks between blocks are not counted)
     */
    public WordCountChangeEvent(VaadinCKEditor source, boolean fromClient, int words, int characters) {
        super(source, fromClient);
        this.words = words;
        this.characters = characters;
    }

    /**
     * @return the number of words
     */
    public int getWords() {
        return words;
    }

    /**
     * @return the number of characters, spaces included, block boundaries not counted
     */
    public int getCharacters() {
        return characters;
    }
}
