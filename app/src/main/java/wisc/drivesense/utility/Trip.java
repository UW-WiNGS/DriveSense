package wisc.drivesense.utility;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by lkang on 3/29/16.
 */
public class Trip extends TripMetadata {

    public int id;
    private long startTime_ = 0;
    private long endTime_ = 0;

    private double score_ = 10.0;

    private List<Trace.Trip> gps_;
    private double tilt_;
    private boolean synced_;

    private static final String TAG = "Trip";

    public Trip(int id, String uuid, long startTime, long endTime) {
        this();
        this.id = id;
        this.guid = uuid;
        this.startTime_ = startTime;
        this.endTime_ = endTime;
    }

    public Trip () {
        this(System.currentTimeMillis());
    }

    private Trip(long time) {
        super();
        guid = UUID.randomUUID().toString();
        status = 1;
        distance = 0.0;
        gps_ = new ArrayList<Trace.Trip>();
        this.startTime_ = time;
        this.endTime_ = time;
    }

    public void setScore(double score) {this.score_ = score;}
    public void setStatus(int status) {this.status = status;}
    public void setEndTime(long time) {this.endTime_ = time;}
    public void setDistance(double dist) {this.distance = dist;}
    public void setSynced(boolean val){synced_=val;}
    public void setTilt(double tilt) {this.tilt_ = tilt;}

    public double getTilt() {return this.tilt_;}
    public long getStartTime() {
        return this.startTime_;
    }
    public long getEndTime() {
        return this.endTime_;
    }
    public double getDistance() {
        return this.distance;
    }
    public double getScore() {return this.score_;}
    public long getDuration() {return this.endTime_ - this.startTime_;}
    public int getStatus() {return this.status;}
    public boolean getSynced(){ return this.synced_; }


    public LatLng getStartPoint() {
        if(gps_ == null || gps_.size() < 1)
            return null;
        return new LatLng(gps_.get(0).lat, gps_.get(0).lng);
    }
    public LatLng getEndPoint() {
        if(gps_ == null || gps_.size() < 1)
            return null;
        return new LatLng(gps_.get(gps_.size() - 1).lat, gps_.get(gps_.size() - 1).lng);
    }


    /**
     * Add one GPS point in real time, do not keep the GPS array in memory
     * always read/write gps points from database
     *
     * @param trace
     */
    public void addGPS(Trace.Trip trace) {
        gps_.add((Trace.Trip)trace);
        this.endTime_ = trace.time;

        int sz = gps_.size();
        if(sz >= 2) {
            this.distance += distance(gps_.get(sz - 2), gps_.get(sz - 1));
            //keep it to be just last two locations
            gps_.remove(0);
        }
    }


    public void setGPSPoints(List<Trace.Trip> gps) {
        int sz = gps.size();
        if(sz == 0) {
            return;
        }
        this.gps_ = gps;
    }

    public List<Trace.Trip> getGPSPoints() {
        return gps_;
    }



    public static double distance(Trace.GPS gps0, Trace.GPS gps1) {

        double lat1 = Math.toRadians(gps0.lat);
        double lat2 = Math.toRadians(gps1.lat);
        double dLat = Math.toRadians(gps1.lat - gps0.lat);
        double dLon = Math.toRadians(gps1.lng - gps0.lng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double res = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        if(res< Constants.kSmallEPSILON || res!=res) {
            res = 0.0;
        }
        //Log.log("dis:", res);
        return res * Constants.kEarthRadius;
    }

}
