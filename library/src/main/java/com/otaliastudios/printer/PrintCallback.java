package com.otaliastudios.printer;

import android.support.annotation.UiThread;

import java.io.File;

/**
 * Callbacks for a print action.
 * They are executed in the UI thread.
 */
public interface PrintCallback {

    /**
     * Notifies that the hierarchy was correctly written to the given file.
     * This might be called multiple times (one per page) depending on the printer.
     *
     * @param id an identifier of the print process
     * @param file a file containing the printed hierarchy
     */
    @UiThread
    void onPrint(String id, File file);

    /**
     * Notifies that the printing execution was blocked due to some error,
     * most likely an {@link java.io.IOException}.
     *
     * @param id an identifier of the print process
     * @param error the cause of the error
     */
    @UiThread
    void onPrintFailed(String id, Throwable error);
}
