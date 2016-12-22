package wisc.drivesense.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;


public class DatabaseHelper extends SQLiteOpenHelper {

    // Logcat tag
    private static final String TAG = "DatabaseHelper";

    // Database Version
    private static final String DATABASE_NAME = "drivesense.db";
    private static final int DATABASE_VERSION = 1;

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
            + TABLE_TRACE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, tripid INTEGER, type TEXT, value TEXT, synced INTEGER,"
            + " FOREIGN KEY(tripid) REFERENCES "+TABLE_TRIP+"(id));";

    //Index Create
    private static final String CREATE_INDEX_TRACE="CREATE INDEX IF NOT EXISTS i1 ON "+ TABLE_TRACE +"(tripid,type)";

    private static final String DROP_TABLE = "DROP TABLE ";

    private SQLiteDatabase wdb;
    private  SQLiteDatabase rdb;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        wdb = this.getWritableDatabase();
        rdb = this.getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRIP);
        db.execSQL(CREATE_TABLE_USER);
        db.execSQL(CREATE_TABLE_TRACE);
        db.execSQL(CREATE_INDEX_TRACE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE+TABLE_TRACE+";");
        db.execSQL(DROP_TABLE+TABLE_TRIP+";");
        db.execSQL(DROP_TABLE+TABLE_USER+";");
        onCreate(db);
    }

    public void onOpen(SQLiteDatabase db) {
        db.execSQL(CREATE_INDEX_TRACE);
        Log.d(TAG, "Created index on traces");
    }


    public void insertTrip(Trip trip) {
        Gson gson = new Gson();
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
        wdb.insert(TABLE_TRIP, null, values);
    }

    /**
     * Insert a list of TraceMessages in a bulk transaction to improve efficiency considerably
     * @param tripUUID UUID of the trip.
     * @param tmList Trace messages to insert
     * @return List of the row IDs of the inserted objects in the order they were given
     * @throws Exception
     */
    public long[] insertSensorData(String tripUUID, ArrayList<TraceMessage> tmList) throws Exception {
        long[] insertIDs = new long[tmList.size()];
        String selectQuery = "SELECT id FROM " + TABLE_TRIP + " WHERE uuid = '" + tripUUID + "';";
        Cursor cursor = rdb.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0)
            throw new Exception();
        int tripID = cursor.getInt(0);
        cursor.close();
        wdb.beginTransaction();
        for (int i = 0; i < tmList.size(); i++) {
            TraceMessage tm = tmList.get(i);
            ContentValues values = new ContentValues();
            values.put("synced", false);
            values.put("value", GsonSingleton.toJson(tm));
            values.put("type", tm.type);
            values.put("tripid", tripID);
            long rowid = wdb.insert(TABLE_TRACE, null, values);
            //rowid is an alias for a column declared as INTEGER PRIMARY KEY, which is id in this case
            //and that is what we want to return
            insertIDs[i] = rowid;
        }
        wdb.setTransactionSuccessful();
        wdb.endTransaction();
        return insertIDs;
    }

    public void markTracesSynced(Long[] traceids) {
        ContentValues values = new ContentValues();
        values.put("synced", 1);
        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (Long i : traceids) {
            sb.append(delim).append(i);
            delim = ",";
        }
        String whereClause = "rowid IN (" + sb.toString() +")";
        wdb.update(TABLE_TRACE, values, whereClause, null);
    }

    public void markTripSynced(String uuid) {
        ContentValues values = new ContentValues();
        values.put("synced", 1);
        wdb.update(TABLE_TRIP, values, "uuid='" + uuid + "'", null);
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
            tm.rowid = cursor.getLong(cursor.getColumnIndex("id"));
            res.add(tm);
        } while (cursor.moveToNext());
        return res;
    }

    public List<TraceMessage> getUnsentTraces(String uuid, int limit) {
        String selectQuery = "SELECT  " + TABLE_TRACE + ".* FROM " + TABLE_TRIP + " left join " + TABLE_TRACE
                + " on trace.tripid = trip.id WHERE trace.synced = 0 and trip.uuid = '" + uuid +"' LIMIT "+limit;
        Cursor cursor = rdb.rawQuery(selectQuery, null);
        List<TraceMessage> res = cursorToTraces(cursor);
        cursor.close();
        return res;
    }
    /**
     * Get the gps points of a trip, which is identified by the start time (the name of the database)
     * @param uuid the id of the trip
     * @return a list of trace, or gps points
     */
    public List<Trace.Trip> getGPSPoints(String uuid) {
        String selectQuery = "SELECT  " + TABLE_TRACE + ".* FROM " + TABLE_TRIP + " left join " + TABLE_TRACE
                + " on trace.tripid = trip.id WHERE type = '" + Trace.Trip.class.getSimpleName() + "' and trip.uuid = '" + uuid +"' ORDER BY id ASC";
        Cursor cursor = rdb.rawQuery(selectQuery, null);
        ArrayList<Trace.Trip> res = new ArrayList<>();
        for (TraceMessage tm : cursorToTraces(cursor)) {
            res.add((Trace.Trip)tm.value);
        }
        cursor.close();
        return res;
    }

    private Trip constructTripByCursor(Cursor cursor) {
        int id = cursor.getInt(0);
        String uuid = cursor.getString(1);
        long stime = cursor.getLong(2);
        long etime = cursor.getLong(3);
        double dist = cursor.getDouble(4);
        double score = cursor.getDouble(5);
        int status = cursor.getInt(6);
        Trip trip = new Trip(id, UUID.fromString(uuid), stime, etime);
        trip.setScore(score);
        trip.setStatus(status);
        trip.setEndTime(etime);
        trip.setDistance(dist);
        return trip;
    }

    public Trip getTrip(String uuid) {
        List<Trip> trips = loadTrips("uuid='"+uuid+"'");
        if(trips.size() != 1) {
            return null;
        } else {
            return trips.get(0);
        }
    }

    public Trip getLastTrip() {
        List<Trip> unfinished = loadTrips();
        if(unfinished.size() < 1) {
            return null;
        } else {
            return unfinished.get(0);
        }
    }

    public void deleteTrip(String uuid) {
        ContentValues values = new ContentValues();
        values.put("status", 0);
        values.put("synced", false);
        wdb.update(TABLE_TRIP, values, "uuid='" + uuid + "'", null);
    }

    /**
     * Finalize the trip with uuid if it's not null otherwise finalize all
     * trips.
     */
    public void finalizeTrips() {
        Log.d(TAG, "finalizing trips");
        ContentValues values = new ContentValues();
        values.put("status", 2);
        wdb.update(TABLE_TRIP, values, "status = 1", null);
    }

    public void updateTrip(Trip trip) {
        ContentValues values = new ContentValues();
        values.put("starttime", trip.getStartTime());
        values.put("endtime", trip.getEndTime());
        values.put("synced", false);
        values.put("score", trip.getScore());
        values.put("distance", trip.getDistance());
        values.put("status", trip.getStatus());
        wdb.update(TABLE_TRIP, values, "uuid='" + trip.uuid + "'", null);
    }

    public List<Trip> loadTrips() { return this.loadTrips(null); }
    public List<Trip> loadTrips(String whereClause) {

        DriveSenseToken user = this.getCurrentUser();
        List<Trip> trips = new ArrayList<Trip>();
        String selectQuery;
        if(user == null) {
            selectQuery = "SELECT  * FROM " + TABLE_TRIP + " WHERE email = '"+"'";

        } else {
            selectQuery = "SELECT  * FROM " + TABLE_TRIP + " WHERE (email = '" + user.email + "' or email = '"+"')";
        }
        if(whereClause != null)
            selectQuery += " and " + whereClause;
        selectQuery += " order by starttime desc;";
        Cursor cursor = wdb.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) {
            return trips;
        }
        do {
            Trip trip = constructTripByCursor(cursor);
            trips.add(trip);
        } while (cursor.moveToNext());
        cursor.close();
        return trips;
    }

    //email TEXT, firstname TEXT, lastname TEXT, dstoken TEXT
/* ========================== UserObject Specific Database Operations =================================== */

    public DriveSenseToken getCurrentUser() {
        DriveSenseToken user;
        String selectQuery = "SELECT  email, firstname, lastname, dstoken FROM " + TABLE_USER;
        Cursor cursor = rdb.rawQuery(selectQuery, null);
        if(cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();
        user = DriveSenseToken.InstantiateFromJWT(cursor.getString(3));
        cursor.close();
        return user;
    }

    public void userLogin(DriveSenseToken token) {
        userLogout();
        ContentValues values = new ContentValues();
        values.put("email", token.email);
        values.put("firstname", token.firstname);
        values.put("lastname", token.lastname);
        values.put("dstoken", token.jwt);
        wdb.insert(TABLE_USER, null, values);
    }

    public void userLogout() {
        Log.d(TAG, "user logout processing in database");
        wdb.delete(TABLE_USER, null, null);
    }
}
