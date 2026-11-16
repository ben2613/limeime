package org.limeime.clipboard;

import android.view.inputmethod.EditorInfo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClipboardPasteTest {

    @Test
    public void acceptsImageWildcardAndSubtypes() {
        assertTrue(ClipboardPaste.acceptsImageMimeTypes(new String[] { "image/png", "text/plain" }));
        assertTrue(ClipboardPaste.acceptsImageMimeTypes(new String[] { "image/*" }));
    }

    @Test
    public void editorRejectsImageWhenNoContentMimeTypes() {
        assertFalse(ClipboardPaste.editorAcceptsImage(new EditorInfo()));
        assertFalse(ClipboardPaste.acceptsImageMimeTypes(null));
    }
}