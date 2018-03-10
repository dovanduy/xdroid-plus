package isc.whu.defender.xmonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

public class Util {
	private static boolean mLogDetermined = false;
	private static boolean mLog = true;
	private static boolean mHasLBE = false;
	private static boolean mHasLBEDetermined = false;

	public static int NOTIFY_RESTART = 0;
	public static int NOTIFY_NOTXPOSED = 1;
	public static int NOTIFY_SERVICE = 2;
	public static int NOTIFY_MIGRATE = 3;
	public static int NOTIFY_RANDOMIZE = 4;
	public static int NOTIFY_UPGRADE = 5;

	private static Version MIN_PRO_VERSION = new Version("1.12");
	/**
	 * Maps a primitive class name to its corresponding abbreviation used in
	 * array class names.
	 */
	private static final Map<String, String> abbreviationMap = new HashMap<String, String>();

	public static void bug(XHook hook, Throwable ex) {
		int priority;
		if (ex instanceof OutOfMemoryError)
			priority = Log.WARN;
		// else if (ex instanceof ActivityShare.AbortException)
		// priority = Log.WARN;
		// else if (ex instanceof ActivityShare.ServerException)
		// priority = Log.WARN;
		else if (ex instanceof NoClassDefFoundError)
			priority = Log.WARN;
		else
			priority = Log.ERROR;

		boolean xprivacy = false;
		for (StackTraceElement frame : ex.getStackTrace())
			if (frame.getClassName() != null
					&& frame.getClassName().startsWith("biz.bokhorst.xprivacy")) {
				xprivacy = true;
				break;
			}
		if (!xprivacy)
			priority = Log.WARN;

		log(hook, priority, ex.toString() + " uid=" + Process.myUid() + "\n"
				+ Log.getStackTraceString(ex));
	}

	public static void log(XHook hook, int priority, String msg) {
		// Check if logging enabled
		int uid = Process.myUid();
		if (!mLogDetermined && uid > 0) {
			mLogDetermined = true;
			try {
				mLog = HookManager.getSettingBool(0, HookManager.cSettingLog,
						false, false);
			} catch (Throwable ignored) {
				mLog = false;
			}
		}

		// Log if enabled
		if (priority != Log.DEBUG && (priority == Log.INFO ? mLog : true))
			if (hook == null) {
				Log.println(priority, "XMonitor", msg);
			} else {
				Log.println(priority, String.format("XMonitor/%s", hook
						.getClass().getSimpleName()), msg);
			}

		// Report to service
		// if (uid > 0 && priority == Log.ERROR)
		// if (PrivacyService.isRegistered())
		// PrivacyService.reportErrorInternal(msg);
		// else
		// try {
		// IPrivacyService client = PrivacyService.getClient();
		// if (client != null)
		// client.reportError(msg);
		// } catch (RemoteException ignored) {
		// }
	}

	public static void logStack(XHook hook, int priority) {
		log(hook, priority,
				Log.getStackTraceString(new Exception("StackTrace")));
	}

	// Empty checks
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Checks if a CharSequence is empty ("") or null.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.isEmpty(null)      = true
	 * StringUtils.isEmpty("")        = true
	 * StringUtils.isEmpty(" ")       = false
	 * StringUtils.isEmpty("bob")     = false
	 * StringUtils.isEmpty("  bob  ") = false
	 * </pre>
	 * 
	 * <p>
	 * NOTE: This method changed in Lang version 2.0. It no longer trims the
	 * CharSequence. That functionality is available in isBlank().
	 * </p>
	 * 
	 * @param cs
	 *            the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is empty or null
	 * @since 3.0 Changed signature from isEmpty(String) to
	 *        isEmpty(CharSequence)
	 */
	public static boolean isEmpty(CharSequence cs) {
		return cs == null || cs.length() == 0;
	}

	// Delete
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Deletes all whitespaces from a String as defined by
	 * {@link Character#isWhitespace(char)}.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.deleteWhitespace(null)         = null
	 * StringUtils.deleteWhitespace("")           = ""
	 * StringUtils.deleteWhitespace("abc")        = "abc"
	 * StringUtils.deleteWhitespace("   ab  c  ") = "abc"
	 * </pre>
	 * 
	 * @param str
	 *            the String to delete whitespace from, may be null
	 * @return the String without whitespaces, {@code null} if null String input
	 */
	public static String deleteWhitespace(String str) {
		if (isEmpty(str)) {
			return str;
		}
		int sz = str.length();
		char[] chs = new char[sz];
		int count = 0;
		for (int i = 0; i < sz; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				chs[count++] = str.charAt(i);
			}
		}
		if (count == sz) {
			return str;
		}
		return new String(chs, 0, count);
	}

	/**
	 * Converts a class name to a JLS style class name.
	 * 
	 * @param className
	 *            the class name
	 * @return the converted name
	 */
	static String toCanonicalName(String className) {
		className = deleteWhitespace(className);
		if (className == null) {
			throw new NullPointerException("className must not be null.");
		} else if (className.endsWith("[]")) {
			StringBuilder classNameBuffer = new StringBuilder();
			while (className.endsWith("[]")) {
				className = className.substring(0, className.length() - 2);
				classNameBuffer.append("[");
			}
			String abbreviation = abbreviationMap.get(className);
			if (abbreviation != null) {
				classNameBuffer.append(abbreviation);
			} else {
				classNameBuffer.append("L").append(className).append(";");
			}
			className = classNameBuffer.toString();
		}
		return className;
	}

	public static boolean hasLBE() {
		if (!mHasLBEDetermined) {
			mHasLBEDetermined = true;
			try {
				File apps = new File(Environment.getDataDirectory()
						+ File.separator + "app");
				File[] files = (apps == null ? null : apps.listFiles());
				if (files != null)
					for (File file : files)
						if (file.getName().startsWith("com.lbe.security"))
							mHasLBE = true;
			} catch (Throwable ex) {
				Util.bug(null, ex);
			}
		}
		return mHasLBE;
	}

	@SuppressLint("NewApi")
	public static int getAppId(int uid) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
			try {
				// UserHandle: public static final int getAppId(int uid)
				Class<?> clazz = Class.forName("android.os.UserHandle");
				Method method = (Method) clazz.getDeclaredMethod("getAppId", int.class);
				uid = (Integer) method.invoke(null, uid);
			} catch (Throwable ex) {
				Util.log(null, Log.WARN, ex.toString());
			}
		return uid;
	}

	@SuppressLint("NewApi")
	public static int getUserId(int uid) {
		int userId = 0;
		if (uid > 99) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
				try {
					// UserHandle: public static final int getUserId(int uid)
					Class<?> clazz = Class.forName("android.os.UserHandle");
					Method method = (Method) clazz.getDeclaredMethod("getUserId", int.class);
					userId = (Integer) method.invoke(null, uid);
				} catch (Throwable ex) {
					Util.log(null, Log.WARN, ex.toString());
				}
		} else
			userId = uid;
		return userId;
	}

	public static String getUserDataDirectory(int uid) {
		// Build data directory
		String dataDir = Environment.getDataDirectory() + File.separator;
		int userId = getUserId(uid);
		if (userId == 0)
			dataDir += "data";
		else
			dataDir += "user" + File.separator + userId;
		dataDir += File.separator + Util.class.getPackage().getName();
		return dataDir;
	}

	public static void copy(File src, File dst) throws IOException {
		FileInputStream inStream = null;
		try {
			inStream = new FileInputStream(src);
			FileOutputStream outStream = null;
			try {
				outStream = new FileOutputStream(dst);
				FileChannel inChannel = inStream.getChannel();
				FileChannel outChannel = outStream.getChannel();
				inChannel.transferTo(0, inChannel.size(), outChannel);
			} finally {
				if (outStream != null)
					outStream.close();
			}
		} finally {
			if (inStream != null)
				inStream.close();
		}
	}

	public static boolean move(File src, File dst) {
		try {
			copy(src, dst);
		} catch (IOException ex) {
			Util.bug(null, ex);
			return false;
		}
		return src.delete();
	}

	public static void setPermissions(String path, int mode, int uid, int gid) {
		try {
			// frameworks/base/core/java/android/os/FileUtils.java
			Class<?> fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions",
					String.class, int.class, int.class, int.class);
			setPermissions.invoke(null, path, mode, uid, gid);
			Util.log(null, Log.WARN, "Changed permission path=" + path
					+ " mode=" + Integer.toOctalString(mode) + " uid=" + uid
					+ " gid=" + gid);
		} catch (Throwable ex) {
			Util.bug(null, ex);
		}
	}

	public static String sha1(String text) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		// SHA1
		String salt = HookManager.getSetting(0, HookManager.cSettingSalt, "",
				true);
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] bytes = (text + salt).getBytes("UTF-8");
		digest.update(bytes, 0, bytes.length);
		bytes = digest.digest();
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(String.format("%02X", b));
		return sb.toString();
	}

	public static int getXposedAppProcessVersion() {
		final Pattern PATTERN_APP_PROCESS_VERSION = Pattern
				.compile(".*with Xposed support \\(version (.+)\\).*");
		try {
			InputStream is = new FileInputStream("/system/bin/app_process");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.contains("Xposed"))
					continue;
				Matcher m = PATTERN_APP_PROCESS_VERSION.matcher(line);
				if (m.find()) {
					br.close();
					is.close();
					return Integer.parseInt(m.group(1));
				}
			}
			br.close();
			is.close();
		} catch (Throwable ex) {
		}
		return -1;
	}

	public static boolean isXposedEnabled() {
		// Will be hooked to return true
		log(null, Log.WARN, "XPrivacy not enabled");
		return false;
	}

	public static Version getProEnablerVersion(Context context) {
		try {
			String proPackageName = context.getPackageName() + ".pro";
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(proPackageName, 0);
			return new Version(pi.versionName);
		} catch (NameNotFoundException ignored) {
		} catch (Throwable ex) {
			Util.bug(null, ex);
		}
		return null;
	}

	public static boolean isValidProEnablerVersion(Version version) {
		return (version.compareTo(MIN_PRO_VERSION) >= 0);
	}

	@SuppressLint("DefaultLocale")
	public static boolean hasValidFingerPrint(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			String packageName = context.getPackageName();
			PackageInfo packageInfo = pm.getPackageInfo(packageName,
					PackageManager.GET_SIGNATURES);
			byte[] cert = packageInfo.signatures[0].toByteArray();
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			byte[] bytes = digest.digest(cert);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < bytes.length; ++i)
				sb.append((Integer.toHexString((bytes[i] & 0xFF) | 0x100))
						.substring(1, 3).toLowerCase());
			String calculated = sb.toString();
			return true;
		} catch (Throwable ex) {
			bug(null, ex);
			return false;
		}
	}

	public static boolean isDebuggable(Context context) {
		return ((context.getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
	}

	public static String getSelfVersionName(Context context) {
		try {
			String self = Util.class.getPackage().getName();
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(self, 0);
			return pInfo.versionName;
		} catch (NameNotFoundException ex) {
			Util.bug(null, ex);
			return null;
		}
	}

}
