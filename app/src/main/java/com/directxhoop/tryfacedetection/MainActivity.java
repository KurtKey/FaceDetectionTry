package com.directxhoop.tryfacedetection;

import androidx.appcompat.app.AppCompatActivity;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import com.directxhoop.tryfacedetection.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FaceDetector faceDetector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Context mContext = this;
    private int cameraDirection = CameraSelector.LENS_FACING_FRONT;
    private float scaledLeft ;
    private float scaledRight;
    private float scaledTop;
    private float scaledBottom;


    @SuppressLint("WrongViewCast")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // managing permissions.
        requestForPermission();
        int hasPermission = ContextCompat.checkSelfPermission(mContext,Manifest.permission.CAMERA);
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            // nothing
        }else{
            requestForPermission();
        }

        // we used data binding library so that we minimise code (no need to define layout components here and bind them using findViewById).
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);

        // in case we want to toggle between cameras.
        binding.toggle.setOnClickListener(view -> {
            flipCamera();
        });

        // prepare and start camera
        startCamera();

        // Real-time detection using contour detection (for good experience either we use contour option or landmark+classification ).
        FaceDetectorOptions realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .enableTracking()
                        .build();

        // getting an instance.
        faceDetector =
                FaceDetection.getClient(realTimeOpts);
    }

    // see the comments inside.
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                //here we matched the preview size with the imageAnalysis size using the same .setTargetReso.., 1.5 day so that we found it fuuuu!!.
                .setTargetResolution(new Size(binding.previewView.getWidth(), binding.previewView.getHeight()))
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraDirection) // here we set the camera to be the front by default.
                .build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());


        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // here we matched the preview size with the imageAnalysis size in order to get the rect exactly on the face
                        .setTargetResolution(new Size(binding.previewView.getWidth(), binding.previewView.getHeight()))  // note* : (1080, 2310) my resolution (Huawei nova 7i).
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // while we are using cameraX this guarantees only one image will be delivered for analysis at a time.
                        .build();

        // analysing and processing captured frames and drawing rects on top of detected faces.
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(ImageProxy imageProxy) {
                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();// taking one frame (Image type).
                if (mediaImage != null) {
                    InputImage processImage =
                            InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());// creating an InputImage using that frame.
                            //processing the inputImage.
                            faceDetector
                                    .process(processImage)
                                    .addOnFailureListener(e -> {
                                        Log.v("MainActivity","ERRORrrrr");
                                        imageProxy.close();
                                    }).addOnSuccessListener(results -> {
                                        if (binding.previewView.getChildCount()>1) binding.previewView.removeViewAt(1);// to remove drawn rect before drawing another one
                                        for (Face face : results) {
                                            if (binding.previewView.getChildCount()>1) binding.previewView.removeViewAt(1);// to remove drawn rect before drawing another one
                                            Rect ourRect = face.getBoundingBox();
                                            String text = "";
                                            //get rect scales according to the camera used by the user (front/back).
                                            checkAndset(ourRect.width(),ourRect.height(),ourRect.right,ourRect.left);
                                            //draw the bounding box and text.
                                            Draw element = new Draw(mContext,scaledLeft, ourRect.top, scaledRight,ourRect.bottom,ourRect,text);
                                            binding.previewView.addView(element); // add our drawing (rect and text) on top of the camera preview.
                                        }
                                        imageProxy.close();// close the image being analyzed
                                    });
                }
            }
        });
        cameraProvider.bindToLifecycle(this, cameraSelector,imageAnalysis, preview);
    }

    // ask permissions from user
    public void requestForPermission(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }

    // set rect scales according to the camera used by the user (front/back).
    public void checkAndset(float rectWidth, float rectHight,float rectRight,float rectLeft){

        //to revers box when using front camera
        float flippedLeft;
        float flippedRight;

        float scaleX = binding.previewView.getWidth() / (float) rectWidth;
        /*float scaleY = binding.previewView.getHeight() / (float) rectHight;*/

        //If the front camera lens is being used, reverse the right/left coordinates
        if(cameraDirection == CameraSelector.LENS_FACING_FRONT) {
            /*flippedLeft = bounds.width()-bounds.right+100f;
            flippedRight = bounds.width()-bounds.left-100f;*/
            flippedLeft = rectWidth-rectRight+300f;//300f and 20f are just a temporary hard coded values we found compatible to
            flippedRight = rectWidth-rectLeft-20f;// adjust rect on face when using front camera (just by experiences), let them for later.

            // Scale all coordinates to match preview
            scaledLeft = scaleX * flippedLeft;
            scaledRight = scaleX * flippedRight;
        }
        else {
            flippedLeft = rectLeft;
            flippedRight = rectRight;

            // Scale coordinates to match preview
            scaledLeft = flippedLeft;
            scaledRight = flippedRight;
        }
    }
    //prepare and start cameraX.
    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                bindPreview(cameraProvider);

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(mContext));
    }

    // to toggle between cameras (front/back).
    private void flipCamera() {
        if (cameraDirection == CameraSelector.LENS_FACING_FRONT) cameraDirection = CameraSelector.LENS_FACING_BACK;
        else if (cameraDirection == CameraSelector.LENS_FACING_BACK) cameraDirection = CameraSelector.LENS_FACING_FRONT;
        startCamera();
    }
}