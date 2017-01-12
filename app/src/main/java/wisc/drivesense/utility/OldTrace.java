package wisc.drivesense.utility;

/**
 * Created by lkang on 1/11/17.
 */

public class OldTrace {
    public long time;
    public double [] values = null;
    public int dim;
    public String type;

    public static String ACCELEROMETER = "accelerometer";
    public static String GYROSCOPE = "gyroscope";
    public static String MAGNETOMETER = "magnetometer";
    public static String ROTATION_MATRIX = "rotation_matrix";
    public static String GPS = "gps";
    public static String SPEED = "Vehicle Speed";


    public OldTrace() {
        time = 0;
        dim = 3;
        values = new double [dim];
    }


    public OldTrace(int d) {
        time = 0;
        dim = d;
        values = new double [dim];
    }
    public OldTrace(String type, int d) {
        this.type = type;
        time = 0;
        dim = d;
        values = new double [dim];
    }


    public void setValues(double x, double y, double z) {
        values[0] = x;
        values[1] = y;
        values[2] = z;
        dim = 3;
    }


    public void copyTrace(OldTrace trace) {
        this.time = trace.time;
        this.dim = trace.dim;
        this.type = trace.type;
        this.values = new double[dim];
        for(int i = 0; i < dim; ++i) {
            this.values[i] = trace.values[i];
        }
    }


    public static OldTrace fromTrace(Trace trace) {
        int d = ((Trace.Accel) trace).values().length;
        OldTrace oldTrace = new OldTrace(d);
        oldTrace.time = trace.time;
        if(trace instanceof Trace.Accel) {
            oldTrace.type = OldTrace.ACCELEROMETER;
            for(int i = 0; i < d; ++i) {
                oldTrace.values[i] = ((Trace.Accel) trace).values()[i];
            }
        } else if (trace instanceof Trace.Gyro) {
            oldTrace.type = OldTrace.GYROSCOPE;
            for(int i = 0; i < d; ++i) {
                oldTrace.values[i] = ((Trace.Accel) trace).values()[i];
            }
        } else if(trace instanceof Trace.Rotation) {
            oldTrace.type = OldTrace.ROTATION_MATRIX;
            for(int i = 0; i < d; ++i) {
                oldTrace.values[i] = ((Trace.Accel) trace).values()[i];
            }
        }
        return oldTrace;
    }


}
