package com.otaliastudios.printer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("ViewConstructor")
class DocumentPage extends LinearLayout implements Container<DocumentPager, DocumentColumn> {

    private final static String TAG = DocumentPage.class.getSimpleName();
    private PrinterLogger mLog;

    private List<DocumentColumn> mColumns;
    private int mColumnCount;
    private int mPageNumber;

    private int mPageWidth;
    private int mPageHeight;

    DocumentPage(@NonNull Context context, int number, int columns, PrintSize size) {
        super(context);
        setBackgroundColor(Color.WHITE);
        setOrientation(HORIZONTAL);
        mColumnCount = columns;
        mPageNumber = number;
        mLog = PrinterLogger.create(TAG + "-" + mPageNumber);

        // Get our size and the single column size.
        if (size.equals(PrintSize.WRAP_CONTENT)) {
            mPageWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
            mPageHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            mPageWidth = size.widthPixels(context);
            mPageHeight = size.heightPixels(context);
        }
        int[] columnSize = computeColumnSize();

        setWeightSum(mColumnCount);
        mColumns = new ArrayList<>(mColumnCount);
        for (int i = 0; i < mColumnCount; i++) {
            // Columns can be WRAP_CONTENT in height if the page size is WRAP_CONTENT.
            // If the page is not, we must provide and update the exact dimension,
            // to have MATCH_PARENT work in final children.
            // We also want width=0 and gravity=1.
            DocumentColumn column = new DocumentColumn(context, number, i + 1, columnSize[0], columnSize[1]);
            addView(column, new LayoutParams(0, columnSize[1], 1));
            mColumns.add(column);
        }
    }

    private int[] computeColumnSize() {
        int[] size = new int[2];
        if (mPageWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
            size[0] = ViewGroup.LayoutParams.WRAP_CONTENT;
            size[1] = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            int unpaddedWidth = mPageWidth - getPaddingStart() - getPaddingEnd();
            int unpaddedHeight = mPageHeight - getPaddingTop() - getPaddingBottom();
            size[0] = (int) ((float) unpaddedWidth / (float) mColumnCount);
            size[1] = unpaddedHeight;
        }
        return size;
    }

    int getNumber() {
        return mPageNumber;
    }

    void setPageElevation(float elevation) {
        // Set up page elevation. Page must have enough margin to show it.
        if (Build.VERSION.SDK_INT >= 21) setElevation(elevation);
    }

    void setPageInset(int insetStart, int insetTop, int insetEnd, int insetBottom) {
        setPaddingRelative(insetStart, insetTop, insetEnd, insetBottom);
        // This changes the column dimensions. Recompute.
        // The column will request it's own re-layout.
        int[] size = computeColumnSize();
        for (DocumentColumn col : mColumns) {
            col.getLayoutParams().height = size[1];
            col.setBounds(size[0], size[1]);
        }
    }

    void setPageBackground(@Nullable Drawable drawable) {
        if (drawable != null) {
            setBackground(drawable);
        }
    }

    @Override
    public void setElevation(float elevation) {
        super.setElevation(elevation);
        if (getLayoutParams() != null) {
            MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
            params.topMargin = (int) elevation * 3;
            params.leftMargin = (int) elevation * 3;
            params.bottomMargin = (int) elevation * 3;
            params.rightMargin = (int) elevation * 3;
            setLayoutParams(params);
        }
    }

    @Override
    public DocumentPager getRoot() {
        return (DocumentPager) getParent();
    }

    @Override
    public List<DocumentColumn> getChildren() {
        return mColumns;
    }

    @Nullable
    @Override
    public DocumentColumn getSibling(DocumentColumn current) {
        int index = mColumns.indexOf(current);
        int next = index + 1;
        if (next < mColumns.size()) {
            return mColumns.get(next);
        }
        DocumentPage page = getRoot().getSibling(this);
        if (page != null) {
            return page.getChildren().get(0);
        } else {
            return null;
        }
    }

    private int getLastNonEmptyColumn() {
        for (int i = mColumnCount - 1; i >= 0; i--) {
            DocumentColumn col = mColumns.get(i);
            if (col.getViewCount() > 0) {
                return i;
            }
        }
        // All of our columns are empty.
        return -1;
    }

    private int getFirstEmptyColumn() {
        for (int i = 0; i < mColumnCount; i++) {
            DocumentColumn col = mColumns.get(i);
            if (col.getViewCount() == 0) {
                return i;
            }
        }

        // All of our columns have something.
        return -1;
    }

    /**
     * True if some of our columns can take this view.
     * Ideally here we would save *which* column can do that,
     * and use it inside addView. this would save a measure pass.
     */
    @Override
    public boolean canTake(View view, ViewGroup.LayoutParams params, boolean asEmpty) {
        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) return true;
        if (asEmpty) {
            // It doesn't matter which column we use, they are of equal size.
            mLog.v("canTake:", "view:", Utils.mark(view), "as empty.");
            DocumentColumn first = mColumns.get(0);
            return first.canTake(view, params, true);
        } else {

            // Try asking the last non-empty column, that might have some space left.
            int nonEmptyIndex = getLastNonEmptyColumn();
            if (nonEmptyIndex >= 0) {
                DocumentColumn nonEmpty = mColumns.get(nonEmptyIndex);
                mLog.i("canTake:", "view:", Utils.mark(view), "Trying with non empty column:", nonEmpty.getNumber());
                if (nonEmpty.canTake(view, params, false)) return true;
            }

            // Pass to the first empty column, if there is one.
            int emptyIndex = getFirstEmptyColumn();
            if (emptyIndex >= 0) {
                // We have an empty column so this child will go there.
                // But anyway check canTake() before returning, so we can add the Untakable flag.
                DocumentColumn empty = mColumns.get(emptyIndex);
                mLog.i("canTake:", "view:", Utils.mark(view), "Accepting into empty column:", empty.getNumber());
                if (!empty.canTake(view, params, false)) {
                    mLog.w("canTake:", "view:", Utils.mark(view), "is bigger than the column bounds.");
                    Utils.setUntakable(view, true);
                }
                return true;

            } else {
                // We have no more columns.
                return false;
            }
        }
    }

    @Override
    public int getViewCount() {
        int count = 0;
        for (DocumentColumn col : mColumns) {
            count += col.getViewCount();
        }
        return count;
    }

    @Override
    public View getViewAt(int position) {
        int count = 0;
        for (DocumentColumn col : mColumns) {
            int pageCount = col.getViewCount();
            if (position - count < pageCount) {
                return col.getViewAt(position - count);
            }
            count += pageCount;
        }
        return null;
    }

    @Override
    public void takeFirst(View view, ViewGroup.LayoutParams params) {
        mLog.i("takeFirst:", "view:", Utils.mark(view), "dispatching to first column.");
        mColumns.get(0).takeFirst(view, params);
    }

    /**
     * Presumably canTake was called before this, so we must just obey.
     * However, in canTake we return true in two cases. We must check here
     * which was the column that wanted this child.
     *
     * @param view incoming view
     * @param params incoming params
     */
    @Override
    public void take(View view, ViewGroup.LayoutParams params) {
        int nonEmptyIndex = getLastNonEmptyColumn();
        if (nonEmptyIndex >= 0) {
            DocumentColumn nonEmpty = mColumns.get(nonEmptyIndex);
            mLog.i("take:", "view:", Utils.mark(view), "dispatching to non empty", nonEmpty.getNumber());
            if (nonEmpty.canTake(view, params, false)) {
                nonEmpty.take(view, params);
                return;
            }
        }

        int emptyIndex = getFirstEmptyColumn();
        if (emptyIndex >= 0) {
            // Could not take but had views. Pass to the empty one.
            // Swallow if untakable.
            DocumentColumn empty = mColumns.get(emptyIndex);
            mLog.i("take:", "view:", Utils.mark(view), "dispatching to non empty", empty.getNumber());
            empty.take(view, params);
        }

        // It's impossible to end up here, given our canTake implementation.
    }

    @Override
    public List<View> collect() {
        List<View> list = new ArrayList<>();
        for (DocumentColumn col : mColumns) {
            removeView(col);
            list.addAll(col.collect());
        }
        return list;
    }

    @Override
    public void release(View view) {
        for (DocumentColumn col : mColumns) {
            if (col.contains(view)) {
                col.release(view);
                break;
            }
        }
    }

    @Override
    public boolean contains(View view) {
        for (DocumentColumn col : mColumns) {
            if (col.contains(view)) return true;
        }
        return false;
    }

    @Override
    public void onEmpty(DocumentColumn documentColumn) {
        // There's some chance that we are empty.
        int count = getViewCount();
        if (count == 0) {
            getRoot().onEmpty(this);
        }
    }

    @Override
    public void onSpaceAvailable(DocumentColumn child) {
        Utils.clearUntakableView(child);
        mLog.i("onSpaceAvailable:", "fromColumn:", child.getNumber());

        int which = mColumns.indexOf(child);
        boolean first = which == 0;
        boolean last = which == mColumnCount - 1;

        if (!first) {
            mLog.v("onSpaceAvailable:", "trying to pass to column", child.getNumber() - 1);
            DocumentColumn previous = mColumns.get(which - 1);
            while (tryPassFirstViewToPrevious(child, previous)) {} // Try until it stops
        }

        // Check if the nextView page wants to give this page its first child.
        if (!last) {
            mLog.v("onSpaceAvailable:", "trying to accept from column", child.getNumber() + 1);
            DocumentColumn next = mColumns.get(which + 1);
            while (tryPassFirstViewToPrevious(next, child)) {} // try until it stops
        } else {
            getRoot().onSpaceAvailable(this);
        }
    }


    private boolean tryPassFirstViewToPrevious(DocumentColumn current, DocumentColumn previous) {
        final View view = current.getViewCount() == 0 ? null : current.getViewAt(0);
        if (view != null && previous.canTake(view, view.getLayoutParams(), false)) {
            mLog.i("tryPassFirstViewToPrevious:", "passing view", Utils.mark(view),
                    "from", current.getNumber(), "to", previous.getNumber());
            boolean hasFocus = view.hasFocus();
            current.release(view);
            previous.take(view, view.getLayoutParams());
            if (hasFocus) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        view.requestFocus();
                        Utils.showKeyboard(view);
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public void onSpaceOver(DocumentColumn column) {
        // This child was not OK in the column it came from.
        // If this is called, we are probably wrap content.
        if (column.getViewCount() == 0) return; // <- happens during collects..
        mLog.i("onSpaceOver:", "triggered by column", column.getNumber());
        int which = mColumns.indexOf(column);
        boolean last = which == mColumnCount - 1;

        if (last) {
            // This is not our business.
            mLog.i("onSpaceOver:", "last column, passing up to pager.");
            getRoot().onSpaceOver(this);
        } else {
            final View lastView = column.getViewAt(column.getViewCount() - 1);
            // If it is marked as untakable, there is nothing we can do.
            // But it could be a view that just grew to be untakable, so we check if the column
            // could take it if it were empty.
            if (Utils.isUntakable(lastView)) return;
            if (!column.canTake(lastView, lastView.getLayoutParams(), true)) {
                Utils.setUntakable(lastView, true);
                return;
            }


            DocumentColumn next = mColumns.get(which + 1);
            mLog.i("onSpaceOver:", "passing view", Utils.mark(lastView), "to column", next.getNumber());
            boolean hasFocus = lastView.hasFocus();
            column.release(lastView);
            next.takeFirst(lastView, lastView.getLayoutParams());
            if (hasFocus) {
                lastView.post(new Runnable() {
                    @Override
                    public void run() {
                        lastView.requestFocus();
                        Utils.showKeyboard(lastView);
                    }
                });
            }
        }
    }
}
