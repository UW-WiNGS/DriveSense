package wisc.drivesense.triprecorder;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;


public class RealTimeTiltCalculation {
	private static final String TAG = "RealTimeTiltCalculation";
	
	private List<Trace.Accel> window_accelerometer = new LinkedList<Trace.Accel>();
	private List<Trace.Gyro> window_gyroscope = new LinkedList<Trace.Gyro>();
	private List<Trace.Rotation> window_rotation_matrix = new LinkedList<Trace.Rotation>();
	private Trace.Accel curSmoothedAccelerometer = null;
	private Trace.Gyro curSmoothedGyroscope = null;
	private final int kWindowSize = 10;
		
	private double curTilt = 0.0;
	
	public double getTilt() {
		return this.curTilt;
	}
	/**
	 * the only input point
	 * @param trace
	 */
	public void processTrace(Trace trace) {
		if(trace instanceof Trace.Accel) {
			onAccelerometerChanged((Trace.Accel)trace);
		} else if (trace instanceof Trace.Gyro) {
			onGyroscopeChanged((Trace.Gyro)trace);
		} else if(trace instanceof Trace.Rotation) {
			window_rotation_matrix.add((Trace.Rotation)trace);
			if(window_rotation_matrix.size() > kWindowSize) {
				window_rotation_matrix.remove(0);
			}
		}
	}
	
	
	private void onGyroscopeChanged(Trace.Gyro gyroscope) {
		curSmoothedGyroscope = lowpassFilter(curSmoothedGyroscope, gyroscope);
		window_gyroscope.add(curSmoothedGyroscope);
		if(window_gyroscope.size() >= kWindowSize) {
			window_gyroscope.remove(0);
		}
		
	}
	
	
	private void onAccelerometerChanged(Trace.Accel accelerometer) {
		curSmoothedAccelerometer = lowpassFilter(curSmoothedAccelerometer, accelerometer);
		window_accelerometer.add(curSmoothedAccelerometer);
		if(window_accelerometer.size() >= kWindowSize) {
			window_accelerometer.remove(0);
		}
		double x = curSmoothedAccelerometer.x;
		double z = curSmoothedAccelerometer.z;
		double angle = 0.0;
		if(z == 0.0) {
			angle = x > 0.0 ? -1.57 : 1.57;
		} else {
			angle = Math.atan(-x/z);
		}
		this.curTilt = Math.toDegrees(angle);
	}
	
	private <T extends Trace.Vector3> T lowpassFilter(T last, T cur) {
		final float alpha = (float)Constants.kExponentialMovingAverageAlpha;
		T res = (T)cur.copyTrace();
		if(last != null) {
			float[] values = cur.values();
			for(int j = 0; j < values.length; ++j) {
				values[j] = alpha * values[j] + (1.0f - alpha) * last.values()[j];
			}
			cur.values(values);
		}
		return res;
	}
}
