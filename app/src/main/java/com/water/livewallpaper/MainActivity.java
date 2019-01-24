package com.water.livewallpaper;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximitySensorListener;
    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeSensorListener;
    private Sensor rotationVectorSensor;
    private SensorEventListener rvListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        init();
    }

    private void init() {
        /**
         * 要访问任何硬件传感器，您需要一个SensorManager对象。要创建它，请使用getSystemService()您的Activity类的方法并将SENSOR_SERVICE常量传递给它。
         */
        // 取传感器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

//        initProximitySensor();
//        initGyroscopeSensor();
        initRotationVectorSensor();

    }

    private void initRotationVectorSensor() {
        /**
         * 步骤1：设置旋转矢量传感器
         要获取旋转矢量传感器，必须将TYPE_ROTATION_VECTOR常量传递给对象的getDefaultSensor()方法SensorManager。
         */
        // 获取旋转矢量传感器
        rotationVectorSensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        /**
         * 使用软件传感器与使用硬件传感器没有什么不同。因此，您必须将侦听器与旋转矢量传感器相关联才能读取其数据。您可以再次使用SENSOR_DELAY_NORMAL常量进行轮询间隔。
         */
// Create a listener
        rvListener = new SensorEventListener() {

            @Override

            public void onSensorChanged(SensorEvent sensorEvent) {

                // More code goes here
                /**
                 * 步骤2：使用数据

                 旋转矢量传感器组合由陀螺仪，加速度计和磁力计产生的原始数据，以产生四元数。因此，values其SensorEvent对象的数组有以下五个元素：
                 四元数的X，Y，Z和W分量
                 标题精度
                 您可以通过使用该类的getRotationMatrixFromVector() 方法将四元数转换为旋转矩阵，即4x4矩阵SensorManager。
                 */

                float[] rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(
                        rotationMatrix, sensorEvent.values);

                /**
                 * 如果您正在开发OpenGL应用程序，则可以直接使用旋转矩阵来转换3D场景中的对象。然而，现在，我们将旋转矩阵转换成方向阵列，指定器件沿着Z，X和Y轴的旋转。为此，
                 我们可以使用该类的getOrientation() 方法SensorManager。
                 在调用该getOrientation() 方法之前，必须重新映射旋转矩阵的坐标系。更准确地说，您必须旋转旋转矩阵，使新坐标系的Z轴与原始坐标系的Y轴重合。
                 */

                // Remap coordinate system
                float[] remappedRotationMatrix = new float[16];
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedRotationMatrix);

// Convert to orientations
                float[] orientations = new float[3];
                SensorManager.getOrientation(remappedRotationMatrix, orientations);
                /**
                 * 默认情况下，orientations数组包含弧度而不是度数的角度。如果您习惯于弧度，请直接使用它。否则，使用以下代码将其所有角度转换为度数：
                 */
                for (int i = 0; i < 3; i++) {
                    orientations[i] = (float) (Math.toDegrees(orientations[i]));
                }

                /**
                 * 您现在可以根据orientations数组的第三个元素更改活动的背景颜色。
                 */

                if (orientations[2] > 45) {
                    getWindow().getDecorView().setBackgroundColor(Color.YELLOW);
                } else if (orientations[2] < -45) {
                    getWindow().getDecorView().setBackgroundColor(Color.BLUE);
                } else if (Math.abs(orientations[2]) < 10) {
                    getWindow().getDecorView().setBackgroundColor(Color.WHITE);
                }
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }

        };


// Register it
        sensorManager.registerListener(rvListener,
                rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void initGyroscopeSensor() {
        /**
         * 步骤1：获取陀螺仪
         要创建Sensor陀螺仪的对象，所有您需要做的是将TYPE_GYROSCOPE常量传递给对象的getDefaultSensor()方法SensorManager。
         */
        // 获取陀螺仪
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        /**
         * 步骤2：注册听众
         创建陀螺仪传感器的监听器与为接近传感器创建侦听器没有什么不同。
         但是，注册时，您必须确保其采样频率非常高。
         因此，我建议您使用SENSOR_DELAY_NORMAL常量，而不是以微秒为单位指定轮询间隔。
         */
        // Create a listener
        gyroscopeSensorListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                /**
                 * 步骤3：使用原始数据
                 陀螺传感器的原始数据由三个float值组成  ，指定器件沿X，Y和Z轴的角速度。
                 每个值的单位是每秒弧度。
                 在沿着任何轴的逆时针旋转的情况下，与该轴相关联的值将为正。
                 在顺时针旋转的情况下，它将为负。
                 因为我们目前只对沿着Z轴的旋转感兴趣，所以我们将只使用对象values数组中的第三个元素SensorEvent。
                 如果超过0.5f，我们可以在很大程度上确保旋转是逆时针旋转的，并将背景颜色设置为蓝色。
                 类似地，如果它小于-0.5f，我们可以将背景颜色设置为黄色。
                 */
                // More code goes here
                if (sensorEvent.values[2] > 0.5f) { // anticlockwise
                    getWindow().getDecorView().setBackgroundColor(Color.BLUE);
                } else if (sensorEvent.values[2] < -0.5f) { // clockwise
                    getWindow().getDecorView().setBackgroundColor(Color.YELLOW);
                }
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        // Register the listener 注册听众
        sensorManager.registerListener(gyroscopeSensorListener,
                gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);


    }

    private void initProximitySensor() {

        /**
         * 您现在可以Sensor通过调用该getDefaultSensor()方法并将TYPE_PROXIMITY常量传递给它来为接近传感器创建一个对象。
         */
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        /**
         * 在继续之前，始终确保Sensor对象不是null。如果是，则表示接近传感器不可用。
         */
        if (proximitySensor == null) {
            Toast.makeText(this, "接近传感器不可用", Toast.LENGTH_LONG).show();
            finish(); // Close app
        }

        // Create listener  创建监听器
        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // More code goes here
                if (sensorEvent.values[0] < proximitySensor.getMaximumRange()) {
                    // Detected something nearby  检测到附近的东西
                    getWindow().getDecorView().setBackgroundColor(Color.RED);
                } else {
                    // Nothing is nearby  附近没什么
                    getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        // Register it, specifying the polling interval in  注册，指定轮询间隔
// microseconds  微秒
        sensorManager.registerListener(proximitySensorListener,
                proximitySensor, 2 * 1000 * 1000);
    }
}
