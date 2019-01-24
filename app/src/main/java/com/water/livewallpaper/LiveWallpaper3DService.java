package com.water.livewallpaper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.water.livewallpaper.obj.Obj3D;
import com.water.livewallpaper.obj.ObjFilter2;
import com.water.livewallpaper.obj.ObjReader;
import com.water.livewallpaper.utils.Gl2Utils;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LiveWallpaper3DService extends WallpaperService{
    private List<ObjFilter2> filters;

    private String TAG = "LiveWallpaper3DService";

    public LiveWallpaper3DService() {
    }

    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends Engine implements SensorEventListener{
        private WallpaperGLSurfaceView mSurfaceView;

        private OpenGLParticleShaderRender mShaderRender;

        private boolean mRender;

        private final float TOUCH_SCALE_FACTOR = 180.0f / 320;//角度缩放比例
        private float mPreviousY;//上次的触控位置Y坐标
        private float mPreviousX;//上次的触控位置X坐标

        private SensorManager mSensorManager;
        private Sensor gyroscopeSensor;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            mSensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);

            gyroscopeSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(this,gyroscopeSensor,SensorManager.SENSOR_DELAY_GAME);

            mSurfaceView = new WallpaperGLSurfaceView(LiveWallpaper3DService.this);

            List<Obj3D> model = ObjReader.readMultiObj(LiveWallpaper3DService.this, "assets/3dres/pikachu.obj");
            filters = new ArrayList<>();
            for (int i = 0; i < model.size(); i++) {
                ObjFilter2 f = new ObjFilter2(getResources());
                f.setObj3D(model.get(i));
                filters.add(f);
            }

            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo configurationInfo = am.getDeviceConfigurationInfo();

            boolean supportEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

            if (supportEs2) {
                // 选择OpenGL ES 2.0
                mSurfaceView.setEGLContextClientVersion(2);
                mShaderRender = new OpenGLParticleShaderRender();
                // 设置渲染
                mSurfaceView.setRenderer(mShaderRender);
                mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                mRender = true;
            } else {
                Toast.makeText(LiveWallpaper3DService.this,
                        "This device does not support OpenGL ES 2.0",
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (mRender) {
                if (visible) {
                    mSurfaceView.onResume();
                } else {
                    mSurfaceView.onPause();
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mSensorManager.unregisterListener(this);
            mSurfaceView.onWallpaperDestroy();
        }

        @Override
        public void onOffsetsChanged(final float xOffset, final float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
            mSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    Log.e("GLEngine", "xOffset = " + xOffset + ", yOffset = " + yOffset);
                }
            });
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            float y = event.getY();
            float x = event.getX();
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float dy = y - mPreviousY;//计算触控笔Y位移
                    float dx = x - mPreviousX;//计算触控笔X位移
                    //
                    float yAngle = 0;
                    yAngle += dx * TOUCH_SCALE_FACTOR;

                    Log.e("xiaxl: ","yAngle: "+yAngle);
                    for (ObjFilter2 f : filters) {
                        /**
                         * Matrix.rotateM(f.getMatrix(),0,0.3f,0,1,0);
                         * 物体旋转
                         */
                        Matrix.rotateM(f.getMatrix(),0,-yAngle,0,1,0);
//                        f.draw();
                    }
                    mSurfaceView.requestRender();//重绘画面
            }
            mPreviousY = y;//记录触控笔位置
            mPreviousX = x;//记录触控笔位置
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                for (ObjFilter2 f : filters) {
                    Matrix.rotateM(f.getMatrix(), 0, event.values[2], 0, 1, 0);
                }
                mSurfaceView.requestRender();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        class WallpaperGLSurfaceView extends GLSurfaceView {

            public WallpaperGLSurfaceView(Context context) {
                super(context);
            }

            @Override
            public SurfaceHolder getHolder() {
                return GLEngine.this.getSurfaceHolder();
            }

            public void onWallpaperDestroy() {
                super.onDetachedFromWindow();
            }
        }
    }

    private class OpenGLParticleShaderRender implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            for (ObjFilter2 f : filters) {
                f.create();
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            for (ObjFilter2 f : filters) {
                f.onSizeChanged(width, height);
                float[] matrix = Gl2Utils.getOriginalMatrix();
                Matrix.translateM(matrix, 0, 0, -0.3f, 0);
                Matrix.scaleM(matrix, 0, 0.008f, 0.008f * width / height, 0.008f);
                f.setMatrix(matrix);
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            for (ObjFilter2 f : filters) {
//                Matrix.rotateM(f.getMatrix(), 0, 0.3f, 0, 1, 0);
                f.draw();
            }
        }
    }
}
