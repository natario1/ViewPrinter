package com.otaliastudios.printer;


import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;

/**
 * A Drawable that will draw nothing but occupy some width
 * or height, depending on its orientation.
 */
class DividerDrawable extends Drawable {

    @Px private int mDimension = 0;
    @DocumentView.PagerType private int mType = -1;

    DividerDrawable(Drawable copy) {
        if (copy instanceof DividerDrawable) {
            DividerDrawable old = (DividerDrawable) copy;
            mDimension = old.mDimension;
            mType = old.mType;
        }
    }

    void setDimension(@Px int dimension) {
        mDimension = dimension;
    }

    void setType(@DocumentView.PagerType int type) {
        mType = type;
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int i) {
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
    }

    @Override
    public void setColorFilter(@ColorInt int color, @NonNull PorterDuff.Mode mode) {
        super.setColorFilter(color, mode);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicWidth() {
        return mType == DocumentView.PAGER_TYPE_HORIZONTAL ? mDimension : 0;
    }

    @Override
    public int getIntrinsicHeight() {
        return mType == DocumentView.PAGER_TYPE_VERTICAL ? mDimension : 0;
    }
}
