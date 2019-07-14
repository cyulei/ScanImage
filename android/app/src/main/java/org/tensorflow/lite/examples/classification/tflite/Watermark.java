package org.tensorflow.lite.examples.classification.tflite;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Watermark {
    private float[][] codeArray = null;                    // 最后解码的100位01串

    // 执行时间
    public long excuseTime;
    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel;
    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite;
    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    protected ByteBuffer imgData = null;


    /** Dimensions of inputs. */
    private static final int DIM_BATCH_SIZE = 1;
    // RGB通道
    private static final int DIM_PIXEL_SIZE = 3;

    public Watermark(Activity activity) throws IOException {
        codeArray = new float[1][100];
        tfliteModel = loadModelFile(activity);
        tflite = new Interpreter(tfliteModel);
        imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                                * getImageSizeX()
                                * getImageSizeY()
                                * DIM_PIXEL_SIZE
                                * getNumBytesPerChannel());
        imgData.order(ByteOrder.nativeOrder());
    }

    protected int getNumBytesPerChannel() {
        // 存放float使用4字节
        return 4;
    }

    // 进入模型的图像大小
    public int getImageSizeX() {
        return 400;
    }

    public int getImageSizeY() {
        return 400;
    }

    /** Memory-map the model file in Assets. */
    // 获得解码的模型
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // 获得解码的模型的路径
    private String getModelPath(){
        return "decoder_10.tflite";
    }


    // 输入模型得到输出为float数组0-1
    public float[] recognizeImage(final Bitmap bitmap) {

        // 将图像转为byte
        convertBitmapToByteBuffer(bitmap);

        long startTime = SystemClock.uptimeMillis();
        // 开始执行
        runInference();
        long endTime = SystemClock.uptimeMillis();

        // 执行时间：endTime - startTime
        excuseTime = endTime - startTime;
        return codeArray[0];
    }

    /** Closes the interpreter and model to release resources. */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        tfliteModel = null;
    }

    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();

       // System.out.println( "宽： "+  bitmap.getWidth() + "高： "+  bitmap.getHeight());
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {

                int color = bitmap.getPixel(i, j);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);

                // 提取rgb值
                imgData.putFloat((float)r /255);
                imgData.putFloat((float)g /255);
                imgData.putFloat((float)b /255);
            }
        }
    }

    // 执行模型
    protected void runInference() {
        // 输入与输出
        tflite.run(imgData, codeArray);
    }

}