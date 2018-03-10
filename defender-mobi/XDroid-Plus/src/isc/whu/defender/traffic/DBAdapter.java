package isc.whu.defender.traffic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {
	public static final String KEY_ROWID = "_id";
	public static final String KEY_UID = "_uid";
	public static final String KEY_NOWTIME = "nowtime";
	public static final String KEY_FLAG = "flag";

	public static final String KEY_NET = "net";

	private static final String TAG = "DBAdapter";
	private static final String DATABASE_NAME = "monitor";
	private static final String DATABASE_TABLE = "program";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_CREATE = "create table program(_id integer primary key autoincrement, "
			+ "_uid long, nowtime nvarchar(128), flag int, net long) ";
	private final Context context;
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS titles");
			onCreate(db);
		}
	}


	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}


	public void close() {
		DBHelper.close();
	}

	public long insertTitle(long uid, String nowtime,
			int flag, double net) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_UID, uid);
		initialValues.put(KEY_NOWTIME, nowtime);
		initialValues.put(KEY_FLAG, flag);
		initialValues.put(KEY_NET, net);
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	public boolean deleteTitle(long rowId) {
		return db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	public long getTitle(long uid, String time, int flag) throws SQLException {
		Cursor mCursor = db.query(DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_UID, KEY_NOWTIME, KEY_FLAG, KEY_NET }, KEY_UID + " = "
				+ uid + " AND " + KEY_NOWTIME + " = '" + time + "' AND " + KEY_FLAG + " = " + flag 
				, null, null, null, null);

		if (mCursor.moveToFirst()) {
			long net0 = mCursor.getLong(mCursor.getColumnIndex(KEY_NET));
			return net0;
		} else
			return -1;

	}


	public boolean updateTitle(long rowId, long uid,
			String nowtime, int flag, double net) {
		ContentValues args = new ContentValues();
		args.put(KEY_UID, uid);
		args.put(KEY_NOWTIME, nowtime);
		args.put(KEY_FLAG, flag);
		args.put(KEY_NET, net);
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

}