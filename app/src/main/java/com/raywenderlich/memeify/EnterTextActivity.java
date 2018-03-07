/*
 * Copyright (c) 2017 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.raywenderlich.memeify;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EnterTextActivity extends Activity implements View.OnClickListener {

    private static final String FILE_SUFFIX_JPG = ".jpg";
    private static final String HELVETICA_FONT = "Helvetica";
    private static final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 42;

    private Bitmap viewBitmap;
    private Uri pictureUri;
    private boolean originalImage = false;

    private ImageView selectedPicture;
    private EditText topTextEditText;
    private EditText bottomTextEditText;

    private static final String IMAGE_URI_KEY = "IMAGE_URI";
    private static final String BITMAP_WIDTH = "BITMAP_WIDTH";
    private static final String BITMAP_HEIGHT = "BITMAP_HEIGHT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_text);

        selectedPicture = (ImageView) findViewById(R.id.selected_picture_imageview);

        Button writeTextToImageButton = (Button) findViewById(R.id.write_text_to_image_button);
        writeTextToImageButton.setOnClickListener(this);

        Button saveImageButton = (Button) findViewById(R.id.save_image_button);
        saveImageButton.setOnClickListener(this);

        topTextEditText = (EditText) findViewById(R.id.top_text_edittext);
        bottomTextEditText = (EditText) findViewById(R.id.bottom_text_edittext);

        originalImage = true;

        pictureUri = getIntent().getParcelableExtra(IMAGE_URI_KEY);

        int bitmapWidth = getIntent().getIntExtra(BITMAP_WIDTH, 100);
        int bitmapHeight = getIntent().getIntExtra(BITMAP_HEIGHT, 100);

        Bitmap selectedImageBitmap = BitmapResizer.shrinkBitmap(this, pictureUri,
                bitmapWidth, bitmapHeight);
        selectedPicture.setImageBitmap(selectedImageBitmap);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.write_text_to_image_button:
                createMeme();
                break;
            case R.id.save_image_button:
                askForPermissions();
                break;
        }
    }

    private void askForPermissions() {
        @PermissionChecker.PermissionResult int permissionCheck =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
        } else {
            saveImageToGallery(viewBitmap);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted, yay!
                    saveImageToGallery(viewBitmap);

                } else {
                    Toaster.show(this, R.string.permissions_please);
                }
                break;
            }
        }
    }

    private void createMeme() {
        // Get strings to place into image
        String topText = topTextEditText.getText().toString();
        String bottomText = bottomTextEditText.getText().toString();

        if (!originalImage) {
            Bitmap bm = BitmapResizer.shrinkBitmap(this, pictureUri, selectedPicture.getWidth(), selectedPicture.getHeight());
            selectedPicture.setImageBitmap(bm);
        }

        // get bitmap from imageView and copy to make mutable
        BitmapDrawable imageDrawable = (BitmapDrawable) selectedPicture.getDrawable();
        viewBitmap = imageDrawable.getBitmap();
        viewBitmap = viewBitmap.copy(viewBitmap.getConfig(), true);

        // add the text you want to your bitmap
        addTextToBitmap(viewBitmap, topText, bottomText);

        // set your imageview to show your newly edited bitmap to
        selectedPicture.setImageBitmap(viewBitmap);
        originalImage = false;
    }

    private void addTextToBitmap(Bitmap viewBitmap, String topText, String bottomText) {
        // get dimensions of image
        int bitmapWidth = viewBitmap.getWidth();
        int bitmapHeight = viewBitmap.getHeight();

        // create a canvas that uses the bitmap as its base
        Canvas pictureCanvas = new Canvas(viewBitmap);

        // create paint object with font parameters

        Typeface tf = Typeface.create(HELVETICA_FONT, Typeface.BOLD);

        int textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18,
                getResources().getDisplayMetrics());

        Paint textPaint = new Paint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(tf);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint textPaintOutline = new Paint();
        textPaintOutline.setAntiAlias(true);
        textPaintOutline.setTextSize(textSize);
        textPaintOutline.setColor(Color.BLACK);
        textPaintOutline.setTypeface(tf);
        textPaintOutline.setStyle(Paint.Style.STROKE);
        textPaintOutline.setTextAlign(Paint.Align.CENTER);
        textPaintOutline.setStrokeWidth(8);

        float xPos = bitmapWidth / 2;
        float yPos = bitmapHeight / 7;

        pictureCanvas.drawText(topText, xPos, yPos, textPaintOutline);
        pictureCanvas.drawText(topText, xPos, yPos, textPaint);

        yPos = bitmapHeight - bitmapHeight / 14;

        pictureCanvas.drawText(bottomText, xPos, yPos, textPaintOutline);
        pictureCanvas.drawText(bottomText, xPos, yPos, textPaint);
    }

    private void saveImageToGallery(Bitmap memeBitmap) {
        if (!originalImage) {
            // save bitmap to file
            File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), memeBitmap + FILE_SUFFIX_JPG);

            try {
                // create outputstream, compress image and write to file, flush and close outputstream
                FileOutputStream fos = new FileOutputStream(imageFile);
                memeBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                Toaster.show(this, R.string.save_image_failed);
            }

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(imageFile));
            sendBroadcast(mediaScanIntent);
            Toaster.show(this, R.string.save_image_succeeded);

        } else {
            Toaster.show(this, R.string.add_meme_message);
        }
    }

}
