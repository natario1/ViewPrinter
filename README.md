[![Build Status](https://travis-ci.org/natario1/ViewPrinter.svg?branch=master)](https://travis-ci.org/natario1/ViewPrinter)

<p align="center">
  <img src="art/logo_400.png" vspace="10" width="250" height="250">
</p>

*The ViewPrinter logo was created inside ViewPrinter itself, with a few minutes and some Canvas drawing.
It's a FloatingActionButton with circles on it, then exported to PNG. See source code in the demo app.*

# ViewPrinter

ViewPrinter lets you preview, edit and print View hierarchies, be it graphics, texts, 
or whatever you can draw in Android.

```groovy
compile 'com.otaliastudios:viewprinter:0.3.0'
```

<p>
  <img src="art/preview_page.png" width="250" vspace="20" hspace="5">
  <img src="art/preview_options.png" width="250" vspace="20" hspace="5">
  <img src="art/preview_logo.png" width="250" vspace="20" hspace="5">
</p>

## Features

- [`DocumentView`](#documentview) : a live preview container for editable, zoomable, pannable views.
- Automatic splitting into separate pages
- Automatic splitting into separate page columns
- [`AutoSplitTextView`](#text-content) and [`AutoSplitEditText`](#text-content) to split text into separate views
- Standardized or custom [`PrintSize`](#printsize)s, or even wrap content
- [`PdfPrinter`](#pdfprinter) prints document to PDF respecting pages
- [`JpegPrinter`](#jpegprinter) and [`PngPrinter`](#pngprinter) to print single pages

ViewPrinter depends on [`natario1/ZoomLayout`](https://github.com/natario1/ZoomLayout): check it out!

## Why

The starting point of this library is that the Android framework is extremely powerful and versatile when drawing.
Android is not a complete word processor, and is not a complete graphic editor. But it is a decent mix between the two,
and with a few Android drawing skills - even just layout - we can draw objects, construct hierarchies,
create complex layouts with dependencies, apply transformations and whatever else we usually do.

This can be an incomplete, but very powerful and versatile tool for document creation, whether its text,
your resume, or a graphical task. The only things lacking, in order to leverage this versatility, are:

- a decent [document preview editor](#live-preview)
- an [easy way to print](#print) the document

This library provides both.

# Docs

#### [Live Preview](#live-preview)

- [`DocumentView`](#documentview)
  - [Free content](#free-content)
  - [Paged content](#paged-content)
  - [Callbacks](#callbacks)
- [Automatic Splitting](#automatic-splitting)
  - [Pagination](#pagination)
  - [Columns](#columns)
  - [Text content](#text-content)
- [Custom Views](#custom-views)

#### [Print](#print)

- [`PrintSize` and Units](#printsize-and-units)
- [Printing documents](#printing-documents)
  - [Permissions](#permissions)
  - [`PdfPrinter`](#pdfprinter)
  - [`PngPrinter`](#pngprinter)
  - [`JpegPrinter`](#jpegprinter)
- [`Printable` and Print Preview](#printable-and-print-preview)



# Live Preview

## DocumentView

```xml
<com.otaliastudios.printer.DocumentView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:pageElevation="0.01in"
    app:pageInsetTop="20pt"
    app:pageInsetBottom="20pt"
    app:pageInsetLeft="30pt"
    app:pageInsetRight="30pt"
    app:pageDividerWidth="0.1in"
    app:columnsPerPage="1"
    app:pageBackground="@color/white"
    app:pagerType="horizontal"
    app:printSize="ISO_A5">

    <!-- Content here. -->

</com.otaliastudios.printer.DocumentView>
```

`DocumentView` offers a live, zoom-and-pannable preview of your View content.
It is actually an instance of [`ZoomLayout`](https://github.com/natario1/ZoomLayout) so
head there to discover more visual APIs.

`DocumentView` can host any kind and number of Android views. Internally, they are laid
out into pages (or columns) that act as a vertical `LinearLayout`, so keep that in mind
when adding childs.

### Free content

The document view can act as if it had no physical boundaries. This can be achieved
by setting the `PrintSize` to `PrintSize.WRAP_CONTENT`, which is a special size designed
for this.

In this case, the content can extend to whatever dimensions you want, and the printed
output file will follow.

```xml
<com.otaliastudios.printer.DocumentView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:printSize="wrapContent">

    <!-- Allowed -->
    <View
        android:layout_width="5000px"
        android:layout_height="5000px"/>

</com.otaliastudios.printer.DocumentView>
```

If your content changes, the page will adapt to it.

### Paged content

Typically you would like to define the output file size (be it PDF or PNG or whatever).
It can be achieved through `app:printSize` or `view.setPrintSize(PrintSize)`, and
by doing this you are implicitly setting a boundary for the page content.

Your page(s) will be laid out as having that dimension. Keep this in mind when adding
your child views:

```xml
<com.otaliastudios.printer.DocumentView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:printSize="ISO_A5">

    <!-- MATCH_PARENT on both: this view will occupy a whole page/column! -->
    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
        
    <!-- MATCH_PARENT on width: this view will fit the page/column width and extend to its height.
         If it happens to be bigger than the page, part of it will be hidden (there's nothing we 
         can do) -->
    <View
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
        
    <!-- WRAP_CONTENT: nothing special, the view wraps content, bounded by the page. -->
    <View
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
    
</com.otaliastudios.printer.DocumentView>
```

### Callbacks

You can be notified of pages creation or destruction by simply setting a `DocumentCallback`:

```java
document.setDocumentCallback(new DocumentCallback() {
    @Override
    public void onPageCreated(int number) {}
    
    @Override
    public void onPageDestroyed(int number) {}
})
```

## Automatic Splitting

The document preview will automatically split the content to make it fit into your page.
The auto-split funcionality acts on three layers:

- Views are distributed over [pages](#pagination)
- Views are distributed over [columns](#columns)
- [Text content](#text-content) is distributed over multiple views

### Pagination

To enable pagination, you simply have to set a [`PrintSize`](#printsize-and-units) that is
different than `PrintSize.WRAP_CONTENT`. In this context, when our views start to go out of
the page boundaries, a new page is created and the view is moved.

It is recommended that you use `DocumentTextView` and `DocumentEditText` for your texts: these
views will notify their parent when they are too small to fit the page. For custom views,
please take a look at the `DocumentHelper` methods.

### Columns

To enable columns, use `document.setColumnsPerPage(int)` or the XML attribute `app:columnsPerPage`.
This will split every page into columns, and we will move Views around if they happen to be
bigger than their available space in the current column.

Again, it is recommended that you use `DocumentTextView` and `DocumentEditText` for your texts.
For other kinds of content, it is your responsibility to ensure that the views are small enough.

### Text Content

We provide support for text splitting, as you can see in the demo app.

- For static text, please use `AutoSplitTextView`
- For editable text, please use `AutoSplitEditText`

These views should work out of the box. When space is not enough, the view will clone itself
into a new child view, trying to keep as much properties as possible (text properties, padding, scale, etc.).
This new view will have no id, but you can use `findViewById(R.id.firstView)` to get the first view
of this chain, and act on that.

|Method|Description|
|------|-----------|
|`setChainText(CharSequence)`|Sets text for the whole chain. This means that it will be redistributed as it should.|
|`getChainText()`|Returns the text for the whole chain - not just the current view.|
|`getFirst()`|Returns the first view of the chain - the original view that started splitting.|
|`next()`|Returns the next view of the chain.|
|`previous()`|Returns the previous view of the chain.|

The `AutoSplitEditText` also supports the `app:chainBackground` XML attribute. It will control
the edit text background, and hide it when the view loses its focus, for example.

## Custom Views

Different views might not be fully supported when there are pages. For example, it's hard to know
if a view is smaller than it would like to be, and it's impossible to split its internal content.
We provide some helpers to address the most common issues.

### Documentable

This is a general interface that might get richer in the future. For now we provide callbacks
for the attach / detach lifecycle of a direct view.

|Method|Description|
|------|-----------|
|`onAttach(int, int)`|Notifies that this view was attached to the given page and column.|
|`onPreDetach()`|Notifies that this view is about to be detached. It can still act on its parent now.|
|`onDetach()`|Notifies that this view has been detached (probably to move it to another page / column).|

### DocumentHelper

The document helper will help your views notify the page when they are smaller than they would like
to be. Just call `DocumentHelper.onLayout(view)` after the view has been laid out. The helper will
check if we need to trigger a re-layout or some movements.

### AutoSplitView

This is, again, a general purpose interface for views that want to implement auto splitting.
The basic example is `AutoSplitTextView` which clones itself to split its content over multiple
views. If you need to implement this, take a look at our implementations and the interface javadocs.

### AutoSplitTextHelper

This helper can help you implement the `AutoSplitView` interface for text views. It's all done -
you just have to delegate the appropriate methods. It is used internally by `AutoSplitTextView` and
`AutoSplitEditText`.

# Print

## PrintSize and Units

The `PrintSize` object is what links the live preview of `DocumentView` to the printers,
defining the actual size of each page.

The `PrintSize` class offers lots of predefined, standardized sizes that you can use for printing
your documents, or a special `WRAP_CONTENT` size that is suitable for one-page content.

You can also define a custom `PrintSize` of your choice, using the static methods:

- `PrintSize.fromMils(int, int)`: sets the content size in *mils*, that is, thousandth-s of an inch.
- `PrintSize.fromPoints(float, float)`: sets the content size in *points*, that is, 72th-s of an inch.
- `PrintSize.fromInches(float, float)`: sets the content size in *inches*.
- `PrintSize.fromMillimeters(int, int)`: sets the content size in *millimeters*.
- `PrintSize.fromPixels(Context, float, float)`: sets the content size in *pixels*. We need a valid context to know the display metrics.

There is no preferable API - it's up to your content and how you want to deal with it.
These units must be considered in XML also when laying out your views, for example:

```xml
android:layout_width="20pt"
android:layout_width="2in"
android:layout_width="2px"
android:textSize="20px"
app:pageInsets="30mm"
```

Depending on what you do, standard units like `sp` and `dp` *might* make no sense here.

## Printing documents

The printing process is extremely simple, and currently requires a non empty `DocumentView` preview
to work. Once you have added your content, simply call the `print(String, File, String)`
method of the printer of your choice. You must pass:

- `String printId`: an identifier for this printing process
- `File file`: a directory where the file will be written
- `String name`: the filename of the output file

The printing action will be executed in a background thread, and you will receive a successful
`PrintCallback` callback on the UI thread once it ended, so you can display your file.

### Permissions

You must have appropriate permissions to write the file in that location.
On Marshmallow+, these permissions must be explicitly asked to the user.

The library currently will automatically ask the `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`
permissions if the output file appears to be in the external storage. Make sure you

- declare these permissions in your manifest
- implement `onRequestPermissionResult` as such:

```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (mPrinter.onRequestPermissionRequest(requestCode, permissions, grantResults)) {
        mPrinter.print(mId, mDirectory, mFilename);
    }
}
```

### PdfPrinter

```java
mPrinter = new PdfPrinter(mDocumentView, mPrintCallback);
mPrinter.setPrintPageBackground(true);
mPrinter.print("id", mFile, "document.pdf");
```

Nothing special. This will create a document with your exact name in the directory, as long as you 
have rights to write there. The output PDF will have as much pages as your live preview.

You can choose whether to include or exclude the page background using `mPrinter.setPrintPageBackground()`
which defaults to `true`.

### PngPrinter

```java
mPrinter = new PngPrinter(mDocumentView, mPrintCallback);
mPrinter.setPrintPageBackground(true);
mPrinter.setPrintPages(PRINT_ALL);
mPrinter.setPrintScale(1f);
mPrinter.print("id", mFile, "my-image");
```

Image printers have a couple differences with the `PdfPrinter`:

- You can choose which pages to print (defaults to all) using `setPrintPages(int...)`.
  If more than one are selected, we will save separate files in your directory, e.g.
  `my-image-1.png`, `my-image-2.png`, `my-image-3.png`.

- This also means that the `PrintCallback` can be called multiple times, each time passing
  the actual image file.
  
- You can downscale the resulting image using `setPrintScale(float)`, defaults to 1. For instance,
  this is useful for caching low-quality previews. A `1000x1000` image with a `0.5` scale will
  result in a `500x500` file.

### JpegPrinter

```java
mPrinter = new JpegPrinter(mDocumentView, mPrintCallback);
mPrinter.setPrintPageBackground(true);
mPrinter.setPrintablePages(PRINT_ALL);
mPrinter.setPrintScale(1f);
mPrinter.setPrintQuality(90);
mPrinter.print("id", mFile, "my-image");
```

On top of the `PngPrinter` functionality, this will let you specify a JPEG compression quality
using `mPrinter.setPrintQuality()`.

## Printable and Print Preview

The `Printable` interface can be implemented by any view in the hierarchy, no matter how deep.
This will let it receive pre- and post-print event notifications, that can help in hiding visual
artifacts that should not make their way to the final page.

For instance, you can use `onPrePrint` to hide the red underlines in text fields.

```java
public interface Printable {

    // View is about to be printed.
    void onPrePrint();

    // View has been printed.
    void onPostPrint();
}
```

The pre-print mode gives an actual preview of the final document. This can be toggled
in the live editor using `documentView.enterPrintPreview()` and `documentView.exitPrintPreview()`.

With these, every `Printable` view in the hierarchy will enter its pre-print / post-print mode.
If there are no `Printable` views, these methods have no noticeable effect.

# Contributions

You are welcome to contribute with suggestions or pull requests. To contact me,
<a href="mailto:mat.iavarone@gmail.com">send an email.</a>
