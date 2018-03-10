package isc.whu.defender;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import me.piebridge.android.preference.PreferenceFragment;
import isc.whu.defender.common.CommonTools;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class PrefsFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {
	private static final String TAG = "XDroid-app";
	private String behInterceptionKey;
	private String malwareDetectionKey;
	private static final String BASE_DIR = "/data/data/isc.whu.defender/";
	private static final String MODULES_LIST_FILE = XDroidDefender.BASE_DIR
			+ "conf/modules.list";
	public static final String INSTALLER_DIR = "/data/data/de.robv.android.xposed.installer/";
	private static Pattern PATTERN_APP_PROCESS_VERSION = Pattern
			.compile(".*with Xposed support \\(version (.+)\\).*");
	private String APP_PROCESS_NAME = null;
	private String XPOSEDTEST_NAME = null;
	private final String BINARIES_FOLDER = AssetUtil.getBinariesFolder();
	private static final String JAR_PATH = XDroidDefender.BASE_DIR
			+ "bin/XposedBridge.jar";
	private static final String JAR_PATH_NEWVERSION = JAR_PATH + ".newversion";
	private static int JAR_LATEST_VERSION = -1;
	private boolean mHadSegmentationFault = false;

	private static final int INSTALL_MODE_NORMAL = 0;
	private static final int INSTALL_MODE_RECOVERY_AUTO = 1;
	private static final int INSTALL_MODE_RECOVERY_MANUAL = 2;

	private ProgressDialog dlgProgress;
	private RootUtil mRootUtil = new RootUtil();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(
				CommonTools.DEFAULT_PREF_NAME);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preference);
		try {
			behInterceptionKey = getResources().getString(
					R.string.beh_interception_key);
			malwareDetectionKey = getResources().getString(
					R.string.malware_detection_key);
		} catch (Exception e) {
			Log.e(TAG, "[!]Resource NOT found.\n");
			e.printStackTrace();
		}

		dlgProgress = new ProgressDialog(getActivity());
		dlgProgress.setIndeterminate(true);

		InstallPreference mInstallFrame = (InstallPreference) getPreferenceScreen()
				.findPreference("framework");
		AsyncClickListener installListener = new AsyncClickListener(
				getString(R.string.install)) {
			@Override
			public void onAsyncClick(View v) {
				final boolean success = install();
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (success)
							updateModulesList(false);
					}
				});
			}
		};
		mInstallFrame.setInstallListener(installListener);
		AsyncClickListener uninstallListener = new AsyncClickListener(
				getString(R.string.uninstall)) {
			@Override
			public void onAsyncClick(View v) {
				final boolean success = uninstall();
			}
		};
		mInstallFrame.setUnintallListenerr(uninstallListener);
		onSharedPreferenceChanged(null, "");
	}

	private void showAlert(final String result) {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showAlert(result);
				}
			});
			return;
		}

		AlertDialog dialog = new AlertDialog.Builder(getActivity())
				.setMessage(result)
				.setPositiveButton(android.R.string.ok, null).create();
		dialog.show();
		TextView txtMessage = (TextView) dialog
				.findViewById(android.R.id.message);
		txtMessage.setTextSize(14);

		mHadSegmentationFault = result.toLowerCase(Locale.US).contains(
				"segmentation fault");
		// refreshKnownIssue();
	}

	private void showConfirmDialog(final String message,
			final DialogInterface.OnClickListener yesHandler,
			final DialogInterface.OnClickListener noHandler) {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showConfirmDialog(message, yesHandler, noHandler);
				}
			});
			return;
		}

		AlertDialog dialog = new AlertDialog.Builder(getActivity())
				.setMessage(message)
				.setPositiveButton(android.R.string.yes, yesHandler)
				.setNegativeButton(android.R.string.no, noHandler).create();
		dialog.show();
		TextView txtMessage = (TextView) dialog
				.findViewById(android.R.id.message);
		txtMessage.setTextSize(14);

		mHadSegmentationFault = message.toLowerCase(Locale.US).contains(
				"segmentation fault");
	}

	private boolean startShell() {
		if (mRootUtil.startShell())
			return true;

		showAlert(getString(R.string.root_failed));
		return false;
	}

	private void reboot(String mode) {
		if (!startShell())
			return;

		List<String> messages = new LinkedList<String>();

		String command = "reboot";
		if (mode != null) {
			command += " " + mode;
			if (mode.equals("recovery"))
				// create a flag used by some kernels to boot into recovery
				mRootUtil.executeWithBusybox("touch /cache/recovery/boot",
						messages);
		}

		if (mRootUtil.executeWithBusybox(command, messages) != 0) {
			messages.add("");
			messages.add(getString(R.string.reboot_failed));
			showAlert(TextUtils.join("\n", messages).trim());
		}
		AssetUtil.removeBusybox();
	}

	private void offerReboot(List<String> messages) {
		messages.add(getString(R.string.file_done));
		messages.add("");
		messages.add(getString(R.string.reboot_confirmation));
		showConfirmDialog(TextUtils.join("\n", messages).trim(),
				new AsyncDialogClickListener(getString(R.string.reboot)) {
					@Override
					protected void onAsyncClick(DialogInterface dialog,
							int which) {
						reboot(null);
					}
				}, null);
	}

	// install fragment, replace system app_process, while completing reboot
	private boolean install() {
		final int installMode = INSTALL_MODE_NORMAL;
		if (BINARIES_FOLDER == null) {
			// incompatible processor architecture
		}
		if (Build.VERSION.SDK_INT == 10) {
			APP_PROCESS_NAME = BINARIES_FOLDER + "app_process_xposed_sdk10";
		} else if (Build.VERSION.SDK_INT == 15) {
			APP_PROCESS_NAME = BINARIES_FOLDER + "app_process_xposed_sdk15";
			XPOSEDTEST_NAME = BINARIES_FOLDER + "xposedtest_sdk15";
		} else if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT <= 19) {
			APP_PROCESS_NAME = BINARIES_FOLDER + "app_process_xposed_sdk16";
			XPOSEDTEST_NAME = BINARIES_FOLDER + "xposedtest_sdk16";
		} else if (Build.VERSION.SDK_INT > 19) {
			APP_PROCESS_NAME = BINARIES_FOLDER + "app_process_xposed_sdk16";
			XPOSEDTEST_NAME = BINARIES_FOLDER + "xposedtest_sdk16";
		}

		if (!startShell())
			return false;

		List<String> messages = new LinkedList<String>();
		boolean showAlert = true;
		try {
			messages.add(getString(R.string.file_copying,
					"Xposed-Disabler-Recovery.zip"));
			if (AssetUtil.writeAssetToSdcardFile(
					"Xposed-Disabler-Recovery.zip", 00644) == null) {
				messages.add("");
				messages.add(getString(R.string.file_extract_failed,
						"Xposed-Disabler-Recovery.zip"));
				return false;
			}

			messages.add(getString(R.string.file_copying, "TrafficMonitor"));
			if (AssetUtil.writeAssetToFile("sniffer", new File(
					XDroidDefender.BASE_DIR + "bin/sniffer"), 00700) == null) {
				messages.add("");
				messages.add(getString(R.string.file_extract_failed, "TrafficMonitor"));
			}

			File appProcessFile = AssetUtil.writeAssetToFile(APP_PROCESS_NAME,
					new File(XDroidDefender.BASE_DIR + "bin/app_process"),
					00700);
			if (appProcessFile == null) {
				showAlert(getString(R.string.file_extract_failed, "app_process"));
				return false;
			}

			if (installMode == INSTALL_MODE_NORMAL) {
				// Normal installation
				messages.add(getString(R.string.file_mounting_writable,
						"/system"));
				if (mRootUtil.executeWithBusybox("mount -o remount,rw /system",
						messages) != 0) {
					messages.add(getString(R.string.file_mount_writable_failed,
							"/system"));
					messages.add(getString(R.string.file_trying_to_continue));
				}

				if (new File("/system/bin/app_process.orig").exists()) {
					messages.add(getString(R.string.file_backup_already_exists,
							"/system/bin/app_process.orig"));
				} else {
					if (mRootUtil
							.executeWithBusybox(
									"cp -a /system/bin/app_process /system/bin/app_process.orig",
									messages) != 0) {
						messages.add("");
						messages.add(getString(R.string.file_backup_failed,
								"/system/bin/app_process"));
						return false;
					} else {
						messages.add(getString(R.string.file_backup_successful,
								"/system/bin/app_process.orig"));
					}

					mRootUtil.executeWithBusybox("sync", messages);
				}

				messages.add(getString(R.string.file_copying, "app_process"));
				if (mRootUtil.executeWithBusybox(
						"cp -af " + appProcessFile.getAbsolutePath()
								+ " /system/bin/app_process", messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_copy_failed,
							"app_process", "/system/bin"));
					return false;
				}
				if (mRootUtil.executeWithBusybox(
						"chmod 755 /system/bin/app_process", messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_set_perms_failed,
							"/system/bin/app_process"));
					return false;
				}
				if (mRootUtil.executeWithBusybox(
						"chown root:shell /system/bin/app_process", messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_set_owner_failed,
							"/system/bin/app_process"));
					return false;
				}

			}

			File blocker = new File(XDroidDefender.BASE_DIR + "conf/disabled");
			if (blocker.exists()) {
				messages.add(getString(R.string.file_removing,
						blocker.getAbsolutePath()));
				if (mRootUtil.executeWithBusybox(
						"rm " + blocker.getAbsolutePath(), messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_remove_failed,
							blocker.getAbsolutePath()));
					return false;
				}
			}

			messages.add(getString(R.string.file_copying, "XposedBridge.jar"));
			File jarFile = AssetUtil.writeAssetToFile("XposedBridge.jar",
					new File(JAR_PATH_NEWVERSION), 00644);
			if (jarFile == null) {
				messages.add("");
				messages.add(getString(R.string.file_extract_failed,
						"XposedBridge.jar"));
				return false;
			}
			if (!new File(INSTALLER_DIR).exists())
				mRootUtil
						.executeWithBusybox("mkdir " + INSTALLER_DIR, messages);
			if (!new File(INSTALLER_DIR + "bin").exists())
				mRootUtil.executeWithBusybox("mkdir " + INSTALLER_DIR + "bin",
						messages);
			mRootUtil.executeWithBusybox("cp " + JAR_PATH_NEWVERSION + " "
					+ INSTALLER_DIR + "bin/XposedBridge.jar.newversion",
					messages);

			mRootUtil.executeWithBusybox("sync", messages);

			showAlert = false;
			messages.add("");
			if (installMode == INSTALL_MODE_NORMAL)
				offerReboot(messages);

			return true;

		} finally {
			AssetUtil.removeBusybox();

			if (showAlert)
				showAlert(TextUtils.join("\n", messages).trim());
		}
	}

	private boolean uninstall() {
		final int installMode = INSTALL_MODE_NORMAL;

		new File(JAR_PATH_NEWVERSION).delete();
		new File(JAR_PATH).delete();
		new File(XDroidDefender.BASE_DIR + "bin/app_process").delete();

		if (!startShell())
			return false;

		List<String> messages = new LinkedList<String>();
		boolean showAlert = true;
		try {
			if (installMode == INSTALL_MODE_NORMAL) {
				if (new File(INSTALLER_DIR).exists())
					mRootUtil.executeWithBusybox("rm -r " + INSTALLER_DIR,
							messages);
				messages.add(getString(R.string.file_mounting_writable,
						"/system"));
				if (mRootUtil.executeWithBusybox("mount -o remount,rw /system",
						messages) != 0) {
					messages.add(getString(R.string.file_mount_writable_failed,
							"/system"));
					messages.add(getString(R.string.file_trying_to_continue));
				}

				messages.add(getString(R.string.file_backup_restoring,
						"/system/bin/app_process.orig"));
				if (!new File("/system/bin/app_process.orig").exists()) {
					messages.add("");
					messages.add(getString(R.string.file_backup_not_found,
							"/system/bin/app_process.orig"));
					return false;
				}
				
				messages.add(getString(R.string.file_removing,
						XDroidDefender.BASE_DIR + "bin/sniffer"));
				if (mRootUtil.executeWithBusybox("rm "
						+ XDroidDefender.BASE_DIR + "bin/sniffer", messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_remove_failed,
							XDroidDefender.BASE_DIR + "bin/sniffer"));
				}

				if (mRootUtil
						.executeWithBusybox(
								"mv /system/bin/app_process.orig /system/bin/app_process",
								messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_move_failed,
							"/system/bin/app_process.orig",
							"/system/bin/app_process"));
					return false;
				}
				if (mRootUtil.executeWithBusybox(
						"chmod 755 /system/bin/app_process", messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_set_perms_failed,
							"/system/bin/app_process"));
					return false;
				}
				if (mRootUtil.executeWithBusybox(
						"chown root:shell /system/bin/app_process", messages) != 0) {
					messages.add("");
					messages.add(getString(R.string.file_set_owner_failed,
							"/system/bin/app_process"));
					return false;
				}
				// Might help on some SELinux-enforced ROMs, shouldn't hurt on
				// others
				mRootUtil.execute(
						"/system/bin/restorecon /system/bin/app_process", null);

			}

			showAlert = false;
			messages.add("");
			if (installMode == INSTALL_MODE_NORMAL)
				offerReboot(messages);

			return true;

		} finally {
			AssetUtil.removeBusybox();

			if (showAlert)
				showAlert(TextUtils.join("\n", messages).trim());
		}
	}

	// add xmonitor module to installed fragment
	public synchronized void updateModulesList(boolean showToast) {
		try {
			List<String> messages = new LinkedList<String>();
			Log.i(TAG, "updating modules.list");
			PrintWriter modulesList = new PrintWriter(MODULES_LIST_FILE);
			modulesList.println(getActivity().getApplicationInfo().sourceDir);
			modulesList
					.println("/data/app/de.robv.android.xposed.examples.redclock-1.apk");

			modulesList.close();

			FileUtils.setPermissions(MODULES_LIST_FILE, 00664, -1, -1);
			if (!new File(INSTALLER_DIR).exists())
				mRootUtil
						.executeWithBusybox("mkdir " + INSTALLER_DIR, messages);
			if (!new File(INSTALLER_DIR + "conf").exists())
				mRootUtil.executeWithBusybox("mkdir " + INSTALLER_DIR + "conf",
						messages);
			mRootUtil.executeWithBusybox("cp " + MODULES_LIST_FILE + " "
					+ INSTALLER_DIR + "conf/modules.list", messages);
			if (showToast)
				Toast.makeText(getActivity(), R.string.module_list_updated,
						Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Log.e(TAG, "cannot write " + MODULES_LIST_FILE, e);
			Toast.makeText(getActivity(), "cannot write " + MODULES_LIST_FILE,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	public void onPause() {
		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(behInterceptionKey)) {
			if (sharedPreferences.getBoolean(behInterceptionKey, false))
				startInjection();
			else
				stopBehInterception();
		} else if (key.equals(malwareDetectionKey)) {
			if (sharedPreferences.getBoolean(malwareDetectionKey, false))
				startMalwareDetection();
			else
				stopMalwareDetection();
		}

	}

	private void stopBehInterception() {
		Log.i(TAG, CommonTools.getLogTime() + "stopBehInterception()...\n");
	}

	private void startInjection() {
		Log.i(TAG, CommonTools.getLogTime() + "startInjection() ....\n");
	}

	private void startMalwareDetection() {
		Log.i(TAG, CommonTools.getLogTime() + "startMalwareDetection()...\n");
		// new Thread() {
		// public void run() {
		// // CommonTools.runRootCommand("/data/sniffer");
		// CommonTools.runRootCommand("/data/auto.sh");
		// }
		// }.start();
	}

	private void stopMalwareDetection() {
		Log.i(TAG, CommonTools.getLogTime() + "stopMalwareDetection()...\n");
	}

	private abstract class AsyncClickListener implements View.OnClickListener {
		private final CharSequence mProgressDlgText;

		public AsyncClickListener(CharSequence progressDlgText) {
			mProgressDlgText = progressDlgText;
		}

		@Override
		public final void onClick(final View v) {
			if (mProgressDlgText != null) {
				dlgProgress.setMessage(mProgressDlgText);
				dlgProgress.show();
			}
			new Thread() {
				public void run() {
					onAsyncClick(v);
					dlgProgress.dismiss();
				}
			}.start();
		}

		protected abstract void onAsyncClick(View v);
	}

	private abstract class AsyncDialogClickListener implements
			DialogInterface.OnClickListener {
		private final CharSequence mProgressDlgText;

		public AsyncDialogClickListener(CharSequence progressDlgText) {
			mProgressDlgText = progressDlgText;
		}

		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			if (mProgressDlgText != null) {
				dlgProgress.setMessage(mProgressDlgText);
				dlgProgress.show();
			}
			new Thread() {
				public void run() {
					onAsyncClick(dialog, which);
					dlgProgress.dismiss();
				}
			}.start();
		}

		protected abstract void onAsyncClick(DialogInterface dialog, int which);
	}
}
