package com.otaliastudios.printer;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


abstract class BitmapPrinter extends Printer {

    private static final String TAG = BitmapPrinter.class.getSimpleName();
    private static final PrinterLogger LOG = PrinterLogger.create(TAG);

    /**
     * Constant for {@link #setPrintablePages(int...)} to say that we want to print
     * all pages. This is actually the default.
     */
    public static final int PRINTABLE_ALL = -1;

    private Bitmap.CompressFormat mCompressFormat;
    private String mFormat;
    private boolean mPrintAll;
    private int[] mPrintable;

    BitmapPrinter(int permissionCode,
                  @NonNull Bitmap.CompressFormat compressFormat, @NonNull String format,
                  @NonNull DocumentView document, @NonNull PrintCallback callback) {
        super(permissionCode, document, callback);
        mCompressFormat = compressFormat;
        mFormat = format.toLowerCase();
        mPrintAll = true;
    }

    /**
     * Sets the numbers of the pages which should be printed.
     * To print all pages (which is the default), you can pass {@link #PRINTABLE_ALL}
     * as the only parameter.
     *
     * @param pageNumbers any number of pages to be printed
     */
    public final void setPrintablePages(int... pageNumbers) {
        if (pageNumbers.length == 1 && pageNumbers[0] == PRINTABLE_ALL) {
            mPrintAll = true;
        } else {
            mPrintAll = false;
            mPrintable = pageNumbers;
        }
    }

    protected abstract int getQuality();

    @Override
    public void print(final String printId, @NonNull final File directory, @NonNull String filename) {
        Context context = mDocument.getContext();
        if (!checkPermission(context)) return;
        if (!checkPreview(printId, directory, filename)) return;

        if (filename.toLowerCase().endsWith(mFormat)) {
            filename = filename.substring(0, filename.length() - 4);
        }
        if (mDocument.getPageCount() == 0) return;


        final Handler ui = new Handler();
        final HandlerThread thread = new HandlerThread(getClass().getSimpleName() + "Worker");
        thread.start();
        final Handler worker = new Handler(thread.getLooper());

        int count = mPrintAll ? mDocument.getPageCount() : mPrintable.length;
        for (int i = 0; i < count; i++) {
            final int page = mPrintAll ? i : mPrintable[i];
            String suffix = count == 1 ? mFormat : "-" + (page + 1) + mFormat;
            final File file = new File(directory, filename + suffix);
            if (!checkFile(printId, file)) {
                thread.quitSafely();
                return; // Error!
            }

            PrintSize size = mDocument.getPdfSize();
            final Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= 26) {
                bitmap = Bitmap.createBitmap(size.widthPixels(context),
                        size.heightPixels(context), Bitmap.Config.ARGB_8888, true);
            } else {
                bitmap = Bitmap.createBitmap(size.widthPixels(context),
                        size.heightPixels(context), Bitmap.Config.ARGB_8888);
            }
            Canvas canvas = new Canvas(bitmap);
            DocumentPage view = mDocument.getPageAt(page);
            Drawable background = null;
            if (!mPrintBackground) {
                background = view.getBackground();
                view.setBackground(null);
            }

            // Tried this to have shadows drawing but no success.
            // view.setWillNotCacheDrawing(false);
            // view.destroyDrawingCache();
            // view.buildDrawingCache();
            // canvas.drawBitmap(view.getDrawingCache(), 0, 0, null);

            view.draw(canvas);
            if (!mPrintBackground) {
                view.setBackground(background);
            }

            worker.post(new Runnable() {
                @Override
                public void run() {
                    try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
                        bitmap.compress(mCompressFormat, getQuality(), stream);
                        bitmap.recycle();
                        ui.post(new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onPrint(printId, file);
                            }
                        });

                    } catch (final IOException e) {
                        LOG.e("print:", "got error on page:", page, "error:", e);
                        ui.post(new Runnable() {
                            @Override
                            public void run() {
                                Throwable error = new RuntimeException("Invalid file: " + file, e);
                                mCallback.onPrintFailed(printId, error);
                            }
                        });
                    }
                }
            });
        }

        worker.post(new Runnable() {
            @Override
            public void run() {
                LOG.i("print:", "done, closing worker thread.");
                thread.quitSafely();
            }
        });
    }
}
