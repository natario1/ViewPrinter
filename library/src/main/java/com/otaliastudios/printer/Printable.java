package com.otaliastudios.printer;

/**
 * Generic interface for views that should be printed.
 * If a view in the printed hierarchy implements this interface, the given methods
 * will be called.
 *
 * This gives the opportunity to adjust the visual effects before printing,
 * for instance, removing the red underlines below words in EditTexts.
 */
public interface Printable {

    /**
     * Notifies that a print is going to happen.
     * This is the right moment to release / hide edit features
     * and artifacts.
     */
    void onPrePrint();

    /**
     * Notifies that the print process has ended.
     * Visual artifacts can be restored now.
     */
    void onPostPrint();
}
