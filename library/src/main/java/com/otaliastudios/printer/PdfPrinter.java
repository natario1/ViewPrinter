package com.otaliastudios.printer;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.support.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A printer instance that can flawlessly print documents preview from {@link DocumentView}
 * to a PDF file - just pass the file to {@link #print(String, File, String)}.
 *
 * The only requirement currently is for the view to be actually attached to a window.
 * The printer will wait for the next layout pass if necessary.
 *
 * The printer will try to automatically request write and read permissions for your file,
 * but you need to pass the result of {@link #onRequestPermissionRequest(int, String[], int[])}
 * from your activity or fragment, and if true, call print again.
 */
public final class PdfPrinter extends Printer {

    private static final int PERMISSION_CODE = 283;
    private static final String TAG = PdfPrinter.class.getSimpleName();

    public PdfPrinter(@NonNull DocumentView document, @NonNull PrintCallback callback) {
        super(PERMISSION_CODE, document, callback);
    }

    /**
     * Prints the current view to a PDF file, in the given directory and with the given
     * filename. If the file exists, it will be deleted.
     *
     * @param printId an (optional) identifier for the process
     * @param directory a directory where the file will be saved
     * @param filename the output file name
     */
    @Override
    public void print(final String printId, @NonNull final File directory, @NonNull String filename) {
        Context context = mDocument.getContext();
        if (!checkPermission(context)) return;
        if (!checkPreview(printId, directory, filename)) return;
        if (!filename.toLowerCase().endsWith(".pdf")) filename += ".pdf";
        final File file = new File(directory, filename);
        if (!checkFile(printId, file)) return;

        if (mDocument.getPageCount() == 0) return;
        DocumentPage firstPage = mDocument.getPageAt(0);
        PrintSize size = mDocument.getPrintSize();

        // Create doc
        PrintAttributes attrs = new PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(size.toMediaSize(firstPage))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        final PrintedPdfDocument doc = new PrintedPdfDocument(context, attrs);

        // Print page
        // Page canvas is passed in PostScript points. In order not to break View drawing,
        // we must scale that up back to pixels.
        for (int i = 0; i < mDocument.getPageCount(); i++) {
            PdfDocument.Page page = doc.startPage(i);
            Canvas canvas = page.getCanvas();
            float pixelsToPoints = PrintSize.PIXELS_TO_INCHES(context) * PrintSize.INCHES_TO_POINTS;
            canvas.scale(pixelsToPoints, pixelsToPoints, 0, 0);

            DocumentPage view = mDocument.getPageAt(i);
            Drawable background = null;
            if (!mPrintBackground) {
                background = view.getBackground();
                view.setBackground(null);
            }
            view.draw(canvas);
            if (!mPrintBackground) {
                view.setBackground(background);
            }
            doc.finishPage(page);
        }

        // I am not sure if the above would work with any view. Some views might be checking for
        // canvas.getWidth() or canvas.tryGetHeight(), which now are not consistent. If errors show up,
        // we might have to draw on a separate canvas and then scale the bitmap.
        // This has other drawbacks: the original pdf canvas, for example, takes text as text and
        // makes it selectable in the final PDF.

        // Print in a separate thread.
        // Since we're API 19 we can use try with resources.
        final Handler ui = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
                    doc.writeTo(stream);
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPrint(printId, file);
                        }
                    });

                } catch (final IOException e) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onPrintFailed(printId, new RuntimeException("Invalid file: " + file, e));
                        }
                    });
                }
            }
        }, TAG + "Worker").start();
    }
}
