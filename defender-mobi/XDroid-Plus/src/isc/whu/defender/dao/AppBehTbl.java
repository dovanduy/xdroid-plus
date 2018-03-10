package isc.whu.defender.dao;

public class AppBehTbl {
	public static final String SELF = "app_beh";

	public static final String COL_UID = "uid";
	public static final String COL_TYPE = "type";
	public static final String COL_ACTION = "action";
	public static final int ACTION_ALLOW = 1;
	public static final int ACTION_REFUSE = 2;
	public static final int ACTION_PROMPT = 3;
	public static final int AppBehAction[] = new int[] { 1, 2, 3 };
	// 1-Allow 2-Refuse 3-Alert

	public static final String CREATE = "CREATE TABLE " + SELF + "( " + COL_UID
			+ " INTEGER NOT NULL, " + COL_TYPE + " INTEGER DEFAULT(0), "
			+ COL_ACTION + " INTEGER DEFAULT(0) " + ", PRIMARY KEY (" + COL_UID
			+ "," + COL_TYPE + ")" + ");";
	// primary key : uid & type
}
