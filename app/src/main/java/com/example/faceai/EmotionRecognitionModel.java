package com.example.faceai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class EmotionRecognitionModel {
    private final Interpreter interpreter;
    private int INPUT_SIZE;
    private int height=0;
    private int width=0;
    private GpuDelegate gpuDelegate=null;

    private CascadeClassifier cascadeClassifier;
    public EmotionRecognitionModel(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException {
        INPUT_SIZE=inputSize;
        Interpreter.Options options=new Interpreter.Options();
        gpuDelegate=new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4); // set this according to your phone
        interpreter=new Interpreter(loadModelFile(assetManager,modelPath),options);
        Log.d("@@@facial_Expression","@@@Model is loaded");

        try {
            InputStream is=context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir=context.getDir("cascade",Context.MODE_PRIVATE);
            File mCascadeFile=new File(cascadeDir,"haarcascade_frontalface_alt");
            FileOutputStream os=new FileOutputStream(mCascadeFile);
            byte[] buffer=new byte[4096];
            int byteRead;
            while ((byteRead=is.read(buffer)) !=-1){
                os.write(buffer,0,byteRead);
            }
            is.close();
            os.close();
            cascadeClassifier=new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.d("@@@facial_Expression","Classifier is loaded");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public Mat recognizeEmotionLive(Mat mat_image){
        Core.flip(mat_image.t(),mat_image,1);
        Mat grayscaleImage=new Mat();
        Imgproc.cvtColor(mat_image,grayscaleImage,Imgproc.COLOR_RGBA2GRAY);

        height=grayscaleImage.height();
        width=grayscaleImage.width();

        int absoluteFaceSize=(int)(height*0.1);
        MatOfRect faces=new MatOfRect();
        if(cascadeClassifier !=null){

            cascadeClassifier.detectMultiScale(grayscaleImage,faces,1.1,2,2,
                    new Size(absoluteFaceSize,absoluteFaceSize),new Size());
        }

        Rect[] faceArray=faces.toArray();
        for (int i=0;i<faceArray.length;i++){

            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255),2);
            Rect roi=new Rect((int)faceArray[i].tl().x,(int)faceArray[i].tl().y,
                    ((int)faceArray[i].br().x)-(int)(faceArray[i].tl().x),
                    ((int)faceArray[i].br().y)-(int)(faceArray[i].tl().y));

            Mat cropped_rgba=new Mat(mat_image,roi);//

            Bitmap bitmap=null;
            bitmap=Bitmap.createBitmap(cropped_rgba.cols(),cropped_rgba.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba,bitmap);

            Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap,48,48,false);
            ByteBuffer byteBuffer=convertBitmapToByteBuffer(scaledBitmap);

            float[][] emotion=new float[1][1];

            interpreter.run(byteBuffer,emotion);

            float emotion_v=(float)Array.get(Array.get(emotion,0),0);
            Log.d("facial_expression","Output:  "+ emotion_v);
            String emotion_s=get_emotion_text(emotion_v);

            Imgproc.putText(mat_image,emotion_s,
                    new Point((int)faceArray[i].tl().x+10,(int)faceArray[i].tl().y+20),
                    1,1.5,new Scalar(0,0,255,150),2);
        }
        Core.flip(mat_image.t(),mat_image,0);
        return mat_image;
    }

    public Mat recognizeEmotionPhoto(Mat mat_image){

        Mat grayscaleImage=new Mat();
        Imgproc.cvtColor(mat_image,grayscaleImage,Imgproc.COLOR_RGBA2GRAY);
        height=grayscaleImage.height();
        width=grayscaleImage.width();
        int absoluteFaceSize=(int)(height*0.1);
        MatOfRect faces=new MatOfRect();
        if(cascadeClassifier !=null){
            cascadeClassifier.detectMultiScale(grayscaleImage,faces,1.1,2,2,
                    new Size(absoluteFaceSize,absoluteFaceSize),new Size());
        }
        Rect[] faceArray=faces.toArray();
        for (Rect rect : faceArray) {
                       Imgproc.rectangle(mat_image, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 2);
                     Rect roi = new Rect((int) rect.tl().x, (int) rect.tl().y,
                    ((int) rect.br().x) - (int) (rect.tl().x),
                    ((int) rect.br().y) - (int) (rect.tl().y));
            Mat cropped_rgba = new Mat(mat_image, roi);//
            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(cropped_rgba.cols(), cropped_rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba, bitmap);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, false);
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);
            float[][] emotion = new float[1][1];
            interpreter.run(byteBuffer, emotion);
            float emotion_v = (float) Array.get(Objects.requireNonNull(Array.get(emotion, 0)), 0);
            Log.d("@@@facial_expression", "@@@Output:  " + emotion_v);
            String emotion_s = get_emotion_text(emotion_v);

            Imgproc.putText(mat_image, emotion_s,
                    new Point((int) rect.tl().x + 5, (int) rect.tl().y + 50),
                    2, 2, new Scalar(255, 0, 0, 255), 2);
        }
        return mat_image;
    }

    private String get_emotion_text(float emotion_v) {
        String val="";

        if(emotion_v>=0 & emotion_v<0.5){
            val="Surprise";
        }
        else if(emotion_v>=0.5 & emotion_v <1.5){
            val="Fear";
        }
        else if(emotion_v>=1.5 & emotion_v <2.5){
            val="Angry";
        }
        else if(emotion_v>=2.5 & emotion_v <3.5){
            val="Neutral";
        }
        else if(emotion_v>=3.5 & emotion_v <4.5){
            val="Sad";
        }
        else if(emotion_v>=4.5 & emotion_v <5.5){
            val="Disgust";
        }
        else {
            val="Happy";
        }
        return val;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int size_image=INPUT_SIZE;

        byteBuffer=ByteBuffer.allocateDirect(4 * size_image * size_image * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues=new int[size_image*size_image];
        scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());
        int pixel=0;
        for(int i =0;i<size_image;++i){
            for(int j=0;j<size_image;++j){
                final int val=intValues[pixel++];

                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f);
                byteBuffer.putFloat((((val>>8)&0xFF))/255.0f);
                byteBuffer.putFloat(((val & 0xFF))/255.0f);

            }
        }
        return byteBuffer;
    }
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException{
        AssetFileDescriptor assetFileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=assetFileDescriptor.getStartOffset();
        long declaredLength=assetFileDescriptor.getDeclaredLength();
        return  fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);

    }

}
