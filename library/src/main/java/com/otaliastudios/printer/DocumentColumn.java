package com.otaliastudios.printer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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

    DocumentColumn(@NonNull Context context, int number, int widthBound, int heightBound) {
        super(context);
        mColumnNumber = number;
        mLog = PrinterLogger.create(TAG + "-" + mColumnNumber);
        mWidthBound = widthBound;
        mHeightBound = heightBound;
        setOrientation(VERTICAL);
        addOnLayoutChangeListener(this);
    }

    int getNumber() {
        return mColumnNumber;
    }

    //region Bounds

    void setBounds(int widthBound, int heightBound) {
        mWidthBound = widthBound;
        mHeightBound = heightBound;
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
        // This can be called by child TextViews or EditText that grown
        // to be too small. See DocumentTextHelper.
        // Go out of the layout pass... see onSpaceAvailable
        mLog.w("onSpaceOver:", "one of our children says is smaller than he would like to.");
        post(new Runnable() {
            @Override
            public void run() {
                mLog.v("onSpaceOver:", "Performing.");
                getRoot().onSpaceOver(DocumentColumn.this);
            }
        });
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
        if (!isBounded()) {
            mLog.v("canTake:", "we can, because not bounded.");
            return true;
        }

        int ourHeight = asEmpty ? 0 : getCurrentHeight();

        view.setLayoutParams(params);
        int viewHeight = Utils.tryGetHeight(view, mHeightBound, false);
        if (viewHeight == -1) {
            int wms = View.MeasureSpec.makeMeasureSpec(mWidthBound, View.MeasureSpec.AT_MOST);
            int hms = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            if (params instanceof MarginLayoutParams) {
                measureChildWithMargins(view, wms, 0, hms, 0);
            } else {
                measureChild(view, wms, hms);
            }
            viewHeight = Utils.tryGetHeight(view, mHeightBound, true);
        }

        mLog.v("canTake:", "bound:", mHeightBound, "childHeight:", viewHeight, "columnHeight:", ourHeight);
        // Assuming we have no padding...
        return ourHeight + viewHeight <= mHeightBound;
    }


    private int getCurrentHeight() {
        int height = 0;
        if (false /* getHeight() > 0 && !isLayoutRequested() && !isDirty() */) {
            // Looks like we are not dirt y and have been laid out. Assume tryGetHeight is correct.
            // ^ Removed this cache, it would be cool but is prone to errors.
            height = getHeight();
        } else {
            // All our views are already measured, even before onMeasure,
            // or at least we can desume height from the layout parameters.
            // That's what Utils.tryGetHeight does.
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                height += Utils.tryGetHeight(child, mHeightBound, true);
            }
        }
        return height;
    }

    //endregion

    //region Remove childs that have grown, pass back childs that did shrink

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
        int space = mHeightBound - newHeight;
        final int pn = getRoot().getNumber();
        mLog.v("onLayoutChange:", "page:", pn, "oldHeight:", oldHeight, "newHeight:", newHeight, "space:", space);

        mContentHeight = newHeight;
        if (oldHeight == 0) return; // First pass.
        if (newHeight == oldHeight) return; // Not really changed. This happens.
        if (newHeight > oldHeight) return; // Nothing to do, hope we get calls to notifyTooSmall.
        if (space <= 0) return;

        // Go out of the layout pass, it's not safe to pass views around during layout,
        // even if you use addViewInLayout or removeViewInLayout.
        post(new Runnable() {
            @Override
            public void run() {
                mLog.i("onLayoutChange:", "page:", pn, "dispatching onSpaceAvailable.");
                getRoot().onSpaceAvailable(DocumentColumn.this);
            }
        });
    }

    @Override
    public void release(View view) {
        mLog.v("release:", "releasing view", Utils.mark(view), ". In layout?", isInLayout());
        if (isInLayout()) {
            removeViewInLayout(view);
        } else {
            removeView(view);
        }
    }

    //endregion
}
