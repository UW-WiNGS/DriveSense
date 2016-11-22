package wisc.drivesense.database;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;


public class DatabaseHelper {

    // Logcat tag
    private static final String TAG = "DatabaseHelper";

    private SQLiteDatabase db_ = null;

    // Database Version
    private static final String DATABASE_NAME = "summary.db";

    // Table Names
    private static final String TABLE_USER = "user";
    private static final String TABLE_TRIP = "trip";
    private static final String TABLE_TRACE = "trace";

    // Table Create Statements
    private static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS "
            + TABLE_USER + "(email TEXT, firstname TEXT, lastname TEXT, dstoken TEXT);";

    private static final String CREATE_TABLE_TRIP = "CREATE TABLE IF NOT EXISTS "
            + TABLE_TRIP + "(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT, starttime INTEGER, endtime INTEGER,"
            + " distance REAL, score REAL, status INTEGER, synced INTEGER, email TEXT);";

    private static final String CREATE_TABLE_TRACE= "CREATE TABLE IF NOT EXISTS "
            + TABLE_TRACE + "(tripid INTEGER, id INTEGER, type TEXT, value TEXT, synced INTEGER,"
            + " FOREIGN KEY(tripid) REFERENCES "+TABLE_TRIP+"(id));";



    private boolean opened = false;
    private static DatabaseHelper _single = null;
    public static DatabaseHelper single() {
        if(_single == null)
            _single = new DatabaseHelper();
        return _single;
    }
    // public interfaces
    public DatabaseHelper() {
        this.opened = true;
        //this.context = cont;
        //openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.CREATE_IF_NECESSARY, null);
        //File dir = this.context.getFilesDir();
        db_ = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + DATABASE_NAME, null, null);
        db_.execSQL(CREATE_TABLE_TRIP);
        db_.execSQL(CREATE_TABLE_USER);
        db_.execSQL(CREATE_TABLE_TRACE);

    }


    public void closeDatabase() {
        this.opened = false;
        if(db_ != null && db_.isOpen()) {
            db_.close();
        }
    }
    public boolean isOpen() {
        return this.opened;
    }

    public void insertTrip(Trip trip) {
        Gson gson = new Gson();
        Log.d(TAG, "insertTrip" + gson.toJson(trip));
        ContentValues values = new ContentValues();
        values.put("uuid", trip.uuid.toString());
        values.put("starttime", trip.getStartTime());
        values.put("endtime", trip.getEndTime());
        values.put("distance", trip.getDistance());
        values.put("score", trip.getScore());
        values.put("status", 1);
        values.put("synced", 0);
        //assign to current user
        DriveSenseToken user = this.getCurrentUser();
        if(user != null) {
            values.put("email", user.email);
        } else {
            //return;
            values.put("email", "");
        }
        db_.insert(TABLE_TRIP, null, values);
    }

    public long insertSensorData(String tripUUID, Trace trace) throws Exception {
        String selectQuery = "SELECT id FROM " + TABLE_TRIP + " WHERE uuid = '" + tripUUID + "';";
        Cursor cursor = db_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0)
            throw new Exception();
        int tripID = cursor.getInt(0);
        ContentValues values = new ContentValues();
        TraceMessage tm = new TraceMessage(trace);
        values.put("synced", false);
        values.put("value", GsonSingleton.toJson(tm));
        values.put("type", tm.type);
        values.put("tripid", tripID);
        return db_.insert(TABLE_TRACE, null, values);
    }

    public void markTraceSynced(Long traceid) {
        ContentValues values = new ContentValues();
        values.put("synced", 1);
        db_.update(TABLE_TRACE, values, "rowid=" + traceid.toString(), null);
    }

    private List<TraceMessage> cursorToTraces(Cursor cursor) {
        List<TraceMessage> res = new ArrayList<TraceMessage>();
        cursor.moveToFirst();
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            String value = cursor.getString(cursor.getColumnIndex("value"));
            TraceMessage tm = GsonSingleton.fromJson(value, TraceMessage.class);
            res.add(tm);
        } while (cursor.moveToNext());
        return res;
    }

    public List<TraceMessage> getUnsentTraces(String uuid) {
        String selectQuery = "SELECT  " + TABLE_TRACE + ".* FROM " + TABLE_TRIP + " left join " + TABLE_TRACE
                + " on trace.tripid = trip.id WHERE trace.synced = 0 and trip.uuid = '" + uuid +"'";
        Cursor cursor = db_.rawQuery(selectQuery, null);
        return cursorToTraces(cursor);
    }
    /**
     * @brief get the gps points of a trip, which is identified by the start time (the name of the database)
     * @param uuid the id of the trip
     * @return a list of trace, or gps points
     */
    public List<Trace.Trip> getGPSPoints(String uuid) {
        String selectQuery = "SELECT  " + TABLE_TRACE + ".* FROM " + TABLE_TRIP + " left join " + TABLE_TRACE
                + " on trace.tripid = trip.id WHERE type = '" + Trace.Trip.class.getSimpleName() + "' and trip.uuid = '" + uuid +"'";
        Cursor cursor = db_.rawQuery(selectQuery, null);
        ArrayList<Trace.Trip> res = new ArrayList<>();
        for (TraceMessage tm : cursorToTraces(cursor)) {
            res.add((Trace.Trip)tm.value);
        }
        return res;
    }

    private Trip constructTripByCursor(Cursor cursor) {
        int id = cursor.getInt(0);
        String uuid = cursor.getString(1);
        long stime = cursor.getLong(2);
        long etime = cursor.getLong(3);
        double dist = cursor.getDouble(4);
        double score = cursor.getDouble(5);
        int deleted = cursor.getInt(6);
        Trip trip = new Trip(id, UUID.fromString(uuid), stime, etime);
        trip.setScore(score);
        trip.setStatus(deleted == 1? 0 : 1);
        trip.setEndTime(etime);
        trip.setDistance(dist);
        return trip;
    }

    public Trip getTrip(String uuid) {
        String selectQuery = "SELECT id, uuid, starttime, endtime, distance, score, deleted FROM " + TABLE_TRIP + " WHERE uuid = '" + uuid + "';";
        Cursor cursor = db_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        Trip trip = null;
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            trip = constructTripByCursor(cursor);
        } while (cursor.moveToNext());
        return trip;
    }

    public void deleteTrip(String uuid) {
        db_.delete(TABLE_TRIP, "uuid = '" + uuid + "'", null);
    }

    public void finalizeTrip(String uuid) {
        ContentValues values = new ContentValues();
        values.put("status", 2);
        db_.update(TABLE_TRIP, values, "uuid = '" + uuid + "';", null);
    }
    public List<Trip> loadTrips() { return this.loadTrips(null); }
    public List<Trip> loadTrips(String whereClause) {
        DriveSenseToken user = this.getCurrentUser();
        List<Trip> trips = new ArrayList<Trip>();
        String selectQuery = "";
        if(user == null) {
            selectQuery = "SELECT  * FROM " + TABLE_TRIP + " WHERE email = '"+"'";

        } else {
            selectQuery = "SELECT  * FROM " + TABLE_TRIP + " WHERE email = '" + user.email + "' or email = '"+"'";
        }
        if(whereClause != null)
            selectQuery += " and " + whereClause;
        selectQuery += " order by starttime desc;";
        Cursor cursor = db_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) {
            return trips;
        }
        do {
            int deleted = cursor.getInt(4);
            if(deleted >= 1) {
                continue;
            }
            Trip trip = constructTripByCursor(cursor);
            trips.add(trip);
            if(trips.size() >= Constants.kNumberOfTripsDisplay) {
                break;
            }
        } while (cursor.moveToNext());
        return trips;
    }


    public long[] tripsToSynchronize (String useremail) {
        String selectQuery = "SELECT  * FROM " + TABLE_TRIP + " WHERE uploaded = 1 and deleted = 1 and " + " email = '" + useremail + "';";
        Cursor cursor = db_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) {
            return null;
        }
        long [] stime = new long[cursor.getCount()];
        int i = 0;
        do {
            stime[i++] = cursor.getLong(0);
        } while (cursor.moveToNext());
        return stime;
    }
    public int tripSynchronizeDone(long time) {
        ContentValues data = new ContentValues();
        data.put("deleted", 2);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        return db_.update(TABLE_TRIP, data, where, whereArgs);
    }
    /**
     * all about uploading
     */
    public long nextTripToUpload(String useremail) {
        String selectQuery = "SELECT  * FROM " + TABLE_TRIP + " WHERE uploaded = 0 and " + " email = '" + useremail + "';";
        Cursor cursor = db_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        long stime = -1;
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            stime = cursor.getLong(0);
            break;
        } while (cursor.moveToNext());
        return stime;
    }


    public void tripRemoveSensorData(long time) {
        String [] tables = {TABLE_TRACE};
        SQLiteDatabase tmpdb = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + String.valueOf(time).concat(".db"), null, null);
        for(int i = 0; i < tables.length; ++i) {
            String dropsql = "DROP TABLE IF EXISTS " + tables[i] + ";";
            tmpdb.execSQL(dropsql);
        }
        tmpdb.close();
    }
    /**
     * label the meta table that the trip has been uploaded, and remove all the sensor tables, leave gps table
     * @param time
     */
    public int tripUploadDone(long time) {
        Log.d(TAG, "tripUploadDone");

        //drop the sensor tables to avoid space waste
        tripRemoveSensorData(time);

        //update information in meta table
        ContentValues data = new ContentValues();
        data.put("uploaded", 1);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        return db_.update(TABLE_TRIP, data, where, whereArgs);
    }



    //email TEXT, firstname TEXT, lastname TEXT, dstoken TEXT
    /* ========================== UserObject Specific Database Operations =================================== */

    public DriveSenseToken getCurrentUser() {
        DriveSenseToken user = null;
        String selectQuery = "SELECT  email, firstname, lastname, dstoken FROM " + TABLE_USER;
        Cursor cursor = db_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            user = DriveSenseToken.InstantiateFromJWT(cursor.getString(3));
            break;
        } while (cursor.moveToNext());
        return user;
    }

    public void userLogin(DriveSenseToken token) {
        userLogout();
        ContentValues values = new ContentValues();
        values.put("email", token.email);
        values.put("firstname", token.firstname);
        values.put("lastname", token.lastname);
        values.put("dstoken", token.jwt);
        db_.insert(TABLE_USER, null, values);
    }

    public void userLogout() {
        Log.d(TAG, "user logout processing in database");
        db_.delete(TABLE_USER, null, null);
    }

}
