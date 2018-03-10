package isc.whu.defender.dao;

/**
 * 每日总流量
 * @author Wind
 *
 */
public class TrafTbl {
	public static final String SELF = "traffic";
	
	public static final String COL_ID = "_id";
	public static final String COL_UID = "uid";
	public static final String COL_UP = "upload";
	public static final String COL_DOWN = "download";
	public static final String COL_TIME = "time";
	
	public static final String CREATE = 
			"CREATE TABLE " + SELF + "( " +
					COL_ID 	+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COL_UID	+ " INTEGER NOT NULL, " +
					COL_UP 	+ " LONG DEFAULT(0), " +
					COL_DOWN + " LONG DEFAULT(0), " +
					COL_TIME + " TIMESTAMP" +
			");";
}
