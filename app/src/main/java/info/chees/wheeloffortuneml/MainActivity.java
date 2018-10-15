package info.chees.wheeloffortuneml;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private TextView currentTextView;
    private TextView historyTextView;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private Handler handler;
    private boolean running;

    private float previousAngle;
    private boolean spinning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentTextView = findViewById(R.id.current);
        historyTextView = findViewById(R.id.history);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // TODO lower delay?
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);
        }

        running = true;

        handler = new Handler();
        final int delay = 100; //milliseconds

        handler.postDelayed(new Runnable() {
            public void run(){
                updateOrientationAngles();

                float angleDiff = Math.abs(mOrientationAngles[0] - previousAngle);

                currentTextView.setText("" + round(mOrientationAngles[0], 2));
                currentTextView.setRotation((float) Math.toDegrees(-mOrientationAngles[0]));
                //currentTextView.setText("" + round(angleDiff, 3));

                if (angleDiff < 0.04) {
                    // Standing still
                    if (spinning) {
                        // Just stopped
                        historyTextView.append("\nDone");
                        spinning = false;
                    }
                } else {
                    // Spinning
                    if (!spinning) {
                        // Just started
                        if (angleDiff > 0.4) {
                            // Only start when the angle got big enough
                            historyTextView.setText("Start");
                            spinning = true;
                        }
                    } else {
                        historyTextView.append("\n" + round(mOrientationAngles[0], 2));
                    }
                }
                previousAngle = mOrientationAngles[0];

                if (running) {
                    handler.postDelayed(this, delay);
                }
            }
        }, delay);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        running = false;
    }


    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }
    }

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
    }

    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
