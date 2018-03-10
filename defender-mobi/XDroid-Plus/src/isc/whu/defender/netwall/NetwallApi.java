package isc.whu.defender.netwall;

import isc.whu.defender.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class NetwallApi {

	private static final String SCRIPT_FILE = "Netwall.sh";
	public static final String PREFS_NAME = "NetwallPrefs";
	public static final String PREFS_3G = "Banned3G";
	public static final String PREFS_WIFI = "BannedWiFi";
	public static final String PREFS_ENABLED = "Enabled";

	public static NetApps apps[] = null;

	private static boolean hasroot = false;

	public static void alert(Context ctx, CharSequence msg) {
		if (ctx != null) {
			new AlertDialog.Builder(ctx)
					.setNeutralButton(android.R.string.ok, null)
					.setMessage(msg).show();
		}
	}

	private static String scriptHeader(Context ctx) {
		final String dir = ctx.getDir("bin", 0).getAbsolutePath();
		// System.out.println(dir + '\n');
		final String myiptables = dir + "/iptables";
		return "" + "IPTABLES=iptables\n" + "BUSYBOX=busybox\n" + "GREP=grep\n"
				+ "ECHO=echo\n" + "# Try to find busybox\n"
				+ "if "
				+ dir
				+ "/busybox --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX="
				+ dir
				+ "/busybox\n"
				+ "	GREP=\"$BUSYBOX grep\"\n"
				+ "	ECHO=\"$BUSYBOX echo\"\n"
				+ "elif busybox --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX=busybox\n"
				+ "elif /system/xbin/busybox --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX=/system/xbin/busybox\n"
				+ "elif /system/bin/busybox --help >/dev/null 2>/dev/null ; then\n"
				+ "	BUSYBOX=/system/bin/busybox\n"
				+ "fi\n"
				+ "# Try to find grep\n"
				+ "if ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n"
				+ "	if $ECHO 1 | $BUSYBOX grep -q 1 >/dev/null 2>/dev/null ; then\n"
				+ "		GREP=\"$BUSYBOX grep\"\n"
				+ "	fi\n"
				+ "	# Grep is absolutely required\n"
				+ "	if ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n"
				+ "		$ECHO The grep command is required. FireWall will not work.\n"
				+ "		exit 1\n"
				+ "	fi\n"
				+ "fi\n"
				+ "# Try to find iptables\n"
				+ "if "
				+ myiptables
				+ " --version >/dev/null 2>/dev/null ; then\n"
				+ "	IPTABLES="
				+ myiptables + "\n" + "fi\n" + "";
	}

	private static void copyRawFile(Context ctx, int resid, File file,
			String mode) throws IOException, InterruptedException {
		final String abspath = file.getAbsolutePath();
		// Write the iptables binary
		final FileOutputStream out = new FileOutputStream(file);
		final InputStream is = ctx.getResources().openRawResource(resid);
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		// Change the permissions
		Runtime.getRuntime().exec("chmod " + mode + " " + abspath).waitFor();
	}

	private static boolean applyIptablesRulesImpl(Context ctx,
			List<Integer> uidsWifi, List<Integer> uids3g) {
		if (ctx == null)
			return false;
		assertBinaries(ctx);
		final String CONN_WIFI[] = { "tiwlan+", "wlan+", "eth+", "ra+" };
		final String CONN_3G[] = { "rmnet+", "pdp+", "ppp+", "uwbr+", "wimax+",
				"vsnet+", "ccmni+", "usb+" };

		final StringBuilder sc = new StringBuilder();
		try {
			sc.append(scriptHeader(ctx));
			sc.append(""
					+ "$IPTABLES --version || exit 1\n"
					+ "# Create the netwall chains if necessary\n"
					+ "$IPTABLES -L netwall >/dev/null 2>/dev/null || $IPTABLES --new netwall || exit 2\n"
					+ "$IPTABLES -L netwall-3g >/dev/null 2>/dev/null || $IPTABLES --new netwall-3g || exit 3\n"
					+ "$IPTABLES -L netwall-wifi >/dev/null 2>/dev/null || $IPTABLES --new netwall-wifi || exit 4\n"
					+ "$IPTABLES -L OUTPUT | $GREP -q netwall || $IPTABLES -A OUTPUT -j netwall || exit 5\n"
					+ "# Flush existing rules\n"
					+ "$IPTABLES -F netwall || exit 6\n"
					+ "$IPTABLES -F netwall-3g || exit 7\n"
					+ "$IPTABLES -F netwall-wifi || exit 8\n" + "");

			sc.append("# Main rules\n");

			for (final String conn : CONN_3G) {
				sc.append("$IPTABLES -A netwall -o ").append(conn)
						.append(" -j netwall-3g || exit\n");
			}
			for (final String conn : CONN_WIFI) {
				sc.append("$IPTABLES -A netwall -o ").append(conn)
						.append(" -j netwall-wifi || exit\n");
			}

			for (final Integer uid : uids3g) {
				if (uid >= 0) {
					sc.append("$IPTABLES -A netwall-3g -m owner --uid-owner ")
							.append(uid).append(" -j REJECT || exit\n");
				}
			}
			for (final Integer uid : uidsWifi) {
				if (uid >= 0) {
					sc.append("$IPTABLES -A netwall-wifi -m owner --uid-owner ")
							.append(uid).append(" -j REJECT || exit\n");
				}
			}

			final StringBuilder res = new StringBuilder();
			int code = runScriptAsRoot(ctx, sc.toString(), res);
			if (code != 0) {
				String msg = res.toString();
				// Remove unnecessary help message from output
				if (msg.indexOf("\nTry `iptables -h' or 'iptables --help' for more information.") != -1) {
					msg = msg
							.replace(
									"\nTry `iptables -h' or 'iptables --help' for more information.",
									"");
				}
				alert(ctx, "Error applying iptables rules. Exit code: " + code
						+ "\n\n" + msg.trim());
			} else {
				return true;
			}
		} catch (Exception e) {
			alert(ctx, "error refreshing iptables: " + e);
		}
		return false;
	}

	public static boolean applySavedIptablesRules(Context ctx) {
		if (ctx == null) {
			return false;
		}
		final SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		final String savedUids_wifi = prefs.getString(PREFS_WIFI, "");
		final String savedUids_3g = prefs.getString(PREFS_3G, "");
		final List<Integer> uids_wifi = new LinkedList<Integer>();
		if (savedUids_wifi.length() > 0) {
			final StringTokenizer tok = new StringTokenizer(savedUids_wifi, "|");
			while (tok.hasMoreTokens()) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						uids_wifi.add(Integer.parseInt(uid));
					} catch (Exception ex) {
					}
				}
			}
		}
		final List<Integer> uids_3g = new LinkedList<Integer>();
		if (savedUids_3g.length() > 0) {
			final StringTokenizer tok = new StringTokenizer(savedUids_3g, "|");
			while (tok.hasMoreTokens()) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						uids_3g.add(Integer.parseInt(uid));
					} catch (Exception ex) {
					}
				}
			}
		}
		return applyIptablesRulesImpl(ctx, uids_wifi, uids_3g);
	}

	public static boolean applyIptablesRules(Context ctx) {
		if (ctx == null) {
			return false;
		}
		saveRules(ctx);
		return applySavedIptablesRules(ctx);
	}

	public static void saveRules(Context ctx) {
		final SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		final NetApps[] apps = getApps(ctx);
		final StringBuilder newuids_wifi = new StringBuilder();
		final StringBuilder newuids_3g = new StringBuilder();
		for (int i = 0; i < apps.length; i++) {
			if (apps[i].selected_wifi) {
				if (newuids_wifi.length() != 0) {
					newuids_wifi.append('|');
				}
				newuids_wifi.append(apps[i].uid);
			}
			if (apps[i].selected_3g) {
				if (newuids_3g.length() != 0) {
					newuids_3g.append('|');
				}
				newuids_3g.append(apps[i].uid);
			}
		}
		// save the new list of UIDs
		final Editor edit = prefs.edit();
		edit.putString(PREFS_WIFI, newuids_wifi.toString());
		edit.putString(PREFS_3G, newuids_3g.toString());
		edit.commit();
	}

	public static boolean purgeIptables(Context ctx) {
		final StringBuilder sb = new StringBuilder();
		try {
			assertBinaries(ctx);
			final StringBuilder sc = new StringBuilder();
			sc.append(scriptHeader(ctx));
			sc.append("" + "$IPTABLES -F netwall\n"
					+ "$IPTBALES -F netwall-reject\n"
					+ "$IPTABLES -F netwall-3g\n"
					+ "$IPTABLES -F netwall-wifi\n");

			int code = runScriptAsRoot(ctx, sc.toString(), sb);
			if (code == -1) {
				alert(ctx, "Error purging iptables. exit code: " + code + "\n"
						+ sb);
				return false;
			}
			return true;
		} catch (Exception e) {
			alert(ctx, "Error purging iptables: " + e);
			return false;
		}
	}

	public static NetApps[] getApps(Context ctx) {
		if (apps != null)
			return apps;

		final SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		// allowed application names separated by pipe '|' (persisted)
		final String savedUids_wifi = prefs.getString(PREFS_WIFI, "");
		final String savedUids_3g = prefs.getString(PREFS_3G, "");
		int selected_wifi[] = new int[0];
		int selected_3g[] = new int[0];
		if (savedUids_wifi.length() > 0) {
			// Check which applications are allowed
			final StringTokenizer tok = new StringTokenizer(savedUids_wifi, "|");
			selected_wifi = new int[tok.countTokens()];
			for (int i = 0; i < selected_wifi.length; i++) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						selected_wifi[i] = Integer.parseInt(uid);
					} catch (Exception ex) {
						selected_wifi[i] = -1;
					}
				}
			}
			Arrays.sort(selected_wifi);
		}

		if (savedUids_3g.length() > 0) {
			// Check which applications are allowed
			final StringTokenizer tok = new StringTokenizer(savedUids_3g, "|");
			selected_3g = new int[tok.countTokens()];
			for (int i = 0; i < selected_3g.length; i++) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						selected_3g[i] = Integer.parseInt(uid);
					} catch (Exception ex) {
						selected_3g[i] = -1;
					}
				}
			}
			Arrays.sort(selected_3g);
		}

		try {
			final PackageManager pm = ctx.getPackageManager();
			final List<ApplicationInfo> installed = pm
					.getInstalledApplications(0);
			final HashMap<Integer, NetApps> map = new HashMap<Integer, NetApps>();
			final Editor edit = prefs.edit();
			boolean changed = false;
			String name = null;
			String label = null;
			NetApps app = null;

			for (final ApplicationInfo info : installed) {

				if (info.uid < 10000)
					continue;

				app = map.get(info.uid);
				if (app == null
						&& PackageManager.PERMISSION_GRANTED != pm
								.checkPermission(Manifest.permission.INTERNET,
										info.packageName)) {
					continue;
				}

				label = "cache.label." + info.packageName;
				name = prefs.getString(label, "");

				if (name.length() == 0) {
					name = pm.getApplicationLabel(info).toString();
					edit.putString(label, name);
					changed = true;
				}

				if (app == null) {
					app = new NetApps();
					app.icon = info.loadIcon(pm);
					app.uid = info.uid;
					app.names = new String[] { name };
					map.put(info.uid, app);
				} else {
					final String nnames[] = new String[app.names.length + 1];
					System.arraycopy(app.names, 0, nnames, 0, app.names.length);
					nnames[app.names.length] = name;
					app.names = nnames;
				}

				if (!app.selected_wifi
						&& Arrays.binarySearch(selected_wifi, app.uid) >= 0) {
					app.selected_wifi = true;
				}
				if (!app.selected_3g
						&& Arrays.binarySearch(selected_3g, app.uid) >= 0) {
					app.selected_3g = true;
				}
			}

			if (changed) {
				edit.commit();
			}

			apps = map.values().toArray(new NetApps[map.size()]);
			return apps;
		} catch (Exception e) {
			alert(ctx, "error: " + e);
		}
		return null;
	}

	public static boolean hasRootAccess(Context ctx, boolean showErrors) {
		if (hasroot)
			return true;
		final StringBuilder sb = new StringBuilder();
		try {
			// Run an empty script just to check root access
			if (runScriptAsRoot(ctx, "exit 0", sb) == 0) {
				hasroot = true;
				return true;
			}
		} catch (Exception e) {
		}
		if (showErrors) {
			alert(ctx,
					"无法获取root权限，您需要一个已经root的手机来运行。\n\n"
							+ "如果手机已root，请确保防火墙有足够权限运行su命令\n" + "错误信息: "
							+ sb.toString());
		}
		return false;
	}

	public static int runScript(Context ctx, String script, StringBuilder res,
			long timeout) {
		final File file = new File(ctx.getDir("bin", 0), SCRIPT_FILE);
		final ScriptRunner runner = new ScriptRunner(file, script, res);
		runner.start();
		try {
			if (timeout > 0) {
				runner.join(timeout);
			} else {
				runner.join();
			}
			if (runner.isAlive()) {
				// Timed-out
				runner.interrupt();
				runner.join(150);
				runner.destroy();
				runner.join(50);
			}
		} catch (InterruptedException ex) {
		}
		return runner.exitcode;
	}

	public static int runScriptAsRoot(Context ctx, String script,
			StringBuilder res) throws IOException {
		return runScript(ctx, script, res, 4000);
	}

	public static boolean assertBinaries(Context ctx) {
		boolean changed = false;
		try {
			// Check iptables_armv5
			File file = new File(ctx.getDir("bin", 0), "iptables");
			if (!file.exists() || file.length() != 198652) {
				copyRawFile(ctx, R.raw.iptables, file, "755");
				changed = true;
			}
			// Check busybox
			file = new File(ctx.getDir("bin", 0), "busybox");
			if (!file.exists()) {
				copyRawFile(ctx, R.raw.busybox, file, "755");
				changed = true;
			}
			if (changed) {
				// Toast.makeText(ctx, "工具已安装", Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			alert(ctx, "无法安装工具: " + e);
			return false;
		}
		return true;
	}

	public static boolean isEnabled(Context ctx) {
		if (ctx == null) {
			return false;
		}
		if (ctx.getSharedPreferences(PREFS_NAME, 0).getBoolean(PREFS_ENABLED,
				false)) {
			return true;
		} else {
			return false;
		}
	}

	public static void setEnabled(Context ctx, boolean enabled) {
		if (ctx == null) {
			return;
		}
		final SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(PREFS_ENABLED, false) == enabled) {
			return;
		}
		final Editor edit = prefs.edit();
		edit.putBoolean(PREFS_ENABLED, enabled);
		edit.commit();
		if (!edit.commit()) {
			alert(ctx, "无法写入Prefs.xml");
			return;
		}
	}

	public static void applicationRemoved(Context ctx, int uid) {
		final SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		final Editor editor = prefs.edit();
		// allowed application names separated by pipe '|' (persisted)
		final String savedUids_wifi = prefs.getString(PREFS_WIFI, "");
		final String savedUids_3g = prefs.getString(PREFS_3G, "");
		final String uid_str = uid + "";
		boolean changed = false;
		// look for the removed application in the "wi-fi" list
		if (savedUids_wifi.length() > 0) {
			final StringBuilder newuids = new StringBuilder();
			final StringTokenizer tok = new StringTokenizer(savedUids_wifi, "|");
			while (tok.hasMoreTokens()) {
				final String token = tok.nextToken();
				if (uid_str.equals(token)) {
					// Log.d("DroidWall", "Removing UID " + token +
					// " from the wi-fi list (package removed)!");
					changed = true;
				} else {
					if (newuids.length() > 0)
						newuids.append('|');
					newuids.append(token);
				}
			}
			if (changed) {
				editor.putString(PREFS_WIFI, newuids.toString());
			}
		}
		// look for the removed application in the "3g" list
		if (savedUids_3g.length() > 0) {
			final StringBuilder newuids = new StringBuilder();
			final StringTokenizer tok = new StringTokenizer(savedUids_3g, "|");
			while (tok.hasMoreTokens()) {
				final String token = tok.nextToken();
				if (uid_str.equals(token)) {
					Log.d("DroidWall", "Removing UID " + token
							+ " from the 3G list (package removed)!");
					changed = true;
				} else {
					if (newuids.length() > 0)
						newuids.append('|');
					newuids.append(token);
				}
			}
			if (changed) {
				editor.putString(PREFS_3G, newuids.toString());
			}
		}
		// if anything has changed, save the new prefs...
		if (changed) {
			editor.commit();
			if (isEnabled(ctx)) {
				// .. and also re-apply the rules if the firewall is enabled
				applySavedIptablesRules(ctx);
			}
		}
	}

	public static final class NetApps {
		Drawable icon;
		int uid;
		String names[];
		boolean selected_wifi;
		boolean selected_3g;
		String tostr;

		public NetApps() {
		}

		public NetApps(Drawable icon, int uid, String name,
				boolean selected_wifi, boolean selected_3g) {
			this.icon = icon;
			this.uid = uid;
			this.names = new String[] { name };
			this.selected_3g = selected_3g;
			this.selected_wifi = selected_wifi;
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			if (tostr == null) {
				final StringBuilder sb = new StringBuilder();
				for (int i = 0; i < names.length; i++) {
					sb.append(names[i]);
				}
				sb.append("\n");
				tostr = sb.toString();
			}
			return tostr;
		}
	}

	private static final class ScriptRunner extends Thread {
		private final File file;
		private final String script;
		private final StringBuilder res;
		public int exitcode = -1;
		private Process exec;

		public ScriptRunner(File file, String script, StringBuilder res) {
			this.file = file;
			this.script = script;
			this.res = res;
		}

		@Override
		public void run() {
			try {
				file.createNewFile();
				final String abspath = file.getAbsolutePath();
				// make sure we have execution permission on the script file
				Runtime.getRuntime().exec("chmod 777 " + abspath).waitFor();
				// Write the script to be executed
				final OutputStreamWriter out = new OutputStreamWriter(
						new FileOutputStream(file));
				if (new File("/system/bin/sh").exists()) {
					out.write("#!/system/bin/sh\n");
				}
				out.write(script);
				if (!script.endsWith("\n"))
					out.write("\n");
				out.write("exit\n");
				out.flush();
				out.close();
				exec = Runtime.getRuntime().exec("su -c " + abspath);
				final InputStream stdout = exec.getInputStream();
				final InputStream stderr = exec.getErrorStream();
				final byte buf[] = new byte[8192];
				int read = 0;
				while (true) {
					final Process localexec = exec;
					if (localexec == null)
						break;
					try {
						// get the process exit code - will raise
						// IllegalThreadStateException if still running
						this.exitcode = localexec.exitValue();
					} catch (IllegalThreadStateException ex) {
						// The process is still running
					}
					// Read stdout
					if (stdout.available() > 0) {
						read = stdout.read(buf);
						if (res != null)
							res.append(new String(buf, 0, read));
					}
					// Read stderr
					if (stderr.available() > 0) {
						read = stderr.read(buf);
						if (res != null)
							res.append(new String(buf, 0, read));
					}
					if (this.exitcode != -1) {
						// finished
						break;
					}
					// Sleep for the next round
					Thread.sleep(50);
				}
			} catch (InterruptedException ex) {
				if (res != null)
					res.append("\nOperation timed-out");
			} catch (Exception ex) {
				if (res != null)
					res.append("\n" + ex);
			} finally {
				destroy();
			}
		}

		/**
		 * Destroy this script runner
		 */
		public synchronized void destroy() {
			if (exec != null)
				exec.destroy();
			exec = null;
		}
	}
}
