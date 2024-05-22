package com.example.faceai.activity;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.faceai.R;

import org.opencv.android.OpenCVLoader;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_FROM_GALLERY = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;
    static {
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity: ","Opencv is loaded");
        }
        else {
            Log.d("MainActivity: ","Opencv failed to load");
        }
    }

    private Button camera_button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }


        // Camera Section
        LinearLayout cameraSection = findViewById(R.id.camera_section);
        cameraSection.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CameraViewActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));

        });

        // Gallery Section
        LinearLayout gallerySection = findViewById(R.id.gallery_section);
        gallerySection.setOnClickListener(v -> {

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            // Verify that there are applications to handle each intent
            @SuppressLint("QueryPermissionsNeeded") List<ResolveInfo> cameraActivities = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
            boolean canTakePhoto = !cameraActivities.isEmpty();

            @SuppressLint("QueryPermissionsNeeded") List<ResolveInfo> galleryActivities = getPackageManager().queryIntentActivities(pickPhotoIntent, PackageManager.MATCH_DEFAULT_ONLY);
            boolean canPickPhoto = !galleryActivities.isEmpty();

            // Create a chooser dialog only if there are both options available
            if (canTakePhoto && canPickPhoto) {
                Intent chooserIntent = Intent.createChooser(pickPhotoIntent, "Select Image");
                startActivityForResult(chooserIntent, REQUEST_IMAGE_FROM_GALLERY);
            } else if (canPickPhoto) {
                startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_FROM_GALLERY);
            } else {
                Toast.makeText(MainActivity.this, "No gallery or camera apps available", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_FROM_GALLERY) {
                // Open PhotoViewActivity with the selected image
                Intent intent = new Intent(MainActivity.this, PhotoViewActivity.class);
                intent.setData(data.getData()); // Pass the URI of the selected image
                startActivity(intent);
            }
        }
    }

}