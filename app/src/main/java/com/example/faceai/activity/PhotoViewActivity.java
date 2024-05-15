package com.example.faceai.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.faceai.EmotionDetectionModel;
import com.example.faceai.R;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
public class PhotoViewActivity extends AppCompatActivity {

    private ImageView imageView;
    private EmotionDetectionModel emotionDetectionModel;
    private static final int REQUEST_IMAGE_FROM_GALLERY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        imageView = findViewById(R.id.selected_image);
        Button select_image = findViewById(R.id.uploadButton);

        // Initialize EmotionDetectionModel
        try {
            int inputSize = 48;
            emotionDetectionModel = new EmotionDetectionModel(getAssets(), PhotoViewActivity.this, "model.tflite", inputSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

        select_image.setOnClickListener(view -> {
            // Open the gallery to select another image
            openGallery();
        });

        // Retrieve the selected image URI from the intent
        Uri selectedImageUri = getIntent().getData();
        if (selectedImageUri != null) {
            // Process the selected image
            processSelectedImage(selectedImageUri);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_FROM_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_FROM_GALLERY) {
                // Retrieve the selected image URI
                Uri selectedImageUri = data.getData();
                // Process the selected image
                processSelectedImage(selectedImageUri);
            }
        }
    }

    private void processSelectedImage(Uri selectedImageUri) {
        if (selectedImageUri != null) {
            try {
                // Convert URI to Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                // Convert Bitmap to Mat
                Mat selectedImageMat = new Mat();
                Utils.bitmapToMat(bitmap, selectedImageMat);
                // Perform emotion detection
                Mat resultMat = emotionDetectionModel.recognizePhoto(selectedImageMat);
                // Convert Mat to Bitmap
                Bitmap resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(resultMat, resultBitmap);
                // Display the result in ImageView
                imageView.setImageBitmap(resultBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
