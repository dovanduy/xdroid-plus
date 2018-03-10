package isc.whu.defender.common;

import isc.whu.defender.XDroidDefender;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import android.Manifest.permission;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class CommonTools {
	public static HashMap<String, Object> SensitivePermissionsMap = new HashMap<String, Object>();
	private static int JAR_LATEST_VERSION = -1;

	static {
		SensitivePermissionsMap.put(permission.WRITE_SMS, 1);
		SensitivePermissionsMap.put(permission.SEND_SMS, 2);
		SensitivePermissionsMap.put(permission.READ_PHONE_STATE, 2);
		SensitivePermissionsMap.put(permission.PROCESS_OUTGOING_CALLS, 5);
		SensitivePermissionsMap.put(permission.RECORD_AUDIO, 5);
	}

	public static final int THRESHOLD = 12;

	/**
	 * 获取并格式化当前时间
	 * 
	 * @return 格式化后的时间
	 */
	public static String getLogTime() {
		SimpleDateFormat sdf = new SimpleDateFormat(" MM-dd HH:mm:ss ");
		Calendar c = Calendar.getInstance();

		return sdf.format(c.getTimeInMillis());
	}

	public static int getJarLatestVersion() {
		if (JAR_LATEST_VERSION == -1) {
			try {
				JAR_LATEST_VERSION = getJarVersion(XDroidDefender.getInstance()
						.getAssets().open("XposedBridge.jar"));
			} catch (IOException e) {
				JAR_LATEST_VERSION = 0;
			}
		}
		return JAR_LATEST_VERSION;
	}

	private static int extractIntPart(String str) {
		int result = 0, length = str.length();
		for (int offset = 0; offset < length; offset++) {
			char c = str.charAt(offset);
			if ('0' <= c && c <= '9')
				result = result * 10 + (c - '0');
			else
				break;
		}
		return result;
	}

	private static int getJarVersion(InputStream is) throws IOException {
		JarInputStream jis = new JarInputStream(is);
		JarEntry entry;
		try {
			while ((entry = jis.getNextJarEntry()) != null) {
				if (!entry.getName().equals("assets/VERSION"))
					continue;

				BufferedReader br = new BufferedReader(new InputStreamReader(
						jis));
				String version = br.readLine();
				is.close();
				br.close();
				return extractIntPart(version);
			}
		} finally {
			try {
				jis.close();
			} catch (Exception e) {
			}
		}
		return 0;
	}

	/*
	 * provide shared preferences
	 */
	public static final String DEFAULT_PREF_NAME = "global_preference";

	public static SharedPreferences getPreferences(Context context) {
		return context.getSharedPreferences(DEFAULT_PREF_NAME,
				Context.MODE_PRIVATE);
	}

	public static SharedPreferences getPreferences(Context context,
			String prefName) {
		return context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
	}

	/**
	 * execute shell command as root
	 * 
	 * @param command
	 *            the command that will be executed as root
	 */
	public static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				Log.e("RootCommand", "run root command error");
				e.printStackTrace();
			}
		}
		return true;
	}
}
