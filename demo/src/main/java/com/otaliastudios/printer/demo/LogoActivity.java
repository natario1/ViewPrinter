package com.otaliastudios.printer.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.printer.DocumentView;
import com.otaliastudios.printer.JpegPrinter;
import com.otaliastudios.printer.PdfPrinter;
import com.otaliastudios.printer.PngPrinter;
import com.otaliastudios.printer.PrintCallback;
import com.otaliastudios.printer.PrintSize;
import com.otaliastudios.printer.PrinterLogger;
import com.otaliastudios.zoom.ZoomLogger;

import java.io.File;


public class LogoActivity extends AppCompatActivity implements PrintCallback, View.OnClickListener {

    // Pass the mime type as printId so it's easy to open
    private final static String PRINT_PNG = "image/png";

    private PngPrinter mPngPrinter;
    private DocumentView mDocument;
    private String mFilename;
    private File mDirectory;
    private ViewGroup mPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrinterLogger.setLogLevel(PrinterLogger.LEVEL_VERBOSE);
        ZoomLogger.setLogLevel(ZoomLogger.LEVEL_VERBOSE);

        setContentView(R.layout.activity_logo);
        mDocument = findViewById(R.id.preview);
        mDocument.setHasClickableChildren(false);
        mDocument.setPrintSize(PrintSize.fromPixels(this, 1000, 1000));
        // mDocument.setPageInset(100, 100, 100, 100);
        mDocument.setPageInset(0, 0, 0, 0);
        mPngPrinter = new PngPrinter(mDocument, this);
        mPngPrinter.setPrintPageBackground(false);

        mDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        mFilename = "my-logo-" + System.currentTimeMillis();

        ImageView logo = findViewById(R.id.logo);
        logo.setImageDrawable(new LogoDrawable(this));
        findViewById(R.id.print_png).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.print_png: print(PRINT_PNG); break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mPngPrinter.onRequestPermissionRequest(requestCode, permissions, grantResults)) {
            print(PRINT_PNG);
        }
    }

    private void print(String id) {
        switch (id) {
            case PRINT_PNG: mPngPrinter.print(PRINT_PNG, mDirectory, mFilename); break;
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
}
