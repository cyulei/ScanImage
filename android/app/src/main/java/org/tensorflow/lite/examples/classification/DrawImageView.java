package org.tensorflow.lite.examples.classification;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import org.opencv.core.Point;


public class DrawImageView extends ImageView {
    private Paint mLinePaint;
    private Paint mAreaPaint;
    private Paint mScanPaint;
    private Rect mCenterRect = null;

    public Point startPoint = null;
    public Point endPoint = null;

    /** 屏幕绿线的偏移位置 */
    private int offset ;
    /** 屏幕绿线每次刷新的步长 */
    private int speed = 20 ;

    private Context mContext;
    int widthScreen, heightScreen;

    public DrawImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        initPaint();
        mContext = context;

        // 获取屏幕宽度
        DisplayMetrics dm = getResources().getDisplayMetrics();
        widthScreen = dm.widthPixels;                                     
        heightScreen = dm.heightPixels;

        offset = 0;
    }

    private void initPaint(){

        mScanPaint = new Paint();
        /** 设置扫描线的画笔 */
        mScanPaint.setStyle(Paint.Style.STROKE);
        mScanPaint.setStrokeWidth(5f);
        mScanPaint.setAntiAlias(true);   //抗锯齿

        /**  绘制中间透明区域矩形边界的Paint */
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // 线的颜色
        mLinePaint.setColor(Color.BLUE);
        mLinePaint.setStyle(Paint.Style.STROKE);
        // 线的宽度
        mLinePaint.setStrokeWidth(8f);
        mLinePaint.setAlpha(50);

        /** 绘制四周阴影区域 */
        mAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAreaPaint.setColor(0xFF484344);
        mAreaPaint.setStyle(Paint.Style.FILL);
        mAreaPaint.setAlpha(210);

    }

    public void setCenterRect(Rect r){
        this.mCenterRect = r;
        setGradient();
        postInvalidate();
    }

    /** 扫描线是一个渐变颜色 */
    private void setGradient()
    {
        int [] colors = {0xFF7efc62,0xFF517c43};
        float[] position = new float[2];
        position[0] = 0f;
        position[1] = 0.98f;
        RadialGradient radialGradient = new RadialGradient(mCenterRect.left + (mCenterRect.right - mCenterRect.left)/2,mCenterRect.top + offset,(mCenterRect.right - mCenterRect.left)/2,colors,position, Shader.TileMode.CLAMP);
        mScanPaint.setShader(radialGradient);
    }

    public void clearCenterRect(){
        this.mCenterRect = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        if(mCenterRect == null)
            return;
        //绘制四周阴影区域
        canvas.drawRect(0, 0, widthScreen, mCenterRect.top, mAreaPaint);
        canvas.drawRect(0, mCenterRect.bottom + 1, widthScreen, heightScreen, mAreaPaint);
        canvas.drawRect(0, mCenterRect.top, mCenterRect.left - 1, mCenterRect.bottom  + 1, mAreaPaint);
        canvas.drawRect(mCenterRect.right + 1, mCenterRect.top, widthScreen, mCenterRect.bottom + 1, mAreaPaint);

        //绘制目标透明区域
        canvas.drawRect(mCenterRect, mLinePaint);

        // 绘制中间的扫描线
        /** 扫描线是一个渐变颜色的椭圆 */
        if(MainActivity.isScaning)
        {
            setGradient();

            RectF oval=new RectF();
            oval.left=mCenterRect.left;                  //左边
            oval.top=mCenterRect.top + offset;           //上边
            oval.right= mCenterRect.right;               //右边
            oval.bottom=mCenterRect.top+ offset + 3;     //下边   包围椭圆的矩形
            canvas.drawOval(oval, mScanPaint);           //绘制椭圆
            if(offset >= mCenterRect.height())
            {
                offset = 0;
            }
            else
            {
                offset += speed;
            }
        }
        else {
            offset = 0;
        }
        super.onDraw(canvas);
    }
}
