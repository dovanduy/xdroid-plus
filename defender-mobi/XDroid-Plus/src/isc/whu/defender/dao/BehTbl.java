package isc.whu.defender.dao;

/**
 * 每个App的行为信息
 * @author Wind
 *
 */
public class BehTbl {
	
	public static final String SELF = "beh_detail";
	
	public static final String COL_ID = "_id";
	public static final String COL_UID = "uid";
	public static final String COL_TYPE = "type";
	public static final String COL_DETAIL = "detail";
	public static final String COL_TIME = "time";

	public static final String CREATE = 
			"CREATE TABLE " + SELF + "( " +
					COL_ID 	+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COL_UID + " INTEGER NOT NULL, " +
					COL_TYPE 	+ " INTEGER DEFAULT(0), " +
					COL_DETAIL 	+ " VARCHAR(128), " +
					COL_TIME 	+ " TIMESTAMP NOT NULL DEFAULT(datetime('now','localtime')) " +
			");";

}
