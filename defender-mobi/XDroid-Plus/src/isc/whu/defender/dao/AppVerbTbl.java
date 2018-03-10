package isc.whu.defender.dao;

/**
 * App详细信息
 * @author Wind
 *
 */
public class AppVerbTbl {
	
	public static final int NOT_EXIST 	= 0;
	public static final int REJECT 		= 1;
	public static final int REQUEST 	= 2;
	public static final int ALLOW 		= 3;
	
	public static final String SELF = "app_verbose";
	
	public static final String COL_ID 		= "_id";
	public static final String COL_UID 		= "uid";
	public static final String COL_NAME 	= "name";
	public static final String COL_PKGNAME 	= "pkg_name";
	
	// 权限
	public static final String COL_SEND_SMS = "send_sms";
	public static final String COL_RECV_SMS = "recv_sms";
	public static final String COL_STATE	= "phone_state";
	public static final String COL_CALL 	= "make_call";
	public static final String COL_REC 		= "record";
	public static final String COL_CAMERA 	= "camera";
	public static final String COL_LOC_NET 	= "loc_network";
	public static final String COL_LOC_GPS 	= "loc_gps";
	public static final String COL_CONTACTS = "read_contacts";
	public static final String COL_BRO_HIS 	= "browser_history";
	
	public static final String COL_ENTRY 	= "launch_entry";
	public static final String COL_SYS_APP  = "sys_app";
	public static final String COL_EXT1 	= "ext1";
	public static final String COL_EXT2 	= "ext2";
	
	public static final String CREATE = 
			"CREATE TABLE " + SELF + "( " +
				COL_ID 			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COL_UID 		+ " INTEGER NOT NULL, " +
				COL_NAME 		+ " VARCHAR(128) NOT NULL, " +
				COL_PKGNAME 	+ " VARCHAR(128) NOT NULL, " +
				
				COL_SEND_SMS	+ " INTEGER DEFAULT(0), " +
				COL_RECV_SMS	+ " INTEGER DEFAULT(0), " +
				COL_STATE		+ " INTEGER DEFAULT(0), " +
				COL_CALL		+ " INTEGER DEFAULT(0), " +
				COL_REC			+ " INTEGER DEFAULT(0), " +
				COL_CAMERA		+ " INTEGER DEFAULT(0), " +
				COL_LOC_NET		+ " INTEGER DEFAULT(0), " +
				COL_LOC_GPS		+ " INTEGER DEFAULT(0), " +
				COL_CONTACTS	+ " INTEGER DEFAULT(0), " +
				COL_BRO_HIS		+ " INTEGER DEFAULT(0), " +
				
				COL_ENTRY 		+ " INTEGER, " +
				COL_SYS_APP 	+ " INTEGER DEFAULT(0), " +
				COL_EXT1 		+ " VARCHAR(128), " +
				COL_EXT2 		+ " VARCHAR(128) " +
			");";
}
