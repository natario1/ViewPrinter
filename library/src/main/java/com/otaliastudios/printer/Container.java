package com.otaliastudios.printer;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Internal interface for nested levels of containers
 * (e.g. Document, Pager, Page, Column). They will implement Holder
 * to communicate.
 *
 * @param <Root> the root holder
 * @param <Child> the child holder
 */
interface Container<Root extends Container, Child extends Container> {

    /**
     * Returns our parent container, if any.
     * @return our root
     */
    Root getRoot();

    /**
     * Returns a list of our children containers, if any.
     * If we are at the end of the hierarchy (e.g. a Paragraph), this will be empty.
     *
     * @return a list of children
     */
    List<Child> getChildren();

    /**
     * Returns the sibling of the current child. If it's the last
     * (e.g. last column asking this to its parent page) the request should go up
     * so that we get the first column of the following page.
     *
     * @param current the child asking
     * @return this child next sibling
     */
    @Nullable
    Child getSibling(Child current);

    /**
     * Whether this holder (or one of its children) can take care of
     * the incoming view. At the page level, this depends on the availability of columns.
     * At the column level, this depends on its height, basically.
     *
     * If true is returned, {@link #take(View, ViewGroup.LayoutParams)} will be called.
     *
     * @param view incoming view
     * @param params incoming params
     * @param asEmpty measure as if we had no content
     * @return whether we can take it
     */
    boolean canTake(View view, ViewGroup.LayoutParams params, boolean asEmpty);

    /**
     * Take care of the incoming view, passing to children or laying it down.
     * This means attaching the view at the end of our container.
     *
     * If the view is untakable, it should be marked as such by the caller.
     *
     * @see Utils#UNTAKABLE
     * @param view incoming view
     * @param params incoming params
     */
    void take(View view, ViewGroup.LayoutParams params);

    /**
     * Take care of the incoming view, laying it down at the start of the container.
     * There is no strict need to call {@link #canTake(View, ViewGroup.LayoutParams, boolean)}
     * before this, because we can always take a view at the first position.
     *
     * It is recommended though to at least check if we can take it with the 'asEmpty' flag.
     * If we can't, this means that the view is untakable and should be marked as such by
     * the caller.
     *
     * @see Utils#UNTAKABLE
     * @param view incoming view
     * @param params incoming params
     */
    void takeFirst(View view, ViewGroup.LayoutParams params);

    /**
     * Return the count of the current views, either managed by this container or by
     * children of this container.
     *
     * @return view count
     */
    int getViewCount();

    /**
     * Returns the view at the current position, either in this container or in
     * children of this container.
     *
     * @param position view position
     * @return the view
     */
    View getViewAt(int position);

    /**
     * Dispose the view, releasing from the View hierarchy. It will be passed to
     * a different container, possibly a sibling.
     *
     * @param view view to be released
     */
    void release(View view);

    /**
     * Collect all the views, releasing them from the View hierarchy.
     *
     * @return a list of views
     */
    List<View> collect();

    /**
     * Whether this container holds the given view.
     *
     * @param view view to check
     * @return whether we hold it
     */
    boolean contains(View view);

    /**
     * Our child notifies that he now happens to have some available space that wasn't there
     * before. We can use this for two tasks:
     * - check if the previous child can get some views from {@code child}
     * - check if {@code child} can get some views from the next child
     *
     * @param child the container notifying us
     */
    void onSpaceAvailable(Child child);

    /**
     * Our child notifies that he has no more space, and some of the views would actually
     * like to have more. This basically means that we should try to move some views to a new
     * sibling.
     *
     * @param child the container notifying us
     */
    void onSpaceOver(Child child);

    /**
     * Our child notifies that he has no views left.
     * This is a good time to close it if necessary.
     *
     * @param child the container notifying us
     */
    void onEmpty(Child child);
}
