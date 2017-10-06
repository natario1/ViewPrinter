package com.otaliastudios.printer.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.printer.DocumentView;
import com.otaliastudios.printer.JpegPrinter;
import com.otaliastudios.printer.PdfPrinter;
import com.otaliastudios.printer.PngPrinter;
import com.otaliastudios.printer.PrintCallback;
import com.otaliastudios.printer.PrintSize;
import com.otaliastudios.printer.Printer;
import com.otaliastudios.printer.PrinterLogger;
import com.otaliastudios.zoom.ZoomLogger;

import java.io.File;

// TODO: add insets and dividerWidth
// TODO: show the WRAP_CONTENT size better

public class MainActivity extends AppCompatActivity implements PrintCallback, View.OnClickListener {

    // Pass the mime type as printId so it's easy to open
    private final static String PRINT_JPEG = "image/jpeg";
    private final static String PRINT_PNG = "image/png";
    private final static String PRINT_PDF = "application/pdf";

    private JpegPrinter mJpegPrinter;
    private PngPrinter mPngPrinter;
    private PdfPrinter mPdfPrinter;
    private DocumentView mDocument;
    private String mFilename;
    private File mDirectory;
    private ViewGroup mPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrinterLogger.setLogLevel(PrinterLogger.LEVEL_VERBOSE);
        ZoomLogger.setLogLevel(ZoomLogger.LEVEL_VERBOSE);

        setContentView(R.layout.activity_main);
        mDocument = findViewById(R.id.pdf_preview);
        mJpegPrinter = new JpegPrinter(mDocument, this);
        mPngPrinter = new PngPrinter(mDocument, this);
        mPdfPrinter = new PdfPrinter(mDocument, this);

        mDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        mFilename = "my-document-" + System.currentTimeMillis();

        findViewById(R.id.print_jpeg).setOnClickListener(this);
        findViewById(R.id.print_pdf).setOnClickListener(this);
        findViewById(R.id.print_png).setOnClickListener(this);
        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.logo_prompt).setOnClickListener(this);

        mPanel = findViewById(R.id.controls);
        mPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetBehavior b = BottomSheetBehavior.from(mPanel);
                b.setState(BottomSheetBehavior.STATE_HIDDEN);
                mPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        setupControls();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.print_jpeg: print(PRINT_JPEG); break;
            case R.id.print_pdf: print(PRINT_PDF); break;
            case R.id.print_png: print(PRINT_PNG); break;
            case R.id.edit:
                BottomSheetBehavior b = BottomSheetBehavior.from(mPanel);
                b.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
            case R.id.logo_prompt:
                startActivity(new Intent(this, LogoActivity.class));
                break;
        }
    }

    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(mPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mJpegPrinter.onRequestPermissionRequest(requestCode, permissions, grantResults)) {
            print(PRINT_JPEG);
        } else if (mPngPrinter.onRequestPermissionRequest(requestCode, permissions, grantResults)) {
            print(PRINT_PNG);
        } else if (mPdfPrinter.onRequestPermissionRequest(requestCode, permissions, grantResults)) {
            print(PRINT_PDF);
        }
    }

    private void print(String id) {
        switch (id) {
            case PRINT_JPEG: mJpegPrinter.print(PRINT_JPEG, mDirectory, mFilename); break;
            case PRINT_PNG: mPngPrinter.print(PRINT_PNG, mDirectory, mFilename); break;
            case PRINT_PDF: mPdfPrinter.print(PRINT_PDF, mDirectory, mFilename); break;
        }
    }

    @Override
    public void onPrint(String id, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        intent.setDataAndType(uri, id);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onPrintFailed(String id, Throwable error) {
        Toast.makeText(this, "Got error while printing", Toast.LENGTH_LONG).show();
    }

    private void setupControls() {
        // Columns
        SeekBar col = mPanel.findViewById(R.id.control_columns);
        final TextView colText = mPanel.findViewById(R.id.control_columns_text);
        col.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                int columns = i + 1;
                mDocument.setColumnsPerPage(columns);
                colText.setText("" + columns);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Print size
        SeekBar size = mPanel.findViewById(R.id.control_size);
        final TextView sizeText = mPanel.findViewById(R.id.control_size_text);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                PrintSize s = null;
                String m = null;
                switch (i) {
                    case 0: s = PrintSize.ISO_A4; m = "ISO A4"; break;
                    case 1: s = PrintSize.ISO_A5; m = "ISO A5"; break;
                    case 2: s = PrintSize.ISO_A6; m = "ISO A6"; break;
                    case 3: s = PrintSize.ISO_A7; m = "ISO A7"; break;
                    case 4: s = PrintSize.WRAP_CONTENT; m = "Wrap Content"; break;
                }
                mDocument.setPrintSize(s);
                sizeText.setText(m);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
