package com.otaliastudios.printer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;


class DocumentPager extends LinearLayout implements Container<DocumentPager, DocumentPage> {

    private final static String TAG = DocumentPager.class.getSimpleName();
    private final static PrinterLogger LOG = PrinterLogger.create(TAG);

    final static int TYPE_VERTICAL = 0;
    final static int TYPE_HORIZONTAL = 1;

    private List<DocumentPage> mPages;
    private boolean mEnabled;

    private float mPageElevation;
    private int mPageInsetStart;
    private int mPageInsetTop;
    private int mPageInsetEnd;
    private int mPageInsetBottom;
    private int mPageColumns = -1;
    private PrintSize mPageSize;

    private DocumentCallback mCallback;
    private Drawable mPageBackground;

    private final Object mLock = new Object();

    public DocumentPager(@NonNull Context context) {
        super(context);
        mPages = new ArrayList<>();
        setShowDividers(SHOW_DIVIDER_MIDDLE);
    }

    private void ensureFirstPage() {
        if (getPageCount() == 0) {
            openPage();
        }
    }

    private boolean openPage() {
        // Can't open page if not enabled.
        LOG.i("openPage:", "there are", getPageCount(), "pages.");
        if (mPageSize == null) throw new RuntimeException("We need a PrintSize set before layout.");
        if (mEnabled || getPageCount() == 0) {
            synchronized (mLock) {
                int index = getPageCount() + 1;
                LOG.w("openPage:", "opening page", index);
                DocumentPage page = new DocumentPage(getContext(), index, mPageColumns, mPageSize);

                int width, height;
                if (mPageSize.equals(PrintSize.WRAP_CONTENT)) {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    // Guaranteed to be > 0.
                    width = mPageSize.widthPixels(getContext());
                    height = mPageSize.heightPixels(getContext());
                }
                ViewGroup.LayoutParams lp = new ViewGroup.MarginLayoutParams(width, height);
                page.setLayoutParams(lp); // View needs lp before elevation is set.
                page.setPageElevation(mPageElevation);
                page.setPageInset(mPageInsetStart, mPageInsetTop, mPageInsetEnd, mPageInsetBottom);
                page.setPageBackground(mPageBackground);
                mPages.add(page);

                if (isInLayout()) {
                    addViewInLayout(page, getChildCount(), lp);
                } else {
                    addView(page, lp);
                }
            }
            LOG.v("openPage:", "dispatching onPageCreated.");
            if (mCallback != null) mCallback.onPageCreated(getPageCount() - 1);
            return true;
        }
        return false;
    }

    private void closePage(DocumentPage page) {
        synchronized (mLock) {
            int number = mPages.indexOf(page);
            LOG.w("closePage:", "closing page", page.getNumber());
            mPages.remove(number);
            if (isInLayout()) {
                removeViewInLayout(page);
            } else {
                removeView(page);
            }
            LOG.v("closePage:", "dispatching onPageDestroyed.");
            if (mCallback != null) mCallback.onPageDestroyed(number);
        }
    }

    private void closeAll() {
        synchronized (mLock) {
            for (int i = getPageCount() - 1; i >= 0; i--) {
                DocumentPage page = getPageAt(i);
                closePage(page);
            }
        }
    }

    void setDocumentCallback(DocumentCallback callback) {
        if (mCallback == null && callback != null) {
            // Dispatch what was not dispatched.
            for (int i = 0; i < getPageCount(); i++) {
                callback.onPageCreated(i);
            }
        }
        mCallback = callback;
    }

    void setType(int type) {
        switch (type) {
            case TYPE_HORIZONTAL: setOrientation(HORIZONTAL); break;
            case TYPE_VERTICAL: setOrientation(VERTICAL); break;
        }

        // Must instantiate a new drawable every time, or LL won't update the dimension.
        DividerDrawable drawable = new DividerDrawable(getDividerDrawable());
        drawable.setType(type);
        setDividerDrawable(drawable);
    }

    int getType() {
        switch (getOrientation()) {
            case HORIZONTAL: return TYPE_HORIZONTAL;
            case VERTICAL: return TYPE_VERTICAL;
        }
        return -1;
    }

    void setPageDividerWidth(int width) {
        // Must instantiate a new drawable every time, or LL won't update the dimension.
        DividerDrawable drawable = new DividerDrawable(getDividerDrawable());
        drawable.setDimension(width);
        setDividerDrawable(drawable);
    }

    void setPageElevation(float elevation) {
        LOG.v("setPageElevation:", elevation);
        if (elevation != mPageElevation) {
            mPageElevation = elevation;
            synchronized (mLock) {
                for (DocumentPage page : mPages) {
                    page.setPageElevation(elevation);
                }
            }
        }
    }

    void setPageInset(int insetStart, int insetTop, int insetEnd, int insetBottom) {
        LOG.v("setPageInset:", insetStart, insetTop, insetEnd, insetBottom);
        if (insetStart != mPageInsetStart || insetTop != mPageInsetTop ||
                insetEnd != mPageInsetEnd || insetBottom != mPageInsetBottom) {
            mPageInsetStart = insetStart;
            mPageInsetTop = insetTop;
            mPageInsetEnd = insetEnd;
            mPageInsetBottom = insetBottom;
            synchronized (mLock) {
                for (DocumentPage page : mPages) {
                    page.setPageInset(insetStart, insetTop, insetEnd, insetBottom);
                }
            }
        }
    }

    void setPageBackground(@Nullable Drawable drawable) {
        mPageBackground = drawable;
        synchronized (mLock) {
            for (DocumentPage page : mPages) {
                page.setPageBackground(drawable);
            }
        }
    }

    // Called when all views have been collected and pages closed.
    void setPrintSize(PrintSize size) {
        LOG.v("setPrintSize:", size);
        closeAll();
        mEnabled = !size.equals(PrintSize.WRAP_CONTENT);
        mPageSize = size;
    }

    // Called when all views have been collected.
    // Close them if needed.
    void setColumnsPerPage(int columnsPerPage) {
        LOG.i("setColumnsPerPage:", columnsPerPage);
        closeAll();
        mPageColumns = columnsPerPage;
    }

    int getColumnsPerPage() {
        return mPageColumns;
    }

    DocumentPage getPageAt(int i) {
        synchronized (mLock) {
            return mPages.get(i);
        }
    }

    int getPageCount() {
        synchronized (mLock) {
            return mPages.size();
        }
    }

    private DocumentPage getLastPage() {
        synchronized (mLock) {
            return mPages.get(getPageCount() - 1);
        }
    }

    @Override
    public DocumentPager getRoot() {
        // We have no root.
        return null;
    }

    @Override
    public List<DocumentPage> getChildren() {
        synchronized (mLock) {
            return mPages;
        }
    }

    @Nullable
    @Override
    public DocumentPage getSibling(DocumentPage current) {
        if (!mEnabled) return null;
        synchronized (mLock) {
            int index = mPages.indexOf(current);
            int next = index + 1;
            if (next < mPages.size()) {
                return mPages.get(next);
            } else {
                openPage();
                return mPages.get(next);
            }
        }
    }

    @Override
    public int getViewCount() {
        int count = 0;
        synchronized (mLock) {
            for (DocumentPage page : mPages) {
                count += page.getViewCount();
            }
        }
        return count;
    }

    @Override
    public View getViewAt(int position) {
        int count = 0;
        synchronized (mLock) {
            for (DocumentPage page : mPages) {
                int pageCount = page.getViewCount();
                if (position - count < pageCount) {
                    return page.getViewAt(position - count);
                }
                count += pageCount;
            }
        }
        return null;
    }

    @Override
    public boolean canTake(View view, ViewGroup.LayoutParams params, boolean asEmpty) {
        // We can always take. Either we are WC, or we open new pages.
        return true;
    }

    @Override
    public void takeFirst(View view, ViewGroup.LayoutParams params) {
        ensureFirstPage();
        LOG.i("takeFirst:", "view:", Utils.mark(view), "dispatching to first page.");
        mPages.get(0).takeFirst(view, params);
    }

    @Override
    public void take(View view, ViewGroup.LayoutParams params) {
        ensureFirstPage();
        DocumentPage page = getLastPage();
        LOG.i("take:", "view:", Utils.mark(view), "dispatching to page", page.getNumber());
        boolean empty = page.getChildCount() == 0;
        if (page.canTake(view, params, false)) {
            page.take(view, params);
        } else if (!empty) {
            // Could not take but had views. Open another.
            LOG.i("take:", "view:", Utils.mark(view), "could not take. Opening another.");
            openPage();
            take(view, params);
        } else {
            // This view is untakable, doesn't fit the page size.
            // Pass anyway, it will be cropped but who cares.
            LOG.w("take:", "view:", Utils.mark(view), "is untakable, appending to page", getLastPage().getNumber());
            Utils.setUntakable(view, true);
            getLastPage().take(view, params);
            LOG.e("take:",
                    "Got a child but it is too tall to fit a single page.",
                    "Please split into multiple childs");
        }
    }

    @Override
    public List<View> collect() {
        List<View> list = new ArrayList<>();
        synchronized (mLock) {
            for (DocumentPage page : mPages) {
                removeView(page);
                list.addAll(page.collect());
            }
        }
        // We are in a illegal state now, but this is followed by
        // something like setPrintSize() which will call closeAll().
        return list;
    }

    @Override
    public void release(View view) {
        synchronized (mLock) {
            for (DocumentPage page : mPages) {
                if (page.contains(view)) {
                    page.release(view);
                    break;
                }
            }
        }
    }

    @Override
    public boolean contains(View view) {
        synchronized (mLock) {
            for (DocumentPage page : mPages) {
                if (page.contains(view)) return true;
            }
        }
        return false;
    }

    @Override
    public void onEmpty(DocumentPage documentPage) {
        int number = mPages.indexOf(documentPage);
        if (number > 0) {
            closePage(documentPage);
        }
    }

    @Override
    public void onSpaceAvailable(DocumentPage child) {
        Utils.clearUntakableView(child);
        LOG.i("onSpaceAvailable:", "fromPage:", child.getNumber());

        synchronized (mLock) {
            int index = mPages.indexOf(child);
            boolean first = index == 0;

            // Check if the previousView page wants our first child.
            // TODO: this is a useless check if the first child was not the one collapsing. !!!
            if (!first) {
                LOG.v("onSpaceAvailable:", "trying to pass to page", child.getNumber() - 1);
                DocumentPage previous = mPages.get(index - 1);

                // Must do a special check for AutoSplit views.
                boolean go = true;
                View view = child.getViewCount() == 0 ? null : child.getViewAt(0);
                if (view instanceof AutoSplitView) {
                    AutoSplitView autoSplitView = (AutoSplitView) view;
                    if (autoSplitView.previous() != null) {
                        go = false;
                    }
                }
                if (go) {
                    while (tryPassFirstViewToPrevious(child, previous)) {}
                }
            }

            // Check if the nextView page wants to give this page its first child.
            index = mPages.indexOf(child);
            boolean last = index == getPageCount() - 1;
            if (index >= 0 && !last) {
                LOG.v("onSpaceAvailable:", "trying to accept from page", child.getNumber() + 1);
                DocumentPage next = mPages.get(index + 1);
                while (tryPassFirstViewToPrevious(next, child)) {}
            }
        }
    }

    private boolean tryPassFirstViewToPrevious(DocumentPage current, DocumentPage previous) {
        final View view = current.getViewCount() == 0 ? null : current.getViewAt(0);
        if (view != null && previous.canTake(view, view.getLayoutParams(), false)) {
            LOG.i("tryPassFirstViewToPrevious:", "passing view", Utils.mark(view),
                    "from", current.getNumber(), "to", previous.getNumber());
            boolean hasFocus = view.hasFocus();
            current.release(view);
            previous.take(view, view.getLayoutParams());
            if (current.getViewCount() == 0) {
                onEmpty(current);
            }
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
    public void onSpaceOver(DocumentPage page) {
        // This child was not OK in the page it came from. Can we open a new one?
        // If this is called, we are NOT wrap content.
        if (!mEnabled) return;
        LOG.i("onSpaceOver:", "triggered by page", page.getNumber());
        final View last = page.getViewAt(page.getViewCount() - 1);

        // If it is marked as untakable, there is nothing we can do.
        // But it could be a view that just grew to be untakable, so we check if the page
        // could take it if it were empty.
        if (Utils.isUntakable(last)) return;
        if (!page.canTake(last, last.getLayoutParams(), true)) {
            Utils.setUntakable(last, true);
            return;
        }

        // Pass to a new page.
        LOG.i("onSpaceOver:", "passing view", Utils.mark(last),
                "to page", page.getNumber() + 1);
        synchronized (mLock) {
            DocumentPage next;
            int which = mPages.indexOf(page);
            int other = which + 1;
            if (other <= getPageCount() - 1) {
                next = mPages.get(which + 1);
            } else {
                openPage();
                next = getLastPage();
            }

            boolean hasFocus = last.hasFocus();
            page.release(last);
            next.takeFirst(last, last.getLayoutParams());
            if (hasFocus) {
                last.post(new Runnable() {
                    @Override
                    public void run() {
                        last.requestFocus();
                        Utils.showKeyboard(last);
                    }
                });
            }
        }
    }
}
