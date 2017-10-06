package com.otaliastudios.printer.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;

import java.util.Random;


public class LogoDrawable extends Drawable {

    private final static int ROWS = 5;
    private final static int COLS = 5;
    private final static Random R = new Random();

    private final Paint mPaint = new Paint();
    private int mWidth;
    private int mHeight;

    private int mGrey;
    private int mColor;
    private int[][] mColors;
    private int[][] mScores;
    private int[][] mCache = new int[ROWS][COLS];

    public LogoDrawable(Context context) {
        mGrey = Color.argb(0, 150, 150, 150);
        mColor = context.getResources().getColor(com.otaliastudios.printer.demo.R.color.colorAccent);
        mColors = new int[][]{
                new int[]{mGrey,  mGrey,  mColor, mGrey,  mGrey},
                new int[]{mGrey,  mColor, mColor, mColor, mGrey},
                new int[]{mColor, mColor, mColor, mColor, mColor},
                new int[]{mGrey,  mColor, mColor, mColor, mGrey},
                new int[]{mGrey,  mGrey,  mColor, mGrey,  mGrey}
        };
        int l = 1;
        int m = 5;
        int h = 6;
        mScores = new int[][]{
                new int[]{l, l, l, l, l},
                new int[]{l, h, m, m, l},
                new int[]{l, m, h, m, l},
                new int[]{l, m, m, m, l},
                new int[]{l, l, l, l, l}
        };
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int i) {}

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        mWidth = right - left;
        mHeight = bottom - top;
    }

    @Override
    public void setBounds(@NonNull Rect bounds) {
        super.setBounds(bounds);
        mWidth = bounds.width();
        mHeight = bounds.height();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int width = mWidth / COLS;
        int height = mHeight / ROWS;
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                int restore = canvas.save();
                canvas.translate(width * col, height * row);
                mPaint.setColor(getColor(row, col));
                canvas.drawCircle(width / 2f, height / 2f, getRadius(row, col), mPaint);
                canvas.restoreToCount(restore);
            }
        }
    }

    private int getScore(int row, int col) {
        return mScores[row][col];
    }

    private float getRadius(int row, int col) {
        int score = getScore(row, col);
        float baseRadius = 0.35f * (float) mWidth / COLS;
        float shift = R.nextFloat() * 20;
        return baseRadius + shift * score;
    }

    private int getColor(int row, int col) {
        if (mCache[row][col] == 0) {
            int score = getScore(row, col);
            int color = mColors[row][col];
            if (color == mGrey) return color;
            float shift = 20;
            int A = 200;
            float shiftR = R.nextFloat() * shift - shift / 2f;
            float shiftG = R.nextFloat() * shift - shift / 2f;
            float shiftB = R.nextFloat() * shift - shift / 2f;
            int R = (int) (Color.red(color) + shiftR * score);
            int G = (int) (Color.green(color) + shiftG * score);
            int B = (int) (Color.blue(color) + shiftB * score);
            R = Math.max(Math.min(255, R), 0);
            G = Math.max(Math.min(255, G), 0);
            B = Math.max(Math.min(255, B), 0);
            mCache[row][col] = Color.argb(A, R, G, B);
        }
        return mCache[row][col];
    }
}
