package com.otaliastudios.printer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Each column must know exactly its bounds when it is created, or after.
 * This way we can effectively split content based on its height,
 * even if onMeasure was not called.
 * It can be updated 'live' but I'm not sure it is going to work.
 *
 * Bounds are the available dimensions. For a single column page, it's the page size minus
 * the page inset (padding).
 */
class DocumentColumn extends LinearLayout implements Container<DocumentPage, DocumentColumn>,
        View.OnLayoutChangeListener {

    private final static String TAG = DocumentColumn.class.getSimpleName();
    private PrinterLogger mLog;

    private int mWidthBound;
    private int mHeightBound;
    private int mContentHeight;
    private int mColumnNumber;

    private List<Runnable> mPostedActions = new ArrayList<>();

    DocumentColumn(@NonNull Context context, int pageNumber, int number, int widthBound, int heightBound) {
        super(context);
        mColumnNumber = number;
        mWidthBound = widthBound;
        mHeightBound = heightBound;
        setOrientation(VERTICAL);
        addOnLayoutChangeListener(this);
        mLog = PrinterLogger.create(TAG + "-" + pageNumber + "-" + number);
    }

    int getNumber() {
        return mColumnNumber;
    }

    //region Bounds

    void setBounds(int widthBound, int heightBound) {
        mWidthBound = widthBound;
        mHeightBound = heightBound;
        if (isLaidOut()) requestLayout();
    }

    boolean isBounded() {
        return mHeightBound != ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    //endregion

    //region Views


    @Override
    public DocumentPage getRoot() {
        return (DocumentPage) getParent();
    }

    @Override
    public List<DocumentColumn> getChildren() {
        // We have no children.
        return Arrays.asList(this);
    }

    @Nullable
    @Override
    public DocumentColumn getSibling(DocumentColumn current) {
        // We have no children, no one will call this.
        return null;
    }

    @Override
    public int getViewCount() {
        return getChildCount();
    }

    @Override
    public View getViewAt(int position) {
        return getChildAt(position);
    }

    @Override
    public boolean contains(View view) {
        return view.getParent() != null && view.getParent() == this;
    }

    @Override
    public void onSpaceAvailable(DocumentColumn child) {
        // We have no children so no one can call this.
    }

    @Override
    public void onSpaceOver(DocumentColumn child) {
        // We have no children so no one can call this.
    }

    //endregion

    //region add


    @Override
    public void takeFirst(View view, ViewGroup.LayoutParams params) {
        mLog.i("takeFirst:", "view:", Utils.mark(view), "taking.");
        if (isInLayout()) {
            addViewInLayout(view, 0, params);
        } else {
            addView(view, 0, params);
        }
    }

    @Override
    public void take(View view, ViewGroup.LayoutParams params) {
        mLog.i("take:", "view:", Utils.mark(view), "taking.");
        if (isInLayout()) {
            addViewInLayout(view, getChildCount(), params);
        } else {
            addView(view, getChildCount(), params);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof Documentable) {
            Documentable doc = (Documentable) child;
            doc.onAttach(getRoot().getNumber(), getNumber());
        }
    }

    //endregion

    //region canTake?

    /**
     * We know how big we are. Steps should be:
     * - measure the incoming view. How tall will it be?
     * - ensure that height + viewHeight < bound
     *
     * Note: this might be called twice (or more) for the same view,
     * due to page logic. Don't rely on this being called only once per view.
     *
     * @param view can we dispatch this?
     * @return whether we can
     */
    @Override
    public boolean canTake(View view, ViewGroup.LayoutParams params, boolean asEmpty) {
        if (params != view.getLayoutParams()) view.setLayoutParams(params);

        if (!isBounded()) {
            mLog.v("canTake:", "we can, because not bounded.");
            return true;
        }

        int ourHeight = asEmpty ? 0 : getCurrentHeight();

        int viewHeight = Utils.tryGetHeight(view, mHeightBound, false);
        if (viewHeight == -1) {
            if (view instanceof AutoSplitView) {
                // We can take an AutoSplitView if the available space is enough for a single line,
                // that is, we can host the AutoSplitView minimumHeight.
                viewHeight = ((AutoSplitView) view).minimumSize();
                if (params instanceof MarginLayoutParams) {
                    viewHeight += ((MarginLayoutParams) params).topMargin;
                    viewHeight += ((MarginLayoutParams) params).bottomMargin;
                }
            } else {
                measureChildForHeight(view);
                viewHeight = Utils.tryGetHeight(view, mHeightBound, true);
            }
        }

        mLog.v("canTake:", "view:", Utils.mark(view), "bound:", mHeightBound, "childHeight:", viewHeight, "columnHeight:", ourHeight);
        mLog.v("canTake:", "view:", Utils.mark(view), "returning:", ourHeight + viewHeight <= mHeightBound);
        // Assuming we have no padding...
        return ourHeight + viewHeight <= mHeightBound;
    }

    private void measureChildForHeight(View view) {
        int wms = View.MeasureSpec.makeMeasureSpec(mWidthBound, View.MeasureSpec.AT_MOST);
        int hms = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        if (view.getLayoutParams() instanceof MarginLayoutParams) {
            measureChildWithMargins(view, wms, 0, hms, 0);
        } else {
            measureChild(view, wms, hms);
        }
    }

    // This is a delicate point where it's common to make assumptions that are not true.
    // Unfortunately, this is possibly not efficient, but I don't think it can't be better.
    // We are not even sure that all our views were measured: some of them lose their measured values
    // after releasing.
    private int getCurrentHeight() {
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int childHeight = Utils.tryGetHeight(child, mHeightBound, true);
            if (childHeight == -1) {
                // TODO: does this make sense for AutoSplit views?
                measureChildForHeight(child);
                childHeight = Utils.tryGetHeight(child, mHeightBound, true);
            }
            height += childHeight;
        }
        return height;
    }

    //endregion

    //region Remove childs that have grown, pass back childs that did shrink


    // This can be called by child TextViews or EditText that grown
    // to be too small. See DocumentTextHelper.
    // Go out of the layout pass... see onSpaceAvailable
    void requestSpace(final int space) {
        mLog.i("requestSpace:", "a children would like", space, "pixels more.", "Posting.");
        Runnable action = new Runnable() {
            @Override
            public void run() {
                mLog.w("requestSpace:", "a children would like", space, "pixels more.", "Dispatching.");
                if (isAttachedToWindow()) dispatchOnSpaceOver(space);
                mPostedActions.remove(this);
            }
        };
        mPostedActions.add(action);
        post(action);
    }

    /**
     * Something changed here.
     * - did it grow? Do nothing. Due to measuring, we don't let anyone grow beyond the page size.
     * - did it shrink? Notify we have space available.
     */
    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // Our LayoutParams have a fixed height, see DocumentPage.
        // So we are not going to grasp anything meaningful from bottom - top.
        int oldHeight = mContentHeight;
        int newHeight = getCurrentHeight();

        final boolean grown = newHeight > oldHeight;
        final int space = mHeightBound - newHeight;
        mLog.i("onLayoutChange:", "oldHeight:", oldHeight, "newHeight:", newHeight, "space:", space);

        mContentHeight = newHeight;
        if (oldHeight == 0) return; // First pass.
        if (newHeight == oldHeight) return; // Not really changed. This happens.
        if (space == 0) return; // Nothing to dispatch.
        // No other quick end is a good idea, even if it might look so.

        // Go out of the layout pass, it's not safe to pass views around during layout,
        // even if you use addViewInLayout or removeViewInLayout.
        Runnable action = new Runnable() {
            @Override
            public void run() {
                if (space > 0) {
                    mLog.w("onLayoutChange:", "dispatching onSpaceAvailable.", space);
                    if (isAttachedToWindow()) dispatchOnSpaceAvailable(space);
                } else {
                    mLog.w("onLayoutChange:", "dispatching onSpaceOver.", -space);
                    if (isAttachedToWindow()) dispatchOnSpaceOver(-space);
                }
                mPostedActions.remove(this);
            }
        };
        mPostedActions.add(action);
        post(action);
    }

    @Override
    public void release(View view) {
        mLog.v("release:", "releasing view", Utils.mark(view), ". In layout?", isInLayout());
        if (view instanceof Documentable) {
            Documentable doc = (Documentable) view;
            doc.onPreDetach();
        }

        if (isInLayout()) {
            removeViewInLayout(view);
        } else {
            removeView(view);
        }

        if (view instanceof Documentable) {
            Documentable doc = (Documentable) view;
            doc.onDetach();
        }
    }

    @Override
    public List<View> collect() {
        mLog.i("collect:", "collecting");
        List<View> list = new ArrayList<>();
        View view = getViewAt(0);
        while (view != null) {
            release(view);
            list.add(view);
            view = getViewAt(0);
        }
        for (Runnable action : mPostedActions) {
            removeCallbacks(action);
        }
        return list;
    }

    //endregion

    //region Space events

    private void dispatchOnSpaceOver(int space) {
        if (!isBounded()) return;
        View view = getViewAt(getViewCount() - 1);
        if (view instanceof AutoSplitView) {
            AutoSplitView split = (AutoSplitView) view;
            String log = "split: " + Utils.mark(view) + " num: " + split.position();
            mLog.i("dispatchOnSpaceOver", "Dispatching to", log);
            boolean releasedAll = split.releaseSpace(space);

            View next = (View) split.next();
            if (next != null && next.getParent() == null) {
                DocumentColumn sibling = getRoot().getSibling(this);
                mLog.w("dispatchOnSpaceOver", log, "created a new view.",
                        "Passing to page:", sibling.getRoot().getNumber(), "column:", sibling.getNumber());
                sibling.takeFirst(next, next.getLayoutParams());
            }

            if (!releasedAll) {
                mLog.i("dispatchOnSpaceOver", log, "Dispatching to parent because there still is too much space.");
                getRoot().onSpaceOver(this);
            }

        } else {
            mLog.i("dispatchOnSpaceOver", "Dispatching to parent.");
            getRoot().onSpaceOver(this);
        }
    }

    private void dispatchOnSpaceAvailable(int space) {
        if (!isBounded()) return;
        View view = getViewAt(getViewCount() - 1);
        if (view instanceof AutoSplitView) {
            AutoSplitView split = (AutoSplitView) view;
            mLog.i("dispatchOnSpaceAvailable", "Dispatching to AutoSplitView:", split.position());
            boolean acceptedAll = split.acceptSpace(space);
            if (!acceptedAll) {
                mLog.w("dispatchOnSpaceAvailable", "Dispatching to parent because there is still free space.");
                getRoot().onSpaceAvailable(this);
            }
        } else {
            mLog.i("dispatchOnSpaceAvailable", "Dispatching to parent.");
            getRoot().onSpaceAvailable(this);
        }
    }

    //endregion
}
