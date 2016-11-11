package wisc.drivesense.utility;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lkang on 3/29/16.
 */
public class Trip implements Serializable {

    private long startTime_ = 0;
    private long endTime_ = 0;
    private double distance_ = 0; // in meter
    private double speed_ = 0.0;
    private double score_ = 10.0;
    private int status_ = 1;
    private List<Trace.Trip> gps_;
    private LatLng start_ = null;
    private LatLng dest_ = null;
    private double tilt_;

    //private Rating rating = null;

    private String TAG = "Trip";

    //private DatabaseHelper dbHelper_ = null;

    public Trip (long time) {
        gps_ = new ArrayList<Trace.Trip>();
        this.startTime_ = time;
        this.endTime_ = time;
        //rating = new Rating(this);
    }

    public void setScore(double score) {this.score_ = score;}
    public void setStatus(int status) {this.status_ = status;}
    public void setEndTime(long time) {this.endTime_ = time;}
    public void setDistance(double dist) {this.distance_ = dist;}

    public void setTilt(double tilt) {this.tilt_ = tilt;}
    public double getTilt() {return this.tilt_;}


    public long getStartTime() {
        return this.startTime_;
    }
    public long getEndTime() {
        return this.endTime_;
    }
    public double getDistance() {
        return this.distance_;
    }
    public double getScore() {return this.score_;}
    public long getDuration() {return this.endTime_ - this.startTime_;}
    public int getStatus() {return this.status_;}


    public LatLng getStartPoint() {return start_;}
    public LatLng getEndPoint() {return dest_;}



    public double getSpeed() {return speed_ * Constants.kMeterPSToMilePH;}


    /**
     * Add one GPS point in real time, do not keep the GPS array in memory
     * always read/write gps points from database
     *
     * @param trace
     */
    public void addGPS(Trace.GPS trace) {

        if(trace instanceof Trace.Trip)
            gps_.add((Trace.Trip)trace);
        if(start_ == null) {
            start_ = new LatLng(trace.lat, trace.lon);
        }
        dest_ = new LatLng(trace.lat, trace.lon);
        speed_ = trace.speed;
        this.endTime_ = trace.time;

        int sz = gps_.size();
        if(sz >= 2) {
            distance_ += distance(gps_.get(sz - 2), gps_.get(sz - 1));
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
        this.start_ = new LatLng(gps.get(0).lat, gps.get(0).lon);
        this.dest_ = new LatLng(gps.get(sz - 1).lat, gps.get(sz - 1).lon);
    }

    public List<Trace.Trip> getGPSPoints() {
        return gps_;
    }



    public static double distance(Trace.GPS gps0, Trace.GPS gps1) {

        double lat1 = Math.toRadians(gps0.lat);
        double lng1 = Math.toRadians(gps0.lon);
        double lat2 = Math.toRadians(gps1.lat);
        double lng2 = Math.toRadians(gps1.lon);

        double p1 = Math.cos(lat1)*Math.cos(lat2)*Math.cos(lng1-lng2);
        double p2 = Math.sin(lat1)*Math.sin(lat2);

        double res = Math.acos(p1 + p2);
        if(res< Constants.kSmallEPSILON || res!=res) {
            res = 0.0;
        }
        //Log.log("dis:", res);
        return res * Constants.kEarthRadius;
    }

}
