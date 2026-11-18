package org.limeime.clipboard;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Builds {@link ClipboardSnapshot} from the primary clip (text, URL, image).
 */
public final class ClipboardInspector {

    static final int LABEL_MAX_LENGTH = 32;

    private ClipboardInspector() {
    }

    @NonNull
    public static ClipboardSnapshot inspect(@NonNull Context context) {
        ClipboardManager manager = ContextCompat.getSystemService(context, ClipboardManager.class);
        if (manager == null || !manager.hasPrimaryClip()) {
            return ClipboardSnapshot.EMPTY;
        }
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return ClipboardSnapshot.EMPTY;
        }
        ClipDescription description = clip.getDescription();
        boolean sensitive = isSensitive(description);

        CharSequence text = coercePrimaryText(context, clip);
        if (text != null) {
            String raw = text.toString();
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                if (sensitive) {
                    String label = context.getString(org.limeime.R.string.candidate_clipboard_paste);
                    return new ClipboardSnapshot(ClipboardSnapshot.Kind.SENSITIVE, label, raw);
                }
                if (looksLikeUrl(trimmed)) {
                    String cleaned = UrlSanitizer.sanitize(trimmed);
                    String fixupx = UrlSanitizer.toFixupXContentUrl(trimmed);
                    return new ClipboardSnapshot(
                            ClipboardSnapshot.Kind.URL,
                            ellipsize(cleaned),
                            cleaned,
                            null,
                            null,
                            fixupx);
                }
                return new ClipboardSnapshot(
                        ClipboardSnapshot.Kind.TEXT,
                        ellipsize(trimmed),
                        raw);
            }
        }

        ClipData.Item item = clip.getItemAt(0);
        Uri uri = item.getUri();
        if (uri != null) {
            String mime = resolveImageMime(context, description, uri);
            if (mime != null) {
                String label = sensitive
                        ? context.getString(org.limeime.R.string.candidate_clipboard_paste)
                        : context.getString(org.limeime.R.string.candidate_clipboard_paste_image);
                return new ClipboardSnapshot(
                        ClipboardSnapshot.Kind.IMAGE,
                        label,
                        "",
                        uri,
                        mime);
            }
        }
        return ClipboardSnapshot.EMPTY;
    }

    @Nullable
    private static String resolveImageMime(
            @NonNull Context context,
            @Nullable ClipDescription description,
            @NonNull Uri uri) {
        if (description != null) {
            for (int i = 0; i < description.getMimeTypeCount(); i++) {
                String mime = description.getMimeType(i);
                if (mime != null && mime.startsWith("image/")) {
                    return mime;
                }
            }
        }
        try {
            String type = context.getContentResolver().getType(uri);
            if (type != null && type.startsWith("image/")) {
                return type;
            }
        } catch (SecurityException ignored) {
            return null;
        }
        return null;
    }

    private static boolean isSensitive(@Nullable ClipDescription description) {
        if (description == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PersistableBundle extras = description.getExtras();
            if (extras != null) {
                return extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false);
            }
        }
        return false;
    }

    @Nullable
    private static CharSequence coercePrimaryText(@NonNull Context context, @NonNull ClipData clip) {
        ClipData.Item item = clip.getItemAt(0);
        CharSequence text = item.coerceToText(context);
        if (text != null) {
            return text;
        }
        return item.getText();
    }

    static boolean looksLikeUrl(@NonNull String trimmed) {
        String lower = trimmed.toLowerCase(java.util.Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return true;
        }
        return trimmed.regionMatches(true, 0, "www.", 0, 4);
    }

    @NonNull
    public static String ellipsize(@NonNull String value) {
        if (value.length() <= LABEL_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, LABEL_MAX_LENGTH - 1) + "…";
    }
}