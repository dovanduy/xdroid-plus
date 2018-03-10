package isc.whu.defender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.internal.util.Application;
import com.android.internal.util.Activity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.preference.PreferenceManager;

public class XDroidDefender extends Application implements
		Application.ActivityLifecycleCallbacks {
	public static final String TAG = "XposedInstaller";

	@SuppressLint("SdCardPath")
	public static final String BASE_DIR = "/data/data/isc.whu.defender/";
	// public static final String BASE_DIR =
	// "/data/data/de.robv.android.xposed.installer/";

	private static XDroidDefender mInstance = null;
	private static Thread mUiThread;
	private static Handler mMainHandler;
	private RootUtil mRootUtil = new RootUtil();

	private boolean mIsUiLoaded = false;
	private Activity mCurrentActivity = null;
	private SharedPreferences mPref;

	public void onCreate() {
		super.onCreate();
		mInstance = this;
		mUiThread = Thread.currentThread();//返回当前正在执行的线程对象的引用，以便对其进行操作
		mMainHandler = new Handler();

		mPref = PreferenceManager.getDefaultSharedPreferences(this);
		createDirectories();
		NotificationUtil.init();
		AssetUtil.checkStaticBusyboxAvailability();
		AssetUtil.removeBusybox();

		registerActivityLifecycleCallbacks(this);
	}

	private void createDirectories() {
		mkdirAndChmod("bin", 00771);
		mkdirAndChmod("conf", 00771);
		mkdirAndChmod("log", 00771);
	}

	private void mkdirAndChmod(String dir, int permissions) {
		dir = BASE_DIR + dir;
		new File(dir).mkdir();
		FileUtils.setPermissions(dir, permissions, -1, -1);
	}

	public static XDroidDefender getInstance() {
		return mInstance;
	}

	public static void runOnUiThread(Runnable action) {
		if (Thread.currentThread() != mUiThread) {
			mMainHandler.post(action);
		} else {
			action.run();
		}
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

	public boolean areDownloadsEnabled() {
		if (!mPref.getBoolean("enable_downloads", true))
			return false;

		if (checkCallingOrSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
			return false;

		return true;
	}

	public static SharedPreferences getPreferences() {
		return mInstance.mPref;
	}

	public void updateProgressIndicator() {
		// final boolean isLoading = RepoLoader.getInstance().isLoading()
		// || ModuleUtil.getInstance().isLoading();
		final boolean isLoading = false;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				synchronized (XDroidDefender.this) {
					if (mCurrentActivity != null)
						mCurrentActivity
								.setProgressBarIndeterminateVisibility(isLoading);
				}
			}
		});
	}

	@Override
	public synchronized void onActivityCreated(Activity activity,
			Bundle savedInstanceState) {
		if (mIsUiLoaded)
			return;

		// RepoLoader.getInstance().triggerFirstLoadIfNecessary();
		mIsUiLoaded = true;
	}

	@Override
	public synchronized void onActivityResumed(Activity activity) {
		mCurrentActivity = activity;
		updateProgressIndicator();
	}

	@Override
	public synchronized void onActivityPaused(Activity activity) {
		activity.setProgressBarIndeterminateVisibility(false);
		mCurrentActivity = null;
	}

	@Override
	public void onActivityStarted(Activity activity) {
	}

	@Override
	public void onActivityStopped(Activity activity) {
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
	}

	@Override
	public void onActivityDestroyed(Activity activity) {
	}
}
