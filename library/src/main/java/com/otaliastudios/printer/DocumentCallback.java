package com.otaliastudios.printer;

/**
 * Callbacks for {@link DocumentView} actions happening
 * during the preview.
 */
public interface DocumentCallback {

    /**
     * Notifies that a page was just created, and some content was
     * added to it.
     *
     * @param number the number of the page
     */
    void onPageCreated(int number);

    /**
     * Notifies that a page was just destroyed, possibly because
     * its content was deleted by the user or moved to the previous page.
     *
     * @param number the number of the page
     */
    void onPageDestroyed(int number);

    // void onPageShown(int number);
}
