package isc.whu.defender.common;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;

/**
 * 日志记录
 * 
 * @author Tina
 * 
 */
public class DebugLog {
	private static String file = null; // defender log
	// private static final String file = "/sdcard/dlog"; 2.1及以下版本sd挂载在根目录下
	public DebugLog (Context ctx){
		file = ctx.getDir("dbg", 0).getAbsolutePath() + "/debug.txt";
	}
	/**
	 * 写一条新日志
	 * 
	 * @param log
	 *            日志内容
	 */
	public static void write(String log) {
		String line = getLogTime() + " " + log + "\n";
		try {
			FileOutputStream fout = new FileOutputStream(file, true);
			BufferedOutputStream out = new BufferedOutputStream(fout);
			out.write(line.getBytes());

			out.close();
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取并格式化当前时间
	 * 
	 * @return 格式化后的时间
	 */
	private static String getLogTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();

		return sdf.format(c.getTimeInMillis());
	}
}