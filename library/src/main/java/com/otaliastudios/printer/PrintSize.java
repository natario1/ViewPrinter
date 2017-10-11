package com.otaliastudios.printer;

import android.content.Context;
import android.print.PrintAttributes;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;


/**
 * Class representing the output size of our print process.
 * Can be any of the predefined sizes or a new size with given dimensions.
 *
 * There is a special size, {@link #WRAP_CONTENT}, that will ensure that
 * the output dimensions are those of your content.
 * In that case the document will always have a single page.
 */
public final class PrintSize {

    private final static PrintSize[] SIZES = new PrintSize[45];

    /**
     * A special size meaning that the content will be printed with
     * its current dimensions.
     */
    public final static PrintSize WRAP_CONTENT = new PrintSize(-1, -1);

    public final static PrintSize ISO_A0 = new PrintSize(PrintAttributes.MediaSize.ISO_A0, 0);
    public final static PrintSize ISO_A1 = new PrintSize(PrintAttributes.MediaSize.ISO_A1, 1);
    public final static PrintSize ISO_A2 = new PrintSize(PrintAttributes.MediaSize.ISO_A2, 2);
    public final static PrintSize ISO_A3 = new PrintSize(PrintAttributes.MediaSize.ISO_A3, 3);
    public final static PrintSize ISO_A4 = new PrintSize(PrintAttributes.MediaSize.ISO_A4, 4);
    public final static PrintSize ISO_A5 = new PrintSize(PrintAttributes.MediaSize.ISO_A5, 5);
    public final static PrintSize ISO_A6 = new PrintSize(PrintAttributes.MediaSize.ISO_A6, 6);
    public final static PrintSize ISO_A7 = new PrintSize(PrintAttributes.MediaSize.ISO_A7, 7);
    public final static PrintSize ISO_A8 = new PrintSize(PrintAttributes.MediaSize.ISO_A8, 8);
    public final static PrintSize ISO_A9 = new PrintSize(PrintAttributes.MediaSize.ISO_A9, 9);
    public final static PrintSize ISO_A10 = new PrintSize(PrintAttributes.MediaSize.ISO_A10, 10);

    public final static PrintSize ISO_B0 = new PrintSize(PrintAttributes.MediaSize.ISO_B0, 11);
    public final static PrintSize ISO_B1 = new PrintSize(PrintAttributes.MediaSize.ISO_B1, 12);
    public final static PrintSize ISO_B2 = new PrintSize(PrintAttributes.MediaSize.ISO_B2, 13);
    public final static PrintSize ISO_B3 = new PrintSize(PrintAttributes.MediaSize.ISO_B3, 14);
    public final static PrintSize ISO_B4 = new PrintSize(PrintAttributes.MediaSize.ISO_B4, 15);
    public final static PrintSize ISO_B5 = new PrintSize(PrintAttributes.MediaSize.ISO_B5, 16);
    public final static PrintSize ISO_B6 = new PrintSize(PrintAttributes.MediaSize.ISO_B6, 17);
    public final static PrintSize ISO_B7 = new PrintSize(PrintAttributes.MediaSize.ISO_B7, 18);
    public final static PrintSize ISO_B8 = new PrintSize(PrintAttributes.MediaSize.ISO_B8, 19);
    public final static PrintSize ISO_B9 = new PrintSize(PrintAttributes.MediaSize.ISO_B9, 20);
    public final static PrintSize ISO_B10 = new PrintSize(PrintAttributes.MediaSize.ISO_B10, 21);

    public final static PrintSize ISO_C0 = new PrintSize(PrintAttributes.MediaSize.ISO_C0, 22);
    public final static PrintSize ISO_C1 = new PrintSize(PrintAttributes.MediaSize.ISO_C1, 23);
    public final static PrintSize ISO_C2 = new PrintSize(PrintAttributes.MediaSize.ISO_C2, 24);
    public final static PrintSize ISO_C3 = new PrintSize(PrintAttributes.MediaSize.ISO_C3, 25);
    public final static PrintSize ISO_C4 = new PrintSize(PrintAttributes.MediaSize.ISO_C4, 26);
    public final static PrintSize ISO_C5 = new PrintSize(PrintAttributes.MediaSize.ISO_C5, 27);
    public final static PrintSize ISO_C6 = new PrintSize(PrintAttributes.MediaSize.ISO_C6, 28);
    public final static PrintSize ISO_C7 = new PrintSize(PrintAttributes.MediaSize.ISO_C7, 29);
    public final static PrintSize ISO_C8 = new PrintSize(PrintAttributes.MediaSize.ISO_C8, 30);
    public final static PrintSize ISO_C9 = new PrintSize(PrintAttributes.MediaSize.ISO_C9, 31);
    public final static PrintSize ISO_C10 = new PrintSize(PrintAttributes.MediaSize.ISO_C10, 32);

    public final static PrintSize NA_FOOLSCAP = new PrintSize(PrintAttributes.MediaSize.NA_FOOLSCAP, 33);
    public final static PrintSize NA_GOVT_LETTER = new PrintSize(PrintAttributes.MediaSize.NA_GOVT_LETTER, 34);
    public final static PrintSize NA_INDEX_3X5 = new PrintSize(PrintAttributes.MediaSize.NA_INDEX_3X5, 35);
    public final static PrintSize NA_INDEX_4X6 = new PrintSize(PrintAttributes.MediaSize.NA_INDEX_4X6, 36);
    public final static PrintSize NA_INDEX_5X8 = new PrintSize(PrintAttributes.MediaSize.NA_INDEX_5X8, 37);
    public final static PrintSize NA_JUNIOR_LEGAL = new PrintSize(PrintAttributes.MediaSize.NA_JUNIOR_LEGAL, 38);
    public final static PrintSize NA_LEDGER = new PrintSize(PrintAttributes.MediaSize.NA_LEDGER, 39);
    public final static PrintSize NA_LEGAL = new PrintSize(PrintAttributes.MediaSize.NA_LEGAL, 40);
    public final static PrintSize NA_LETTER = new PrintSize(PrintAttributes.MediaSize.NA_LETTER, 41);
    public final static PrintSize NA_MONARCH = new PrintSize(PrintAttributes.MediaSize.NA_MONARCH, 42);
    public final static PrintSize NA_QUARTO = new PrintSize(PrintAttributes.MediaSize.NA_QUARTO, 43);
    public final static PrintSize NA_TABLOID = new PrintSize(PrintAttributes.MediaSize.NA_TABLOID, 44);

    private int mWidthMils;
    private int mHeightMils;
    private PrintAttributes.MediaSize mSize;

    static PrintSize fromValue(int enumValue) {
        return enumValue == -1 ? WRAP_CONTENT : SIZES[enumValue];
    }

    /**
     * Creates a {@link PrintSize} out of its exact dimensions
     * in mils, meaning thousandth-s of an inch.
     *
     * @param widthMils width in mils
     * @param heightMils height in mils
     * @return a new size
     */
    public static PrintSize fromMils(int widthMils, int heightMils) {
        return new PrintSize(widthMils, heightMils);
    }

    /**
     * Creates a {@link PrintSize} out of its exact dimensions
     * in inches.
     *
     * @param widthInches width in inches
     * @param heightInches height in inches
     * @return a new size
     */
    public static PrintSize fromInches(float widthInches, float heightInches) {
        return new PrintSize((int) (widthInches * INCHES_TO_MILS),
                (int) (heightInches * INCHES_TO_MILS));
    }

    /**
     * Creates a {@link PrintSize} out of its exact dimensions
     * in points.
     *
     * @param widthPoints width in points
     * @param heightPoints height in points
     * @return a new size
     */
    public static PrintSize fromPoints(float widthPoints, float heightPoints) {
        return fromInches(widthPoints * POINTS_TO_INCHES, heightPoints * POINTS_TO_INCHES);
    }

    /**
     * Creates a {@link PrintSize} out of its exact dimensions
     * in millimeters.
     *
     * @param widthMm width in millimeters
     * @param heightMm height in millimeters
     * @return a new size
     */
    public static PrintSize fromMillimeters(int widthMm, int heightMm) {
        return fromInches(widthMm * MM_TO_INCHES, heightMm * MM_TO_INCHES);
    }

    /**
     * Creates a {@link PrintSize} out of its exact dimensions
     * in pixels.
     *
     * @param context a valid context
     * @param widthPixels width in pixels
     * @param heightPixels height in pixels
     * @return a new size
     */
    public static PrintSize fromPixels(Context context, float widthPixels, float heightPixels) {
        return fromInches(widthPixels * PIXELS_TO_INCHES(context),
                heightPixels * PIXELS_TO_INCHES(context));
    }

    private PrintSize(PrintAttributes.MediaSize size, int enumValue) {
        mWidthMils = size.getWidthMils();
        mHeightMils = size.getHeightMils();
        mSize = size;
        SIZES[enumValue] = this;
    }

    private PrintSize(int widthMils, int heightMils) {
        mWidthMils = widthMils;
        mHeightMils = heightMils;
        if (mWidthMils <= 0 || mHeightMils <= 0) {
            if (mWidthMils == -1 && mHeightMils == -1) {
                // Ok, this is WRAP_CONTENT
            } else {
                throw new RuntimeException("Width and height must be > 0");
            }

        }
    }

    /**
     * Returns the width of this size in mils,
     * that is, thousandths of an inch.
     *
     * @return width in mils
     */
    public int widthMils() {
        return mWidthMils;
    }

    /**
     * Returns the height of this size in mils,
     * that is, thousandths of an inch.
     *
     * @return height in mils
     */
    public int heightMils() {
        return mHeightMils;
    }

    /**
     * Returns the width of this size in inches.
     *
     * @return width in inches
     */
    public float widthInches() {
        return (float) widthMils() * MILS_TO_INCHES;
    }

    /**
     * Returns the height of this size in inches.
     *
     * @return height in inches
     */
    public float heightInches() {
        return (float) heightMils() * MILS_TO_INCHES;
    }

    /**
     * Returns the width of this size in points,
     * that is, 72th-s of an inch (1 inch is 72 points).
     *
     * @return width in points
     */
    public int widthPoints() {
        return (int) (widthInches() * INCHES_TO_POINTS);
    }

    /**
     * Returns the height of this size in points,
     * that is, 72th-s of an inch (1 inch is 72 points).
     *
     * @return height in points
     */
    public int heightPoints() {
        return (int) (heightInches() * INCHES_TO_POINTS);
    }

    /**
     * Returns the width of this size in millimeters.
     *
     * @return width in millimeters
     */
    public int widthMillimeters() {
        return (int) (widthInches() * INCHES_TO_MM);
    }

    /**
     * Returns the height of this size in millimeters.
     *
     * @return height in millimeters
     */
    public int heightMillimeters() {
        return (int) (heightInches() * INCHES_TO_MM);
    }

    /**
     * Returns the width of this size in pixels,
     * based on the current display. Throws if this size
     * is equal to {@link #WRAP_CONTENT}.
     *
     * @param context a context
     * @return width in pixels
     */
    public int widthPixels(Context context) {
        if (this.equals(WRAP_CONTENT)) {
            throw new IllegalArgumentException("Cant ask for pixel width on a WRAP_CONTENT size.");
        }
        return (int) (widthInches() * INCHES_TO_PIXELS(context));
    }

    /**
     * Returns the height of this size in pixels,
     * based on the current display. Throws if this size
     * is equal to {@link #WRAP_CONTENT}.
     *
     * @param context a context
     * @return height in pixels
     */
    public int heightPixels(Context context) {
        if (this.equals(WRAP_CONTENT)) {
            throw new IllegalArgumentException("Cant ask for pixel height on a WRAP_CONTENT size.");
        }
        return (int) (heightInches() * INCHES_TO_PIXELS(context));
    }

    PrintAttributes.MediaSize toMediaSize(View content) {
        if (mSize != null) return mSize;
        if (!this.equals(WRAP_CONTENT)) {
            int widthPoints = widthPoints();
            int heightPoints = heightPoints();
            String id = "CustomSize_" + widthPoints + "x" + heightPoints;
            String label = "Custom size: " + widthPoints + " x " + heightPoints + " points";
            return new PrintAttributes.MediaSize(id, label, widthMils(), heightMils());
        }

        // Don't modify our state so we keep being WRAP_CONTENT
        float pixelsToMils = PIXELS_TO_INCHES(content.getContext()) * INCHES_TO_MILS;
        int widthMils = (int) (content.getWidth() * pixelsToMils);
        int heightMils = (int) (content.getHeight() * pixelsToMils);
        return new PrintSize(widthMils, heightMils).toMediaSize(content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PrintSize) {
            PrintSize size = (PrintSize) obj;
            return size.mHeightMils == mHeightMils &&
                    size.mWidthMils == mWidthMils;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{PrintSize: widthMils=" + widthMils() + ", heightMils=" + heightMils() + "}";
    }

    static float INCHES_TO_MILS = 1000f;
    static float MILS_TO_INCHES = 1f / 1000f;
    static float INCHES_TO_POINTS = 72f;
    static float POINTS_TO_INCHES = 1f / 72f;
    static float INCHES_TO_MM = 25.4f;
    static float MM_TO_INCHES = 1f / 25.4f;

    static float INCHES_TO_PIXELS(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 1, metrics);
    }

    static float MM_TO_PIXELS(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, metrics);
    }

    static float PIXELS_TO_INCHES(Context context) {
        return 1f / INCHES_TO_PIXELS(context);
    }

    static float PIXELS_TO_MM(Context context) {
        return 1f / MM_TO_PIXELS(context);
    }

}
