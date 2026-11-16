package org.limeime.clipboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable clipboard summary for the idle candidate-strip paste chip (phase A/B).
 */
public final class ClipboardSnapshot {

    public enum Kind {
        EMPTY,
        TEXT,
        URL,
        SENSITIVE,
        /** Image / file URI without pasteable text (phase C+). */
        NON_TEXT
    }

    public static final ClipboardSnapshot EMPTY = new ClipboardSnapshot(Kind.EMPTY, "", "");

    private final Kind kind;
    @NonNull
    private final String displayLabel;
    @NonNull
    private final String pasteText;

    public ClipboardSnapshot(Kind kind, @NonNull String displayLabel, @NonNull String pasteText) {
        this.kind = kind;
        this.displayLabel = displayLabel;
        this.pasteText = pasteText;
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

    public boolean isPasteable() {
        return kind == Kind.TEXT || kind == Kind.URL || kind == Kind.SENSITIVE;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ClipboardSnapshot)) return false;
        ClipboardSnapshot that = (ClipboardSnapshot) o;
        return kind == that.kind
                && displayLabel.equals(that.displayLabel)
                && pasteText.equals(that.pasteText);
    }
}