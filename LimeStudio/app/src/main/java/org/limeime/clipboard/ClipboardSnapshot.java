package org.limeime.clipboard;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable clipboard summary for the idle candidate-strip paste chip.
 */
public final class ClipboardSnapshot {

    public enum Kind {
        EMPTY,
        TEXT,
        URL,
        SENSITIVE,
        IMAGE,
    }

    public static final ClipboardSnapshot EMPTY =
            new ClipboardSnapshot(Kind.EMPTY, "", "", null, null, null);

    private final Kind kind;
    @NonNull
    private final String displayLabel;
    @NonNull
    private final String pasteText;
    @Nullable
    private final Uri contentUri;
    @Nullable
    private final String mimeType;
    @Nullable
    private final String alternatePasteText;

    public ClipboardSnapshot(
            @NonNull Kind kind,
            @NonNull String displayLabel,
            @NonNull String pasteText) {
        this(kind, displayLabel, pasteText, null, null, null);
    }

    public ClipboardSnapshot(
            @NonNull Kind kind,
            @NonNull String displayLabel,
            @NonNull String pasteText,
            @Nullable Uri contentUri,
            @Nullable String mimeType) {
        this(kind, displayLabel, pasteText, contentUri, mimeType, null);
    }

    public ClipboardSnapshot(
            @NonNull Kind kind,
            @NonNull String displayLabel,
            @NonNull String pasteText,
            @Nullable Uri contentUri,
            @Nullable String mimeType,
            @Nullable String alternatePasteText) {
        this.kind = kind;
        this.displayLabel = displayLabel;
        this.pasteText = pasteText;
        this.contentUri = contentUri;
        this.mimeType = mimeType;
        this.alternatePasteText = alternatePasteText;
    }

    public Kind getKind() {
        return kind;
    }

    @NonNull
    public String getDisplayLabel() {
        return displayLabel;
    }

    @NonNull
    public String getPasteText() {
        return pasteText;
    }

    @Nullable
    public Uri getContentUri() {
        return contentUri;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public boolean isPasteable() {
        switch (kind) {
            case TEXT:
            case URL:
            case SENSITIVE:
                return true;
            case IMAGE:
                return contentUri != null;
            default:
                return false;
        }
    }

    public boolean showThumbnail() {
        return kind == Kind.IMAGE && contentUri != null;
    }

    public boolean hasAlternatePaste() {
        return alternatePasteText != null && !alternatePasteText.isEmpty();
    }

    @Nullable
    public String getAlternatePasteText() {
        return alternatePasteText;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ClipboardSnapshot)) return false;
        ClipboardSnapshot that = (ClipboardSnapshot) o;
        return kind == that.kind
                && displayLabel.equals(that.displayLabel)
                && pasteText.equals(that.pasteText)
                && (contentUri == null ? that.contentUri == null : contentUri.equals(that.contentUri))
                && (mimeType == null ? that.mimeType == null : mimeType.equals(that.mimeType))
                && (alternatePasteText == null
                        ? that.alternatePasteText == null
                        : alternatePasteText.equals(that.alternatePasteText));
    }
}