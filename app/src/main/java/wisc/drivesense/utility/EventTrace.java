package wisc.drivesense.utility;

public class EventTrace {
	public long start_;
	public long end_;
	public String type_;
	public double steering_sum_ = 0.0; //the sum of the angle change
	public double steering_abs_sum_ = 0.0; // the sum of the absolute angle change, in lane change, the steering_sum may be zero, but not the absolute steering
	
	
	public static String TURN = "turn";
	public static String LANECHANGE = "lane_change";
	public static String CURVING = "curving";
	public EventTrace() {};
}
