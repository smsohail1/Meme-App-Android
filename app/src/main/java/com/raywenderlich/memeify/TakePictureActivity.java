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

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import static android.support.v4.content.FileProvider.getUriForFile;

public class TakePictureActivity extends Activity implements View.OnClickListener {

  private static final String MIME_TYPE_IMAGE = "image/";

  private Uri selectedPhotoPath;

  private ImageView takePictureImageView;
  private TextView lookingGoodTextView;

  private static final int TAKE_PHOTO_REQUEST_CODE = 1;
  private boolean pictureTaken;
  private static final String IMAGE_URI_KEY = "IMAGE_URI";
  private static final String BITMAP_WIDTH = "BITMAP_WIDTH";
  private static final String BITMAP_HEIGHT = "BITMAP_HEIGHT";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_take_picture);

    takePictureImageView = (ImageView) findViewById(R.id.picture_imageview);
    takePictureImageView.setOnClickListener(this);

    lookingGoodTextView = (TextView) findViewById(R.id.looking_good_textview);

    Button nextScreenButton = (Button) findViewById(R.id.enter_text_button);
    nextScreenButton.setOnClickListener(this);

    checkReceivedIntent();

  }


  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.picture_imageview:
        takePictureWithCamera();
        break;

      case R.id.enter_text_button:
        moveToNextScreen();
        break;

      default:
        break;
    }
  }

  private void takePictureWithCamera() {
    // 1
    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    // 2
    File imagePath = new File(getFilesDir(), "images");
    File newFile = new File(imagePath, "default_image.jpg");
    if (newFile.exists()) {
      newFile.delete();
    } else {
      newFile.getParentFile().mkdirs();
    }
    selectedPhotoPath = getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile);

    // 3
    captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, selectedPhotoPath);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    } else {
      ClipData clip= ClipData.newUri(getContentResolver(), "A photo", selectedPhotoPath);
      captureIntent.setClipData(clip);
      captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
    startActivityForResult(captureIntent, TAKE_PHOTO_REQUEST_CODE);
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
      setImageViewWithImage();
    }
  }

  private void setImageViewWithImage() {
    takePictureImageView.post(new Runnable() {
      @Override
      public void run() {
        Bitmap pictureBitmap = BitmapResizer.shrinkBitmap(
          TakePictureActivity.this,
          selectedPhotoPath,
          takePictureImageView.getWidth(),
          takePictureImageView.getHeight()
        );
        takePictureImageView.setImageBitmap(pictureBitmap);
      }
    });
    lookingGoodTextView.setVisibility(View.VISIBLE);
    pictureTaken = true;
  }


  private void moveToNextScreen() {
    if (pictureTaken) {
      Intent nextScreenIntent = new Intent(this, EnterTextActivity.class);
      nextScreenIntent.putExtra(IMAGE_URI_KEY, selectedPhotoPath);
      nextScreenIntent.putExtra(BITMAP_WIDTH, takePictureImageView.getWidth());
      nextScreenIntent.putExtra(BITMAP_HEIGHT, takePictureImageView.getHeight());

      startActivity(nextScreenIntent);
    } else {
      Toaster.show(this, R.string.select_a_picture);
    }
  }

  private void checkReceivedIntent() {
    Intent imageRecievedIntent = getIntent();
    String intentAction = imageRecievedIntent.getAction();
    String intentType = imageRecievedIntent.getType();

    if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
      if (intentType.startsWith(MIME_TYPE_IMAGE)) {
        selectedPhotoPath = imageRecievedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        setImageViewWithImage();
      }
    }
  }

}
