package com.otaliastudios.printer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.otaliastudios.zoom.ZoomEngine;
import com.otaliastudios.zoom.ZoomLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Displays content in a live, editable, zoomable container based on {@link ZoomLayout}.
 * See that class docs and <a href="https://github.com/natario1/ZoomLayout">repo</a> for documentation
 * about what you can do with the zoom engine.
 *
 * For the content to be displayed it is necessary to know its {@link PrintSize}
 * through {@link #setPrintSize(PrintSize)} or, better, through the XML attribute.
 *
 * Changing the size when the view has been laid out can be an expensive operation, because it requires
 * collecting all the views, removing them, and adding them to a new shaped layout.
 * The same is true for other APIs as well.
 */
public class DocumentView extends ZoomLayout implements View.OnLayoutChangeListener {

    // Internal note: the whole hierarchy currently relies on the fact that all columns are equal
    // Think for example of the Untakable flag which is persisted among columns.

    // TODO: view shadows are not drawn, https://stackoverflow.com/questions/34711211/draw-elevation-shadows-to-canvas
    // TODO: scrollToPage() API

    private final static String TAG = DocumentView.class.getSimpleName();
    private final static PrinterLogger LOG = PrinterLogger.create(TAG);

    /**
     * Lays down pages in a vertical fashion.
     */
    public final static int PAGER_TYPE_VERTICAL = DocumentPager.TYPE_VERTICAL;

    /**
     * Lays down pages in a horizontal fashion.
     */
    public final static int PAGER_TYPE_HORIZONTAL = DocumentPager.TYPE_HORIZONTAL;

    /**
     * An orientation value for the pager to be passed
     * to {@link #setPagerType(int)}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ PAGER_TYPE_HORIZONTAL, PAGER_TYPE_VERTICAL })
    public @interface PagerType {}

    private DocumentPager mPager;
    private PrintSize mSize;
    private View mFocusedView;

    public DocumentView(@NonNull Context context) {
        this(context, null);
    }

    public DocumentView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DocumentView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setHasClickableChildren(true);

        // Steal focus at start-up.
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float defaultElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, metrics);
        int defaultInset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
        int defaultDivider = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, metrics);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DocumentView, defStyleAttr, 0);
        PrintSize size = PrintSize.fromValue(a.getInteger(R.styleable.DocumentView_printSize, -1));
        float elevation = a.getDimension(R.styleable.DocumentView_pageElevation, defaultElevation);
        int inset = a.getDimensionPixelSize(R.styleable.DocumentView_pageInset, defaultInset);
        int insetTop = a.getDimensionPixelSize(R.styleable.DocumentView_pageInsetTop, 0);
        int insetStart = a.getDimensionPixelSize(R.styleable.DocumentView_pageInsetStart, 0);
        int insetEnd = a.getDimensionPixelSize(R.styleable.DocumentView_pageInsetEnd, 0);
        int insetBottom = a.getDimensionPixelSize(R.styleable.DocumentView_pageInsetBottom, 0);
        @PagerType int pagerType = a.getInteger(R.styleable.DocumentView_pagerType, DocumentPager.TYPE_HORIZONTAL);
        int pagerDividerWidth = a.getDimensionPixelSize(R.styleable.DocumentView_pageDividerWidth, defaultDivider);
        int columnsPerPage = a.getInteger(R.styleable.DocumentView_columnsPerPage, 1);
        @Nullable Drawable pageBackground = a.getDrawable(R.styleable.DocumentView_pageBackground);
        insetTop = Math.max(inset, insetTop);
        insetStart = Math.max(inset, insetStart);
        insetEnd = Math.max(inset, insetEnd);
        insetBottom = Math.max(inset, insetBottom);
        a.recycle();

        mPager = new DocumentPager(context);
        addView(mPager, WRAP_CONTENT, WRAP_CONTENT);

        setPageElevation(elevation);
        setPageInset(insetStart, insetTop, insetEnd, insetBottom);
        setPrintSizeInternal(size);
        setPagerType(pagerType);
        setPageDividerWidth(pagerDividerWidth);
        setColumnsPerPage(columnsPerPage);
        setPageBackground(pageBackground);

        // Pass our padding to the frame. This should be the margin between our edges
        // and the edge of the page.
        // TODO: this is only because ZoomLayout does not support it.
        mPager.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        super.setPadding(0, 0, 0, 0);

        // Color our background.
        if (getBackground() == null) {
            a = getContext().getTheme().obtainStyledAttributes(new int[]{ android.R.attr.colorBackground });
            int color = a.getColor(0, Color.LTGRAY);
            setBackgroundColor(color);
            a.recycle();
        }
    }

    /**
     * Sets a {@link DocumentCallback} to be notified of page
     * events like creations and deletions.
     *
     * @param callback the callback
     */
    public void setDocumentCallback(@Nullable DocumentCallback callback) {
        mPager.setDocumentCallback(callback);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child == mPager) {
            super.addView(child, index, params);
        } else if (mPager.canTake(child, params, false)) {
            int count = mPager.getViewCount();
            Utils.mark(child, count + 1);
            mPager.take(child, params);
        }
    }

    /**
     * Sets an orientation value for our pager, either {@link #PAGER_TYPE_VERTICAL}
     * or {@link #PAGER_TYPE_HORIZONTAL}.
     *
     * @param type the desired type
     */
    public void setPagerType(@PagerType int type) {
        mPager.setType(type);
    }

    /**
     * Sets an elevation value for the page, so that it actually looks like a
     * real page. Works only on Lollipop and above.
     *
     * @param elevation the page elevation
     */
    public void setPageElevation(float elevation) {
        mPager.setPageElevation(elevation);
    }

    /**
     * Sets insets to be applied to each page (margins), so that the content
     * will draw in the inner part.
     *
     * @param insetStart the left inset
     * @param insetTop the top inset
     * @param insetEnd the right inset
     * @param insetBottom the bottom inset
     */
    public void setPageInset(@Px int insetStart, @Px int insetTop, @Px int insetEnd, @Px int insetBottom) {
        mPager.setPageInset(insetStart, insetTop, insetEnd, insetBottom);
    }

    /**
     * Sets the width (or height, depending on the orientation) of
     * the divider between pages, to have them visually distant or close.
     *
     * @param width the divider width
     */
    public void setPageDividerWidth(@Px int width) {
        mPager.setPageDividerWidth(width);
    }

    /**
     * Sets the background for each page.
     * If null, goes back to the default (a white color).
     *
     * @param drawable the new page background
     */
    public void setPageBackground(@Nullable Drawable drawable) {
        mPager.setPageBackground(drawable);
    }

    /**
     * Zooms to the real size, where 1 inch of content equals
     * 1 inch of the display.
     *
     * @param animate whether to animate the transition
     */
    public void zoomToRealSize(boolean animate) {
        getEngine().realZoomTo(1, animate);
    }

    /**
     * Sets the columns per page count.
     * If you have a lot of content and pages this can be a pretty expensive operation,
     * because here we need to collect all views, remove them from the window, and re-add
     * them to newly sized pages.
     * It's better to call this at startup or through XML.
     *
     * @param columnsPerPage the new columns per page count (1 ... 4)
     */
    public void setColumnsPerPage(int columnsPerPage) {
        if (columnsPerPage <= 0 || columnsPerPage > 4) {
            throw new RuntimeException("Columns per page must be > 0 and <= 4.");
        }
        if (columnsPerPage > 1 && mSize.equals(PrintSize.WRAP_CONTENT)) {
            throw new RuntimeException("Can't have more than 1 column when size is WRAP_CONTENT.");
        }
        if (columnsPerPage != mPager.getColumnsPerPage()) {
            List<View> collectViews = collectViews();
            mPager.setColumnsPerPage(columnsPerPage);
            for (View view : collectViews) {
                addView(view, view.getLayoutParams());
            }
        }
    }

    /**
     * Sets the current print size.
     * If you have a lot of content and pages this can be a pretty expensive operation,
     * because here we need to collect all views, remove them from the window, and re-add
     * them to newly sized pages.
     * It's better to call this at startup or through XML.
     *
     * @param size the new print size
     */
    public void setPrintSize(@NonNull PrintSize size) {
        if (size.equals(mSize)) return;
        List<View> collectViews = collectViews();
        setPrintSizeInternal(size);
        for (View view : collectViews) {
            addView(view, view.getLayoutParams());
        }
    }

    private List<View> collectViews() {
        int count = mPager.getViewCount();
        List<View> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(null);
        }
        for (int i = count - 1; i >= 0; i--) {
            View view = mPager.getViewAt(i);
            mPager.release(view);
            list.set(i, view);
        }
        return list;
    }

    private void setPrintSizeInternal(PrintSize size) {
        mSize = size;
        if (mSize.equals(PrintSize.WRAP_CONTENT)) {
            mPager.setColumnsPerPage(1);
        }
        mPager.setPrintSize(size);

        ZoomEngine engine = getEngine();
        if (mSize == PrintSize.WRAP_CONTENT) {
            // Max zoom should be a 4:1 ratio, where one content inch is four screen inch.
            // Min zoom here is risky (can be > maxZoom if the content is extremely small).
            engine.setMaxZoom(4, ZoomEngine.TYPE_REAL_ZOOM);
            engine.setMinZoom(1, ZoomEngine.TYPE_ZOOM);
        } else {
            // Max zoom should be a 4:1 ratio, where one content inch is four content inch.
            // In min zoom mode we want to show 2.5 pages. TODO: must know our width for this.
            engine.setMaxZoom(4, ZoomEngine.TYPE_REAL_ZOOM);
            engine.setMinZoom(0.2f, ZoomEngine.TYPE_REAL_ZOOM);
        }
    }

    PrintSize getPrintSize() {
        return mSize;
    }

    DocumentPage getPageAt(int i) {
        return mPager.getPageAt(i);
    }

    /**
     * Returns the current page count.
     * It will always be 1 when the print size is {@link PrintSize#WRAP_CONTENT}.
     *
     * @return the current page count
     */
    public int getPageCount() {
        return mPager.getPageCount();
    }

    @Override
    public void setPadding(@Px int left, @Px int top, @Px int right, @Px int bottom) {
        if (mPager != null) {
            mPager.setPadding(left, top, right, bottom);
        } else {
            // Pass to ourselves. We will remove it in the constructor.
            super.setPadding(left, top, right, bottom);
        }
    }

    //region Focusability (from platform ScrollView)

    @Override
    public void requestChildFocus(View child, View focused) {
        if (focused != null) {
            if (!isInLayout() && !isLayoutRequested()) {
                onFocusChange(focused);
            } else {
                mFocusedView = focused;
            }
        }
        super.requestChildFocus(child, focused);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        // TODO: don't give focus to something that is off screen.
        // Well, theoretically we can, since in onFocusChange we are going to zoom to that view...
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mFocusedView != null) {
            onFocusChange(mFocusedView);
            mFocusedView = null;
        }

        // After our first layout, set focusability to childs again. See constructor.
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    }

    private Rect mTmpRect;

    private void onFocusChange(final View view) {
        if (!(view instanceof TextView)) return;

        // Post does not seem *necessary*, but it doesn't hurt.
        // It's possible that the zoom engine is in a re-layout phase, because
        // maybe we just created a new page. Animating in these cases is risky,
        // for some moments the engine is in a unreliable state.
        postDelayed(new Runnable() {
            @Override
            public void run() {
                zoomToView(view);
                view.removeOnLayoutChangeListener(DocumentView.this);
                view.addOnLayoutChangeListener(DocumentView.this);
            }
        }, 600);
    }

    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (view.isFocused() && view instanceof TextView) {
            // A focused view changed its bounds. Follow it?
            int height = bottom - top;
            int oldHeight = oldBottom - oldTop;
            if (oldHeight != height) {
                zoomToView(view);
            }
        } else {
            view.removeOnLayoutChangeListener(this);
        }
    }

    private void zoomToView(View view) {
        if (mTmpRect == null) mTmpRect = new Rect();
        view.getDrawingRect(mTmpRect);
        offsetDescendantRectToMyCoords(view, mTmpRect);

        ZoomEngine e = getEngine();
        float focusedWidth = mTmpRect.width();
        float focusedHeight = mTmpRect.height();
        float ourWidth = getWidth();
        float ourHeight = getHeight();
        float desiredRealZoom = ourWidth / focusedWidth;
        float desiredZoom = desiredRealZoom * e.getZoom() / e.getRealZoom();

        // Top should be: bottom - Math.min(viewHeight, focusedHeight)
        // At the final zoom, ourWidth == focusedWidth
        float finalViewportHeight = ourHeight * (focusedWidth / ourWidth);
        float panX = -(mTmpRect.left);
        float panY = -(mTmpRect.bottom - Math.min(focusedHeight, finalViewportHeight));
        LOG.i("zoomToView:", "moving to panX:", panX,
                "panY:", panY, "realZoom:", desiredRealZoom);
        e.moveTo(desiredZoom, panX, panY, true);
    }

    @Override
    public void onIdle(ZoomEngine e) {
        super.onIdle(e);
        LOG.i("onIdle", "panX:", e.getPanX(), "panY:", e.getPanY(), "realZoom:", e.getRealZoom());
    }

    //endregion
}
