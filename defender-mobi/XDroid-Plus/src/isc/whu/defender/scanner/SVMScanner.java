package isc.whu.defender.scanner;

import isc.whu.defender.ActivityUsage;
import isc.whu.defender.MainActivity;
import isc.whu.defender.R;
import isc.whu.defender.xmonitor.HookManager;
import isc.whu.defender.xmonitor.PRestriction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.util.EncodingUtils;

import com.android.libsvm.svm;
import com.android.libsvm.svm_model;
import com.android.libsvm.svm_parameter;
import com.android.libsvm.svm_predict;
import com.android.libsvm.svm_scale;
import com.android.libsvm.svm_train;

import dalvik.system.DexClassLoader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SVMScanner extends Service {
	private static final String TAG = "SVMScanner";
	private Context mContext = null;
	private Thread mThread = null;
	public static String cUidApi = "uid-api";
	public static String cModel = "svm_model";
	public static String cTestSet = "test_set";
	public static String cPredictResult = "svm_result";

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
	}

	@Override
	public void onDestroy() {

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "SVMService->onStartCommand()");
		if (mThread == null) {
			mThread = new Thread(runnable);
			mThread.start();
		}

		return START_REDELIVER_INTENT;// 服务被异常kill掉，系统会自动重启该服务，并将Intent的值传入
	}

	protected void writetoFile(String text, String filename) {
		try {
			File targetFile = new File(getApplicationContext().getFilesDir()
					.getAbsolutePath() + File.separator + filename);
			FileOutputStream fout = new FileOutputStream(targetFile);
			byte[] bytes = text.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	Runnable runnable = new Runnable() {
		// 重写run()方法，此方法在新的线程中运行
		@Override
		public void run() {
			Log.i(TAG, "Get third party app's uid");
			// Get third party app's uid
			PackageManager pm = mContext.getPackageManager();
			List<ApplicationInfo> listAppInfo = pm
					.getInstalledApplications(PackageManager.GET_META_DATA);
			List<Integer> listUid = new ArrayList<Integer>();
			List<String> listAppName = new ArrayList<String>();
			for (ApplicationInfo appInfo : listAppInfo) {
				if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
					String appName = pm.getApplicationLabel(appInfo).toString();
					if (appName.contains("XDroid Defender")
							|| appName
									.contains("Xposed Installer Static BusyBox"))
						break;
					listUid.add(appInfo.uid);
					listAppName.add(appName);
				}
			}
			String[] API = new String[] { "getDeviceId", "getSubscriberId",
					"getCurrentPhoneType", "getPhoneType", "getLine1Number",
					"sendTextMessage", "abortBroadcast", "getSimSerialNumber",
					"getRunningServices", "load", "getSimOperator",
					"getInstalledPackages", "getNetworkOperatorName",
					"loadUrl", "getCellLocation", "isConnected",
					"getRunningTasks", "getSimOperatorName", "getPhoneType",
					"getSimCountryIso", "sendMultipartTextMessage",
					"getNetworkType", "getInstalledApplications",
					"loadLibrary", "getNetworkOperator", "addView",
					"requestLocationUpdates", "exec", "getNetworkCountryIso",
					"getAccountsByType", "getLatitude", "getLongitude",
					"startRecording", "takePicture", "BrowserProvider2",
					"listen", "android.intent.action.CALL",
					"android.intent.action.PHONE_STATE", "CallLogProvider",
					"ContactsProvider2" };
			List<String> listAPI = Arrays.asList(API);
			writetoFile(listUid.toString() + "\n" + listAppName.toString()
					+ "\n" + listAPI.toString(), cUidApi);

			// Fetch xmonitor usage log for third party apps
			String libsvm = formatLibsvm(listUid, listAPI);

			// Save Libsvm formatted lines to file
			Log.i(TAG, "Save Libsvm formatted lines to file");
			writetoFile(libsvm, cTestSet);
			String resultFileName = run_svm(cModel, cTestSet);

			// String resultFileName = getApplicationContext().getFilesDir()
			// .getAbsolutePath() + "/" + "svm_result";
			// makeDecision(resultFileName, listUid, listAppName);
		}
	};

	private String formatLibsvm(List<Integer> listUid, List<String> listAPI) {
		String libsvm = "";
		Log.i(TAG, "Fetch xmonitor usage log for third party apps");
		for (Integer uid : listUid) {
			Map<String, Integer> apiCount = new HashMap<String, Integer>();
			String pliststr = HookManager.getUsageList(mContext, uid, null)
					.toString();
			writetoFile(pliststr, uid.toString());
			for (PRestriction usageData : HookManager.getUsageList(mContext,
					uid, null)) {
				String key = usageData.methodName;
				Integer val;
				if (listAPI.contains(key) & !apiCount.containsKey(key))
					apiCount.put(key, 1);
				else if (apiCount.containsKey(key)) {
					val = apiCount.get(key) + 1;
					apiCount.remove(key);
					apiCount.put(key, val);
				}
			}// end for uid UsageList
			Iterator iter = apiCount.entrySet().iterator();
			libsvm += "\n+1";
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				libsvm += " " + (listAPI.indexOf(entry.getKey()) + 1) + ":"
						+ entry.getValue();
			}
		}
		if (libsvm.length() > 1)
			libsvm = libsvm.substring(1);
		return libsvm;
	}

	private String run_svm(String modelFile, String testFile) {
		// String training_set_file = getApplicationContext().getFilesDir()
		// .getAbsolutePath() + "/" + train;
		String testing_set_file = getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/" + testFile;
		String model_file = getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/" + modelFile;
		String result_file = getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/" + cPredictResult;

		// String[] scale_train_set_argv = new String("-l 0 -u 1 "
		// + training_set_file).split(" ");
		// String[] scale_test_set_argv = new String("-l 0 -u 1 "
		// + testing_set_file).split(" ");
		// String[] train_argv = new String("-s " + svm_parameter.C_SVC + " -t "
		// + svm_parameter.RBF + " -c 0.5 -g 0.25 " + training_set_file
		// + " " + model_file).split(" ");
		String[] predict_argv = { testing_set_file, model_file, result_file };

		try {
			// Log.i(TAG, ">>>>>>>> Scaling ...");
			// svm_scale.svm_scale_main(scale_train_set_argv);
			// svm_scale.svm_scale_main(scale_test_set_argv);

			// Log.i(TAG, ">>>>>>>> Training ...");
			// svm_train.svm_train_main(train_argv);

			Log.i(TAG, ">>>>>>>> Predicting ...");
			svm_predict.svm_predict_main(predict_argv);
			Log.i(TAG, ">>>>>>>> Predicting  done...");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result_file;
	}

	private void makeDecision(String resultFile, List<Integer> listUid,
			List<String> listAppName) {
		try {
			FileInputStream fin = new FileInputStream(resultFile);
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			int k = 0;
			String result = EncodingUtils.getString(buffer, "UTF-8");
			Log.i(TAG, result);
			for (String item : result.split("\n")) {
				Log.i(TAG, "item[" + k + "]" + item);
				if (Double.valueOf(item) < 0) {
					showInstallNotification(listUid.get(k), listAppName.get(k));
				}
				k++;
			}
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showInstallNotification(int uid, String packageName) {
		NotificationManager mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// 点击通知栏图标会进入某个界面
		// 构建一个Intent
		Intent intent = new Intent(mContext, ActivityUsage.class);
		intent.putExtra(ActivityUsage.cUid, uid);
		// 封装一个Intent
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
			// 定义通知栏展现的内容信息
			Bitmap btm = BitmapFactory.decodeResource(getResources(),
					R.drawable.unsafe_warning);
			String format = mContext.getString(R.string.text_app_unsafe_format);
			CharSequence title = String.format(format, packageName);
			CharSequence text = "点击查看详细内容";
			Notification.Builder builder = new Notification.Builder(mContext);
			builder.setLargeIcon(btm) // 设置状态栏里面的图标（小图标）　　
					.setTicker(title)// 设置状态栏的显示的信息
					.setWhen(System.currentTimeMillis())// 设置时间发生时间
					.setAutoCancel(false) // 设置可以清除
					.setContentTitle(title)// 设置下拉列表里的标题
					.setContentText(text);// 设置上下文内容
			// 设置通知主题的意图
			builder.setContentIntent(contentIntent);

			// 生成标题栏消息通知
			mNotificationManager.notify(1, builder.build());
		} else {
			int icon = R.drawable.unsafe_warning; // 有无风险图标不同
			String format = mContext.getString(R.string.text_app_unsafe_format);
			CharSequence tickerText = String.format(format, packageName);
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);

			// 定义下拉通知栏时要展现的内容信息
			CharSequence title = tickerText;
			CharSequence text = "点击查看详细内容";
			notification.setLatestEventInfo(mContext, title, text,
					contentIntent);

			// 生成标题栏消息通知
			mNotificationManager.notify(1, notification);
		}
	}
}
