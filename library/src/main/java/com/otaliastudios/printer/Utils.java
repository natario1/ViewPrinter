package com.otaliastudios.printer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

/**
 * Static utilities to pass / store information
 * into view tags.
 */
class Utils {

    private static final int UNTAKABLE = R.id.untakable;
    private static final int VIEW_NUMBER = R.id.viewNumber;
    // static final int WIDTH_BOUND = R.id.widthBound;
    // static final int HEIGHT_BOUND = R.id.heightBound;
    // static final int UNBOUNDED_HEIGHT = R.id.unboundedHeight;

    //region Mark

    // This is just a debug helper, but works only for views added through XML,
    // Programmatically (e.g. AutoSplitView), everything breaks, unless we take care of it.

    static void mark(View view, int number) {
        view.setTag(VIEW_NUMBER, number);
    }

    static int mark(View view) {
        Object tag = view.getTag(VIEW_NUMBER);
        return (tag != null) ? (int) tag : -99;
    }

    //endregion


    //region Untakable

    static void setUntakable(View view, boolean untakable) {
        view.setTag(UNTAKABLE, untakable);
    }

    static boolean isUntakable(View view) {
        Boolean bigger = (Boolean) view.getTag(UNTAKABLE);
        return bigger != null && bigger;
    }

    static boolean hasUntakableView(Container container) {
        return container.getViewCount() == 1 && isUntakable(container.getViewAt(0));
    }

    static void clearUntakableView(Container container) {
        for (int i = 0; i < container.getViewCount(); i++) {
            View view = container.getViewAt(i);
            if (isUntakable(view)) setUntakable(view, false);
        }
    }

    //endregion

    //region Height

    /**
     * Returns this child height, or -1 if it could not be computed.
     * The height can be computed from:
     * - measured height: if the getMeasured flag is true
     * - estimated height: if this view has simple LayoutParams
     *
     * If both checks do fail, the view must be measured again, and we return -1.
     *
     * TODO: add a third check that acts as a kind of cache using view tags.
     * We must be sure to invalidate the cache at the right time though...
     * The view might change, it's layout params might change the margins...
     *
     * @param child to be measured
     * @param heightBound a vertical boundary
     * @param getMeasuredIfAny whether we accept cached measured values
     * @return height estimation or -1
     */
    static int tryGetHeight(View child, int heightBound, boolean getMeasuredIfAny) {
        int height = -1;
        // Order is important. We don't want to trust measuredHeight if it belonged to a different page size.
        if (height == -1) height = getEstimatedHeight(child, heightBound);
        if (height == -1 && getMeasuredIfAny) height = getMeasuredHeight(child);
        return height;
    }

    private static int getMeasuredHeight(View child) {
        if (child.getMeasuredHeight() > 0) {
            int height = 0;
            height += child.getMeasuredHeight();
            if (child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                height += margins.topMargin;
                height += margins.bottomMargin;
            }
            return height;
        }
        return -1;
    }

    // This might change the internal getMeasuredState(). We should remeasure with old specs..
    private static int getEstimatedHeight(View child, int heightBound) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) return heightBound;

        int margin = 0;
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            margin += mlp.topMargin;
            margin += mlp.bottomMargin;
        }
        if (lp.height >= 0) return lp.height + margin;
        return -1;

    }

    //endregion

    //region Keyboard

    static void showKeyboard(View view) {
        if (view instanceof TextView) {
            view.requestFocus();
            Context c = view.getContext();
            InputMethodManager i = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (i != null) i.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    //endregion

}
