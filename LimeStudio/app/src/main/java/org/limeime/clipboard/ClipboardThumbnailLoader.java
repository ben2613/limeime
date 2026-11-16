package org.limeime.clipboard;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Decodes a small clipboard image thumbnail off the main thread.
 */
public final class ClipboardThumbnailLoader {

    public interface Callback {
        void onThumbnail(@Nullable Bitmap bitmap, int requestId);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void load(
            @NonNull Context context,
            @NonNull Uri uri,
            int targetSizePx,
            int requestId,
            @NonNull Callback callback) {
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            Bitmap bitmap = decodeThumbnail(appContext, uri, targetSizePx);
            mainHandler.post(() -> callback.onThumbnail(bitmap, requestId));
        });
    }

    @Nullable
    private static Bitmap decodeThumbnail(@NonNull Context context, @NonNull Uri uri, int targetSizePx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ContentResolver resolver = context.getContentResolver();
                return resolver.loadThumbnail(uri, new Size(targetSizePx, targetSizePx), null);
            } catch (IOException ignored) {
                // fall through
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    int w = info.getSize().getWidth();
                    int h = info.getSize().getHeight();
                    int max = Math.max(w, h);
                    if (max > targetSizePx) {
                        float scale = targetSizePx / (float) max;
                        decoder.setTargetSize(Math.round(w * scale), Math.round(h * scale));
                    }
                });
            } catch (IOException ignored) {
                // fall through
            }
        }
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetSizePx);
            try (InputStream in2 = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in2, null, opts);
            }
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }

    private static int sampleSize(int width, int height, int targetPx) {
        int max = Math.max(width, height);
        if (max <= 0) {
            return 1;
        }
        int sample = 1;
        while (max / sample > targetPx * 2) {
            sample *= 2;
        }
        return sample;
    }
}