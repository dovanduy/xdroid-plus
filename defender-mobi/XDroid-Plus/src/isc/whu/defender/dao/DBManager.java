package isc.whu.defender.dao;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 一个简单的DAO
 * 
 * @author shaoyuru@whu.edu.cn Created: 2012/04/01 Last modified: 2012/05/15
 */
public class DBManager{

	private static final String mDbName = "app.db";
	private static final String LOG_TAG = "DBManager";

	private static DBManager mInstance = null;
	private static DatabaseOpenHelper mHelper = null;

	/**
	 * 获得DBManager实例
	 * 
	 * @param ctx
	 *            
	 * @return DBManager
	 */
	public static synchronized DBManager getInstance(Context ctx) {
		if (mInstance == null) {
			mInstance = new DBManager(ctx);
		}

		return mInstance;
	}

	private DBManager(Context ctx) {
		mHelper = new DatabaseOpenHelper(ctx, mDbName);
	}

	/**
	 * 插入一条记录
	 * 
	 * @param tblName
	 *            表名
	 * @param values
	 *           需要插入的数据
	 */
	public void insert(String tblName, ContentValues values) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.insert(tblName, null, values);
		db.close();
	}

	/**
	 * 插入多条记录
	 * 
	 * @param tblName
	 *            表名
	 * @param valuesList
	 *            需要插入的数据
	 */
	public void insert(String tblName, List<ContentValues> valuesList) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.beginTransaction();

		for (int i = 0; i < valuesList.size(); i++) {
			db.insert(tblName, null, valuesList.get(i));
		}

		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}

	public void update(String tblName, ContentValues values,
			String whereClause, String[] whereArgs) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.update(tblName, values, whereClause, whereArgs);
		db.close();
	}

	public Cursor query(String tblName, String[] columns, String selection,
			String[] selectionArgs, String groupBy, String having,
			String orderBy) {

		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor c = db.query(tblName, columns, selection, selectionArgs,
				groupBy, having, orderBy);

		return c;
	}

	private class DatabaseOpenHelper extends SQLiteOpenHelper {

		public static final int DB_VERSION = 1;

		public DatabaseOpenHelper(Context ctx, String dbName) {
			super(ctx, dbName, null, DB_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			Log.i(LOG_TAG, "Start creating tables...");
			
			db.execSQL(AppBehTbl.CREATE);
			db.execSQL(AppVerbTbl.CREATE);
			db.execSQL(BehTbl.CREATE);
			db.execSQL(TrafTbl.CREATE);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}
	}

}
