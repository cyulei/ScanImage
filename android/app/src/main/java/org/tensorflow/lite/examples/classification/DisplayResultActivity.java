package org.tensorflow.lite.examples.classification;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.examples.classification.tflite.Watermark;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;


public class DisplayResultActivity extends AppCompatActivity {
    private Watermark watermark;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);

        // 获取一些UI组件
        ImageView imageView = (ImageView)findViewById(R.id.result_img);
        TextView result = (TextView)findViewById(R.id.result_code);
        TextView time = (TextView)findViewById(R.id.time);

        // 播放声音
        playBee(DisplayResultActivity.this);

        // 获取前一个活动传过来的图片信息
        Intent intent = getIntent();
        byte buff[] = intent.getByteArrayExtra(MainActivity.EXTRA_MESSAGE);
        Bitmap bitmap = BitmapFactory.decodeByteArray(buff, 0, buff.length);
        imageView.setImageBitmap(bitmap);

        try {
            watermark = new Watermark(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 进行识别
        float[] results = watermark.recognizeImage(bitmap);
        int []resultInt = new int[100];
        String text = "";
        // 获取识别出的01串 使用round取整
        for(int i = 1; i <= results.length;i ++)
        {
            // 将得到的100个浮点数取整
            resultInt[i-1] = Math.round(results[i-1]);
            text += Integer.toString(resultInt[i-1]);
            if(i % 20 == 0){
                text += "\n";
            }
        }
        result.setText(text);

        // 设置时间
        String stringTime = Integer.toString((int)watermark.excuseTime) + " ms";
        time.setText(stringTime);


        // 以下方法会出现闪退现象
        // Get the Intent
//        Intent intent = getIntent();
//        long addr = intent.getLongExtra(MainActivity.EXTRA_MESSAGE, 0);
//        Mat tempImg = new Mat(addr);
//        Log.d("Display", "get derr content");
//        // Cloning Mat in child is necessary since parent activity could be killed
//        Mat img = tempImg.clone();
//        Log.d("Display", "get derr content after clone");

        // Display the image
        // reference: https://stackoverflow.com/questions/44579822/convert-opencv-mat-to-android-bitmap
//        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
//        Bitmap resultBitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(img, resultBitmap);
//        ImageView imageView = (ImageView)findViewById(R.id.result_img);
//        imageView.setImageBitmap(resultBitmap);
    }

    // 产生提示音
    public void playBee(final Context context)
    {
        boolean shouldPlayBeep = true;
        AudioManager audioService = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            shouldPlayBeep = false;//检查当前是否是静音模式
        }

        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                player.seekTo(0);
            }
        });

        AssetFileDescriptor file = context.getResources().openRawResourceFd(R.raw.success);
        try {
            mediaPlayer.setDataSource(file.getFileDescriptor(),
                    file.getStartOffset(), file.getLength());
            file.close();
            mediaPlayer.setVolume(0, 1);
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            mediaPlayer = null;
        }

        if (shouldPlayBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.stop();
            }
        });
    }
}
