package com.otaliastudios.printer;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.otaliastudios.printer.view.DocumentEditText;
import com.otaliastudios.printer.view.DocumentTextView;

/**
 * Static utilities for text containers that might become smaller than they would like to,
 * without notifying the parent.
 *
 * It is recommended that they call {@link #onLayout(View)} here to have our splitting
 * policy working.
 *
 * @see DocumentTextView
 * @see DocumentEditText
 */
public class DocumentTextHelper {

    private final static String TAG = DocumentTextHelper.class.getSimpleName();
    private final static PrinterLogger LOG = PrinterLogger.create(TAG);

    public static void onLayout(View view) {
        if (view.getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            LOG.i("onLayout:", "We are wrap content. Looking if we can scroll.");
            // TODO: don't do this if getMaxLines is >= 0 (< MAX_VALUE). Same for getMaxheight.
            if (view.canScrollVertically(-1) || view.canScrollVertically(1)) {
                LOG.w("onLayout:", "We can scroll. Notifying the parent column.");
                notifyContainer(view);
            }
        }
    }

    private static void notifyContainer(View view) {
        Container<?, ?> container = null;
        View current = view;
        while (true) {
            ViewParent parent = current.getParent();
            if (parent == null || !(parent instanceof ViewGroup)) {
                break;
            }

            if (parent instanceof Container) {
                container = (Container) parent;
                break;
            }
            current = (ViewGroup) parent;
        }

        if (container != null) {
            // View smallChild = this
            // View directChild = current
            container.onSpaceOver(null);
        }
    }
}
