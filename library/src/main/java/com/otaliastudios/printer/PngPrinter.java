package com.otaliastudios.printer;


import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.OutputStream;

/**
 * A printer instance that can flawlessly print documents preview from {@link DocumentView}
 * to a PNG file - just pass the file to {@link #print(String, File, String)}.
 *
 * The only requirement currently is for the view to be actually attached to a window.
 * The printer will wait for the next layout pass if necessary.
 *
 * The printer will try to automatically request write and read permissions for your file,
 * but you need to pass the result of {@link #onRequestPermissionRequest(int, String[], int[])}
 * from your activity or fragment, and if true, call print again.
 */
public final class PngPrinter extends BitmapPrinter {

    private static final int PERMISSION_CODE = 284;
    private static final String TAG = PngPrinter.class.getSimpleName();

    public PngPrinter(@NonNull DocumentView document, @NonNull PrintCallback callback) {
        super(PERMISSION_CODE, Bitmap.CompressFormat.PNG, ".png", document, callback);
    }

    @Override
    protected int getQuality() {
        return 100;
    }

    /**
     * Prints the current view to a PNG file, in the given directory and with the given
     * base name. If the document has multiple pages, we will print multiple PNGs by adding
     * count suffix to the file (e.g. file-1.png, file-2.png).
     * This also means that the callback will be called multiple times.
     *
     * If any of the files exist, it will be deleted.
     *
     * @param printId an (optional) identifier for the process
     * @param directory a directory where the file will be saved
     * @param filename the output base name
     */
    @Override
    public void print(String printId, @NonNull File directory, @NonNull String filename) {
        super.print(printId, directory, filename);
    }
}
