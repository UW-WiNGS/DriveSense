package wisc.drivesense.utility;


import android.content.ContentValues;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

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
    public static class Trip extends GPS {
        public float tilt;
        public float score;
        public float brake;
    }
    public static  class GPS extends Trace {
        public float lat;
        public float lon;
        public float speed;
        public float alt;
        public LatLng toLatLng() { return new LatLng(lat, lon); }
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
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(this), this.getClass());
    }

    public String toJson() {
        return GsonSingleton.toJson(this);
    }

}