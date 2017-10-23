package com.otaliastudios.printer;

/**
 * A documentable view receives special callbacks about its position in the page.
 */
public interface Documentable {

    /*
     * Whether this view can be moved 'forward' to free space.
     * What 'forward' means depends on the context: might be
     * moved to the nextView column, or moved to the nextView page.
     *
     * @return true if this view can be moved.
     */
    // boolean canMoveForward();

    /*
     * Whether this view can be moved 'backward' to free space.
     * What 'backward' means depends on the context: might be
     * moved to the previousView column, or moved to the previousView page.
     *
     * @return true if this view can be moved.
     */
    // boolean canMoveBackward();

    /**
     * Notifies that this view was attached to the given page and column.
     *
     * @param pageNumber the page we were attached to
     * @param columnNumber the column we were attached to
     */
    void onAttach(int pageNumber, int columnNumber);

    /**
     * Notifies that this view was detached from its page and column.
     * Presumably it will be added in a new position,
     * so this view is being passed around to fix the page layout.
     */
    void onDetach();

    /**
     * Notifies that this view is about to be detached from its page and column.
     * Presumably it will be added in a new position.
     *
     * This can be used to do immediate layout changes. For example,
     * {@link AutoSplitView}s use this to collect the chain and remove.
     *
     */
    void onPreDetach();
}
