package org.limeime.clipboard;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Debounced primary-clip listener for the active IME session.
 */
public final class ClipboardMonitor implements ClipboardManager.OnPrimaryClipChangedListener {

    public interface Listener {
        void onClipboardSnapshot(@NonNull ClipboardSnapshot snapshot);
    }

    private static final long DEBOUNCE_MS = 80L;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private ClipboardManager clipboardManager;
    @Nullable
    private Listener listener;
    private final Runnable debouncedRead = new Runnable() {
        @Override
        public void run() {
            deliverSnapshot();
        }
    };

    public ClipboardMonitor(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    public void start(@NonNull Listener listener) {
        this.listener = listener;
        ClipboardManager manager = ContextCompat.getSystemService(appContext, ClipboardManager.class);
        clipboardManager = manager;
        if (manager != null) {
            manager.addPrimaryClipChangedListener(this);
        }
        deliverSnapshot();
    }

    public void stop() {
        if (clipboardManager != null) {
            clipboardManager.removePrimaryClipChangedListener(this);
        }
        clipboardManager = null;
        listener = null;
        mainHandler.removeCallbacks(debouncedRead);
    }

    @NonNull
    public ClipboardSnapshot readSnapshot() {
        return ClipboardInspector.inspect(appContext);
    }

    @Override
    public void onPrimaryClipChanged() {
        mainHandler.removeCallbacks(debouncedRead);
        mainHandler.postDelayed(debouncedRead, DEBOUNCE_MS);
    }

    private void deliverSnapshot() {
        Listener target = listener;
        if (target == null) {
            return;
        }
        target.onClipboardSnapshot(readSnapshot());
    }
}