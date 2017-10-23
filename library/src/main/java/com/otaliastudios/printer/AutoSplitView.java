package com.otaliastudios.printer;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


/**
 * General interface for views that can split their content through
 * multiple pages / columns in order to better layout the content in fixed-size pages
 * or columns.
 *
 * The basic concept is that the view should construct, and hold references to,
 * a chain of clones of itself, and should be able to pass content between two
 * adjacent elements.
 *
 * Take a look at {@link AutoSplitTextView} or {@link AutoSplitEditText}
 * to understand how to better implement this.
 */
public interface AutoSplitView extends Documentable {

    /**
     * Returns the minimum size (height) that a split view can have. For example:
     * - in TextViews, this is the size of a line (plus padding)
     * - in a table, this is the size of a row (plus padding).
     *
     * @return this view minimum size
     */
    int minimumSize();

    /**
     * Reduces its content by (at least) space pixels,
     * if possible. If needed, a new {@link AutoSplitView}
     * can be added to the chain at this point.
     *
     * @param space the minimum amount of space to release by passing it to nextView view
     * @return whether all the space was released
     */
    boolean releaseSpace(int space);

    /**
     * Increases its content by (at most) space pixels,
     * by fetching this content from the nextView {@link AutoSplitView}
     * in the chain, if any.
     *
     * @param space the maximum amount of space to fetch from nextView view
     * @return whether all the space was consumed
     */
    boolean acceptSpace(int space);

    /**
     * The 0-based position of this view inside the chain.
     * A typical implementation would be:
     *
     *     return (previousView() == null) ? 0 : previousView().position() + 1
     *
     * Currently just used for logging.
     *
     * @return the position of this view inside the chain
     */
    int position();

    /**
     * Returns the nextView view in the chain, if any.
     * @return the nextView view
     */
    @Nullable
    AutoSplitView next();

    /**
     * Returns the previousView view in the chain, if any.
     * @return the previousView view
     */
    @Nullable
    AutoSplitView previous();


    /**
     * Creates a copy of this view, to be added to the view chain.
     * @return a copy of this view
     */
    @NonNull
    AutoSplitView split();
}
