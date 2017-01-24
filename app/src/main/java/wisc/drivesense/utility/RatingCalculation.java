package wisc.drivesense.utility;

import android.util.Log;

import java.io.Serializable;

import wisc.drivesense.triprecorder.RealTimeTiltCalculation;

/**
 * Created by lkang on 4/20/16.
 */
public class RatingCalculation implements Serializable {
    private int counter_;
    private Trace.GPS lastTrace_;
    private double lastSpeed_;
    private double score_ = 10.0;

    private static String TAG = "RatingCalculation";

    public RatingCalculation() {
        lastSpeed_ = -1.0;
        lastTrace_ = null;
        counter_ = 0;
    }

    /**
     * Initialize the rating calculation mechanism with stats from an existing trip
     * @param traceCount
     * @param score
     */
    public RatingCalculation(int traceCount, double score) {
        this();
        counter_ = traceCount;
        score_ = score;
    }

    public Trace.Trip getRating(Trace.GPS trace) {
        float brake = this.calculateBraking(trace);
        //create a new trace for GPS, since we use GPS to capture driving behaviors
        Trace.Trip ntrace = trace.copyTrace(Trace.Trip.class);
        ntrace.time = trace.time;
        ntrace.score = (float)score_;
        ntrace.brake = brake;
        return ntrace;
    }

    private float calculateBraking(Trace.GPS trace) {
        if(lastTrace_ == null) {
            lastTrace_ = trace;
            return 0;
        }
        double time = trace.time - lastTrace_.time;
        if(time == 0) {
            return 0;
        }
        double curSpeed = Trip.distance(lastTrace_, trace)/(time/1000.0);
        if(lastSpeed_ == -1.0) {
            lastSpeed_ = curSpeed;
            return 0;
        } else {
            counter_++;
        }
        double a = (curSpeed - lastSpeed_)/(time/1000.0);

        lastSpeed_ = curSpeed;
        lastTrace_ = trace;
        if(a < -2.5) {
            double curscore = 3.0 - Math.min(3.0, Math.abs(a));
            score_ = (score_ * (counter_ - 1) + curscore * 10.0)/counter_;
            return (float)a;
        } else {
            score_ = (score_ * (counter_ - 1) + 10.0)/counter_;
            return 0;
        }
    }



}
