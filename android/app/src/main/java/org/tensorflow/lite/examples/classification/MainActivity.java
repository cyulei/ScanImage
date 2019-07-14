package org.tensorflow.lite.examples.classification;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2  {
    private CameraBridgeViewBase mOpenCvCameraView;
    private static final String TAG  = "OCVSample::Activity";
    public static final String EXTRA_MESSAGE = "com.example.Opencv-VideoProcessing.Image";
    private Mat mInternmediateMat;

    public static final int  VIEW_MODE_RGBA=0;
    public static final int  VIEW_DETECT=2;

    private MenuItem  mItemPreviewRGBA;
    private MenuItem  mItemPreviewDetect;

    DrawImageView drawImageView;                  // 最外面的一层UI
    Button scanBtn;                               // 扫描按钮

    float DST_CENTER_RECT_WIDTH = 200;            // 屏幕显示方形区域的宽,单位dp
    float DST_CENTER_RECT_HEIGHT = 200;           // 屏幕显示方形区域的高,单位dp

    int viewWidth;                               // SurfaceView的宽度
    int viewHeight;                              // SurfaceView的高度

    public static int viewMode = VIEW_MODE_RGBA;

    public static boolean isScaning = false;     // 标记正在扫描

    private Handler handler = new Handler();


    private  BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                }break;
                default:
                {
                    super.onManagerConnected(status);
                }break;
            }

        }
    };

    protected void onStart(){
        super.onStart();
        viewWidth = DisplayUtils.getScreenWidth(MainActivity.this);
        viewHeight = DisplayUtils.getScreenHeight(MainActivity.this);

        if(drawImageView != null){
            Rect screenCenterRect = createCenterScreenRect(DisplayUtils.dip2px(MainActivity.this, DST_CENTER_RECT_WIDTH)
                    ,DisplayUtils.dip2px(MainActivity.this, DST_CENTER_RECT_HEIGHT));
            // 在界面上画出矩形与遮罩
            drawImageView.setCenterRect(screenCenterRect);
        }
    }

    /**
     * 生成屏幕中间的矩形
     * @param w 目标矩形的宽度,单位px
     * @param h	目标矩形的高度,单位px
     * @return
     */
    private Rect createCenterScreenRect(int w, int h){
        // 获取屏幕宽度高度，选取中间位置
        int offset = 50;                // 偏移 因为可能会不是图像正中间
        int x1 = viewWidth / 2 - w / 2;
        int y1 = viewHeight / 2 -  h / 2 - offset;
        int x2 = x1 + w;
        int y2 = y1 + h;
        return new Rect(x1, y1, x2, y2);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
        else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.menu_main, menu);
        mItemPreviewRGBA =menu.add("Preview RGBA");
        mItemPreviewDetect=menu.add("Detect");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.i(TAG,"called onOptionsItemsSelected;selected item:"+ item);
        if(item==mItemPreviewRGBA)
            viewMode=VIEW_MODE_RGBA;
        if(item==mItemPreviewDetect)
            viewMode=VIEW_DETECT;
        //noinspection SimplifiableIfStatement
        return true;
    }
    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // 给扫图按钮绑定事件
        scanBtn = (Button)findViewById(R.id.scan_btn);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanImage();
            }
        });

        // 扫图按钮按下和抬起的放缩
        scanBtn.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(event.getAction() == MotionEvent.ACTION_UP){
                            ViewGroup.LayoutParams lp = scanBtn.getLayoutParams();
                            lp.height = 240;
                            lp.width = 240;
                            scanBtn.setLayoutParams(lp);
                        }
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            ViewGroup.LayoutParams lp = scanBtn.getLayoutParams();
                            lp.height = 260;
                            lp.width = 260;
                            scanBtn.setLayoutParams(lp);
                        }
                        return false;
                    }
                }
        );

        drawImageView = (DrawImageView)findViewById(R.id.view_draw);
    }

    // 点击扫图按钮
    public  void scanImage()
    {
        viewMode = VIEW_DETECT;
        isScaning = true;
    }

    // 构建Runnable对象，在runnable中更新界面
    Runnable   runnableUi=new  Runnable(){
        @Override
        public void run() {
            // 绘制中间的绿线
            drawImageView.invalidate();
        }

    };
    public Mat onCameraFrame(CvCameraViewFrame inputFrame){
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();
        Mat rgbaInnerWindow;
        int rows = (int)sizeRgba.height;
        int cols = (int)sizeRgba.width;
        int left = 0;// cols/8;
        int top = 0;//rows/8;
        int width = cols;//*3/4;
        int height = rows;//*3/4;

        switch (MainActivity.viewMode){
            case MainActivity.VIEW_MODE_RGBA:
                rgbaInnerWindow = rgba.submat(top,top+height,left,left+width);
                Mat m_rotateMat = Imgproc.getRotationMatrix2D(new Point(rgba.rows()/2, rgba.cols()/2), -90, 1);
                Imgproc.warpAffine(rgbaInnerWindow, rgbaInnerWindow, m_rotateMat, rgba.size());
                rgbaInnerWindow.release();
                break;
            case MainActivity.VIEW_DETECT:
                handler.post(runnableUi);

                // 1.计算灰度图像
                rgbaInnerWindow = rgba.submat(top,top+height,left,left+width);
                Mat img_gray = inputFrame.gray();
                Mat m_grayInnerWindow = img_gray.submat(top,top+height,left,left+width);

                // 2.阈值二值化
                Imgproc.threshold(m_grayInnerWindow, mInternmediateMat,140, 255, Imgproc.THRESH_BINARY_INV);

                // 3.膨胀操作，消除小孔洞 如果要缩减运行时间，可考虑去除
                Mat kernel = new Mat(3, 3, CvType.CV_8UC1);
                kernel.put(0, 0, new byte[]{1, 1, 1, 1, 1, 1, 1, 1, 1});
                Imgproc.dilate(mInternmediateMat, mInternmediateMat, kernel);

                // 4.加入边缘修正
                Mat edge = new Mat();
                Imgproc.Canny(m_grayInnerWindow,edge,20,60);
                Core.add(mInternmediateMat, edge, mInternmediateMat);

                // 5.寻找边界
                Vector<MatOfPoint> contours = new Vector<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(mInternmediateMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                Vector<MatOfPoint> outlines = new Vector<>();
                for(int idx = 0; idx < contours.size(); ++idx)
                {
                    // 6.较小区域根据面积过滤掉
                    if(Imgproc.contourArea(contours.get(idx)) < 40000)
                        continue;

                    // 7.计算凸包并过滤小于4个点的凸包
                    MatOfInt hull_idx = new MatOfInt();
                    Imgproc.convexHull(contours.get(idx), hull_idx);
                    if(hull_idx.size().height < 4)
                        continue;
                    // 上面hull_idx只是得到凸包对应于contours的索引
                    Point[] points = new Point[(int)(hull_idx.size().height)];
                    for(int i = 0; i < hull_idx.size().height ; i++)
                    {
                        int index = (int)hull_idx.get(i, 0)[0];
                        points[i] = new Point(
                                contours.get(idx).get(index, 0)[0], contours.get(idx).get(index, 0)[1]
                        );
                    }
                    MatOfPoint2f hull_val = new MatOfPoint2f(points);

                    // 8.多边形近似，进一步减少凸包点
                    double epsilon = 0.01 * Imgproc.arcLength(hull_val, true);
                    MatOfPoint2f approx = new MatOfPoint2f();
                    Imgproc.approxPolyDP(hull_val, approx, epsilon, true);
                    if(approx.size().height != 4)
                        continue;

                    // 9.对检测出的四个角点进行重新排列，确保从左上角点起按顺时针方向排列，以和dst依次对应
                    Point[] approx_points = approx.toArray();
                    Point[] rearrange_points = new Point[(int)(approx.size().height)];
                    Arrays.sort(approx_points, new Comparator<Point>() {
                        @Override
                        public int compare(Point o1, Point o2) {
                            if(o1.y != o2.y)
                                return o1.y > o2.y ? 1 : -1;
                            else
                                return 0;
                        }
                    });
//                    Log.d(TAG, "-------------------");
//                    Log.d(TAG, approx_points[0].toString());
//                    Log.d(TAG, approx_points[1].toString());
//                    Log.d(TAG, approx_points[2].toString());
//                    Log.d(TAG, approx_points[3].toString());
//                    Log.d(TAG, "-------------------");
                    if(approx_points[0].x < approx_points[1].x) {
                        rearrange_points[0] = approx_points[0];
                        rearrange_points[1] = approx_points[1];
                    }else{
                        rearrange_points[1] = approx_points[0];
                        rearrange_points[0] = approx_points[1];
                    }
                    if(approx_points[2].x > approx_points[3].x) {
                        rearrange_points[2] = approx_points[2];
                        rearrange_points[3] = approx_points[3];
                    }else{
                        rearrange_points[3] = approx_points[2];
                        rearrange_points[2] = approx_points[3];
                    }
//                    Log.d(TAG, "-------------------");
//                    Log.d(TAG, rearrange_points[0].toString());
//                    Log.d(TAG, rearrange_points[1].toString());
//                    Log.d(TAG, rearrange_points[2].toString());
//                    Log.d(TAG, rearrange_points[3].toString());
//                    Log.d(TAG, "-------------------");

                    // 用于可视化
                    MatOfPoint approxP = new MatOfPoint();
                    approx.convertTo(approxP, CvType.CV_32S);
                    outlines.clear();
                    outlines.add(approxP);

                    /** 计算获取到的边的比例 */
                    double length1 =  Math.sqrt((rearrange_points[1].x-rearrange_points[2].x)*(rearrange_points[1].x-rearrange_points[2].x)
                            + (rearrange_points[1].y-rearrange_points[2].y)*(rearrange_points[1].y-rearrange_points[2].y));
                    double length2 =  Math.sqrt((rearrange_points[2].x-rearrange_points[3].x)*(rearrange_points[2].x-rearrange_points[3].x)
                            + (rearrange_points[2].y-rearrange_points[3].y)*(rearrange_points[2].y-rearrange_points[3].y));
                    double offset = 0.2;
                    /** 在范围内的可以进行识别 用于排除一些干扰 */
                    if(length1 /  length2 < 1 - offset || length1 /  length2 > 1 + offset)
                    {
                        continue ;
                    }

                    // 11. 可视化
                    for(int j = 0;j < outlines.size();j ++)
                    {
                        Imgproc.drawContours(rgbaInnerWindow, outlines, j, new Scalar(255, 0, 0), 3);
                    }

                    //------------------------------------------------------------------
                    MatOfPoint2f rearrange_approx = new MatOfPoint2f(rearrange_points);

                    // 10. 仿射变换到400×400大小 备注：要考虑到原图是旋转了90度的
                    MatOfPoint2f dst = new MatOfPoint2f(
                            new Point(400-1, 0),
                            new Point(400-1,400-1),
                            new Point(0,400-1),
                            new Point(0,0)
                    );
                    Mat tfs_matrix = Imgproc.getPerspectiveTransform(rearrange_approx, dst);
                    Mat destImg = new Mat(400, 400, CvType.CV_8UC3);
                    Imgproc.warpPerspective(rgbaInnerWindow, destImg, tfs_matrix, destImg.size());

                    viewMode = VIEW_MODE_RGBA;      // 跳到另一个Activity回来后为拍摄状态，需再次点击扫描才是检测状态
                    isScaning = false;


                    // reference: https://stackoverflow.com/questions/29060376/how-do-i-send-opencv-mat-as-a-putextra-to-android-intent
                    // 这种方式会出现闪退现象
//                    long addr = destImg.getNativeObjAddr();

                    Bitmap resultBitmap = Bitmap.createBitmap(destImg.cols(), destImg.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(destImg, resultBitmap);
                    byte[] buff = Bitmap2Bytes(resultBitmap);

                    Intent intent = new Intent(this, DisplayResultActivity.class);
                    intent.putExtra(EXTRA_MESSAGE, buff);
                    startActivity(intent);


                    break;
                }

                // 11. 可视化
                for(int j = 0;j < outlines.size();j ++)
                {
                    Imgproc.drawContours(rgbaInnerWindow, outlines, j, new Scalar(255, 0, 0), 3);
                }

                Mat rotateMat = Imgproc.getRotationMatrix2D(new Point(rgba.rows()/2, rgba.cols()/2), -90, 1);
                Imgproc.warpAffine(rgbaInnerWindow, rgbaInnerWindow, rotateMat, rgba.size());
                m_grayInnerWindow.release();
                rgbaInnerWindow.release();

                break;
        }
        return rgba;
    }
    // 将Bitmap转为Bytes
    private byte[] Bitmap2Bytes(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
    public void onCameraViewStopped(){
        if(mInternmediateMat !=null)
            mInternmediateMat.release();
        mInternmediateMat=null;
    }
    public void onCameraViewStarted(int width,int height){
        mInternmediateMat=new Mat();
    }
}
