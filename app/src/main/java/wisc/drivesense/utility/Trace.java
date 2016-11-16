package wisc.drivesense.utility;


import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.io.Serializable;

public abstract class Trace implements Serializable {

    public static interface IVector {
        public float[] values();
        public void values(float[] v);
    }
    public static class Vector3 extends Trace implements IVector {
        public float x, y, z;
        @Override
        public float[] values() {
            return new float[] {x,y,z};
        }
        @Override
        public void values(float[] v) {
            x = v[0]; y = v[1]; z = v[2];
        }
    }
    public static class Gyro extends Vector3 { }
    public static class Accel extends Vector3 { }
    public static class Magnetometer extends Vector3 { }
    public static class Trip extends GPS implements Serializable{
        public float tilt;
        public float score;
        public float brake;
    }
    public static  class GPS extends Trace {
        public float lat;
        public float lng;
        public float speed;
        public float alt;
        public LatLng toLatLng() { return new LatLng(lat, lng); }
    }
    public static  class Rotation extends Trace implements IVector{
        float[] matrix = new float[9];
        @Override
        public float[] values() {
            return matrix.clone();
        }
        @Override
        public void values(float[] v) {
            matrix = v;
        }
    }
    @Expose
    public long time;
    public Trace copyTrace() {
        return copyTrace(this.getClass());
    }
    public <T extends Trace> T copyTrace(Class<T> type) {
        Gson gson = new Gson();
        String m = gson.toJson(this);
        return gson.fromJson(m, type);
    }

    public String toJson() {
        return GsonSingleton.toJson(this);
    }

}