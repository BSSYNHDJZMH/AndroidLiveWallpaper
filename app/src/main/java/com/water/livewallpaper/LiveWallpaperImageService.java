package com.water.livewallpaper;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.water.livewallpaper.utils.LogUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveWallpaperImageService extends WallpaperService {
    private String TAG = "LiveWallpaperImageService";
    private ExecutorService fixedThreadPool;

    public LiveWallpaperImageService() {

    }

    @Override
    public Engine onCreateEngine() {
        return new ImageEngine();
    }

    class ImageEngine extends Engine implements SensorEventListener {
        private SensorManager mSensorManager;
        private Sensor gyroscopeSensor;
        private Bitmap backgroundImage;
        private Rect frame;

        private long endTimestamp;
        private double angleX;
        private double angleY;

        //0到π/2
        private double maxAngle = Math.PI / 3;
        // 将纳秒转化为秒
        private static final float NS2S = 1.0f / 1000000000.0f;
        private int x;
        private int y;
        private int z;
        private Rect mSrcRect;
        private RectF mDestRect;
        private float lenX;
        private float lenY;
        private float frameWidth;
        private float frameHeight;
        private int drawableWidth;
        private int drawableHeight;
        private boolean mDrawFlag;
        private final SurfaceHolder surfaceHolder =getSurfaceHolder();
        private Canvas canvas;
        private float currentOffsetX;
        private float currentOffsetY;
        private ExecutorService cachedThreadPool;

        private final Handler handler = new Handler();
        private final Runnable drawRunnable = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        private boolean visible = true;
        private boolean isLandscape;


        private void draw() {
            if (getSurfaceHolder() != null && getSurfaceHolder().getSurface() != null && getSurfaceHolder().getSurface().isValid()) {
//                surfaceHolder= getSurfaceHolder();
                synchronized (surfaceHolder){
                    if(visible){
                        Canvas canvas1 =null;
                        //锁定画布
                        canvas1 = surfaceHolder.lockHardwareCanvas();
                        try {
                            /**
                             * 清除之前的轨迹问题
                             */
                            canvas1.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            //平移
                            canvas1.translate(currentOffsetX, currentOffsetY);
                            //绘制图片
                            canvas1.drawBitmap(backgroundImage, null, mDestRect, null);

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            // 解锁画布，提交画好的图像
//                        if (getSurfaceHolder() != null && getSurfaceHolder().getSurface().isValid() && canvas != null) {
                            try{
                                if(canvas1!=null) {
                                    surfaceHolder.unlockCanvasAndPost(canvas1);
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }

//                        }
                        }
                    }
                }
                handler.removeCallbacks(drawRunnable);
                if (visible) {
                    handler.postDelayed(drawRunnable, 5000);
                }
            }
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            LogUtils.e(TAG, "onCreate: ");
            fixedThreadPool = Executors.newFixedThreadPool(4);
        }

        @Override
        public void onSurfaceCreated(final SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            LogUtils.e(TAG, "onSurfaceCreated: ");
            frame = holder.getSurfaceFrame();
            frameWidth = frame.width();
            frameHeight = frame.height();
            LogUtils.e(TAG, "onSurfaceCreated: frameWidth" + frameWidth + ",frameHeight" + frameHeight);
            // 绘制背景
            /**
             * 在使用BitmapFactory加载图片时，常会出现这种情况，返回的图片尺寸与实际尺寸不符。
             * 这是由于我们把图片资源放到res/drawable文件路径下时，选择的文件不同所致，不同的文件夹会有不同的缩放。
             *电脑上图片尺寸，单位是像素。Android手机的屏幕分ldpi、mdpi、hdpi，
             * 甚至还有xhdpi，对于mdpi（density=160）设备，1dp=1px，
             * 对于hdpi（density=240）的设备，1dp=1.5px。
             * 所以，把图片放在了res/drawable-mdpi目录下，而运行的Android设备屏幕属于hdpi，导致图片尺寸会扩大1.5倍。
             */
            /**
             * 设置缩放为false,不论你将图片放在哪里，都是原尺寸。
             * start
             */
            BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
            bfoOptions.inScaled = false;
            /**
             * end
             */

            backgroundImage = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper2560_min);
            drawableWidth = backgroundImage.getWidth();
            drawableHeight = backgroundImage.getHeight();
            LogUtils.e(TAG, "onSurfaceCreated: drawableWidth" + drawableWidth + ",drawableHeight" + drawableHeight);
//            mSrcRect = new Rect(0, 0, drawableWidth, drawableHeight);
            mSrcRect = new Rect(0, 0, drawableWidth, drawableHeight);
//            mDestRect = new RectF(-200, -200, frameWidth + 200, frameHeight + 200);
            mDestRect = new RectF(0, 0, frameWidth, frameHeight);

            /**
             *
             * Math.abs(x) 函数返回指定数字 “x“ 的绝对值
             */
            lenX = Math.abs((drawableWidth - frameWidth) * 0.5f);
            lenY = Math.abs((drawableHeight - frameHeight) * 0.5f);
            LogUtils.e(TAG, "onMeasure: " + "lenX=" + lenX + "，lenY=" + lenY);

//            surfaceHolder = holder;
//            new Thread(new MyThread()).start();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            LogUtils.e(TAG, "onSurfaceChanged: ");
            mDrawFlag = true;
//            surfaceHolder = holder;
            frame = holder.getSurfaceFrame();
            frameWidth = frame.width();
            frameHeight = frame.height();
            LogUtils.e(TAG, "onSurfaceChanged: frameWidth" + frameWidth + ",frameHeight" + frameHeight);
            handler.removeCallbacks(drawRunnable);
//            mDestRect = new RectF(-200, -200, frameWidth + 200, frameHeight + 200);
            mDestRect = new RectF(-750, -500, frameWidth+750, frameHeight+500);
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point outSize = new Point();
            display.getRealSize(outSize);
            if(outSize.x>outSize.y){
                isLandscape = true;
            }else{
                isLandscape =false;
            }
//            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
//
//                isLandscape = true;
//                LogUtils.e(TAG, "onSurfaceChanged: 横屏");
//            }else{
//                isLandscape =false;
//                LogUtils.e(TAG, "onSurfaceChanged: 竖屏");
//            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            LogUtils.e(TAG, "onVisibilityChanged: "+visible);
            this.visible = visible;
            if (visible) {
                init();
                handler.post(drawRunnable);
            } else {
                destroy();
            }
        }

        private void destroy() {
            if (mSensorManager != null) {
                mSensorManager.unregisterListener(this);
                mSensorManager = null;
            }
            synchronized (this) {
                mDrawFlag = false;
            }
            handler.removeCallbacks(drawRunnable);
            visible=false;
        }

        private void init() {
            if (mSensorManager == null) {
                mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            }
            if (gyroscopeSensor == null) {
                gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            }

            mSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
            endTimestamp = 0;
            angleX = 0;
            angleY = 0;

            synchronized (this) {
                mDrawFlag = true;
            }
        }


        @Override
        public void onDestroy() {
            super.onDestroy();
            LogUtils.e(TAG, "onDestroy: ");
            destroy();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            LogUtils.e(TAG, "onSurfaceDestroyed: ");

            destroy();
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (endTimestamp == 0) {
                endTimestamp = event.timestamp;
                return;
            }
            angleX += event.values[0] * (event.timestamp - endTimestamp) * NS2S;
            angleY += event.values[1] * (event.timestamp - endTimestamp) * NS2S;

            if (angleX > maxAngle) {
                angleX = maxAngle;
            }
            if (angleX < -maxAngle) {
                angleX = -maxAngle;
            }

            if (angleY > maxAngle) {
                angleY = maxAngle;
            }
            if (angleY < -maxAngle) {
                angleY = -maxAngle;
            }
            double scaleX = angleY / maxAngle;
            double scaleY = angleX / maxAngle;

//            update(scaleX, scaleY);
            if(isLandscape){
                currentOffsetX = (float) (lenY * scaleY);
                currentOffsetY = (float) (lenX * scaleX);
            }else{
                currentOffsetX = (float) (lenX * scaleX);
                currentOffsetY = (float) (lenY * scaleY);
            }
//            new Thread(drawRunnable).start();
//            for(int i =0;i<10;i++){
//            fixedThreadPool.execute(new MyThread());
//            }
            if(visible&&surfaceHolder!=null&&surfaceHolder.getSurface()!=null&&surfaceHolder.getSurface().isValid()){
                handler.post(drawRunnable);
            }else{
                handler.removeCallbacks(drawRunnable);
            }
            endTimestamp = event.timestamp;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

}
