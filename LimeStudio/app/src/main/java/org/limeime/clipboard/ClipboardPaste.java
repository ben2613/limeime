package org.limeime.clipboard;

import android.content.ClipDescription;
import android.net.Uri;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

/**
 * Paste clipboard images via {@link InputConnectionCompat#commitContent}.
 */
public final class ClipboardPaste {

    private ClipboardPaste() {
    }

    public static boolean editorAcceptsImage(@Nullable EditorInfo info) {
        if (info == null) {
            return false;
        }
        return acceptsImageMimeTypes(EditorInfoCompat.getContentMimeTypes(info));
    }

    static boolean acceptsImageMimeTypes(@Nullable String[] mimeTypes) {
        if (mimeTypes == null) {
            return false;
        }
        for (String mime : mimeTypes) {
            if (mime == null) {
                continue;
            }
            if ("image/*".equals(mime) || mime.startsWith("image/")) {
                return true;
            }
        }
        return false;
    }

  /**
   * @return true when the editor accepted the image content.
   */
    public static boolean pasteImage(
            @NonNull InputConnection connection,
            @NonNull EditorInfo editorInfo,
            @NonNull Uri uri,
            @Nullable String mimeType) {
        if (!editorAcceptsImage(editorInfo)) {
            return false;
        }
        String mime = mimeType;
        if (mime == null || !mime.startsWith("image/")) {
            mime = "image/png";
        }
        ClipDescription description = new ClipDescription("LIME clipboard", new String[] { mime });
        InputContentInfoCompat content = new InputContentInfoCompat(uri, description, null);
        int flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
        return InputConnectionCompat.commitContent(connection, editorInfo, content, flags, null);
    }
}