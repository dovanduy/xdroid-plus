package isc.whu.defender.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.http.util.EncodingUtils;

import com.android.internal.util.AsyncTask;
import com.android.libsvm.svm_predict;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import isc.whu.defender.ActivityUsage;
import isc.whu.defender.R;
import isc.whu.defender.softmgr.Activity;
import isc.whu.defender.xmonitor.ApplicationInfoEx;
import isc.whu.defender.xmonitor.HookManager;
import isc.whu.defender.xmonitor.PRestriction;

public class DetectionActivity extends Activity {

	public static final String TAG = "DetectionActivity";
	public static String cUidApi = "uid-api";
	public static String cModel = "svm_model";
	public static String cTestSet = "test_set";
	public static String cPredictResult = "svm_result";

	private DetectionAdapter mDetectionAdapter = null;
	private ProgressDialog mProcessDialog = null;
	private Context mContext = null;

	private static ExecutorService mExecutor = Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors(),
			new PriorityThreadFactory());

	private static class PriorityThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set layout
		setContentView(R.layout.detectionlist);
		mContext = this;

		// Set title
		setTitle(String.format("%s - %s", getString(R.string.app_name),
				getString(R.string.title_malware_detection)));

		// Start task to get app list
		DetectionTask mDetectionTask = new DetectionTask();
		mDetectionTask.executeOnExecutor(mExecutor, (Object) null);

		// Up navigation
		// getActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	// Action Bar return
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Intent upIntent = NavUtils.getParentActivityIntent(this);
			Intent upIntent = getSupportParentActivityIntent();
			if (upIntent != null)
				if (NavUtils.shouldUpRecreateTask(this, upIntent))
					TaskStackBuilder.create(this)
							.addNextIntentWithParentStack(upIntent)
							.startActivities();
				else
					NavUtils.navigateUpTo(this, upIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Intent getSupportParentActivityIntent() {
		Intent parentIntent = getIntent();
		String className = parentIntent.getStringExtra("ParentClassName");
		Intent newIntent = null;
		try {
			newIntent = new Intent(DetectionActivity.this,
					Class.forName(className));

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return newIntent;
	}

	// Tasks
	private class DetectionTask extends
			AsyncTask<Object, Integer, List<PDetection>> {

		@Override
		protected void onPreExecute() {
			mProcessDialog = new ProgressDialog(mContext);
			mProcessDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProcessDialog.setMessage(getString(R.string.msg_loading));
			mProcessDialog.setIndeterminate(true);
			mProcessDialog.setCancelable(false);
			mProcessDialog.show();
		}

		@Override
		protected List<PDetection> doInBackground(Object... params) {
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
			String resultFileName = run_svm_predict(cModel, cTestSet);

			// Read from predict result file
			List<PDetection> list = new ArrayList<PDetection>();
			String Uid = readfromFile(cUidApi);
			String Predict = readfromFile(cPredictResult);
			String Testset = readfromFile(cTestSet);
			String uidPart = Uid.split("\n")[0];
			String uidReplace = uidPart.replace("[", "").replace("]", "")
					.trim();
			String[] uidList = uidReplace.split(",");
			String[] predList = Predict.split("\n");
			String[] testList = Testset.split("\n");
			if (uidList.length != predList.length)
				return null;

			// Analyse predict result file to generate View
			for (int i = 0; i < uidList.length; i++) {
				int uid = Integer.parseInt(uidList[i].trim());
				boolean bNormal = true;
				if (testList[i].contains(":"))
					bNormal = predList[i].trim().equals("1.0") ? true : false;
				ApplicationInfoEx appInfo = new ApplicationInfoEx(mContext, uid);
				Drawable icon = appInfo.getIcon(mContext);
				String appName = appInfo.getApplicationName().toString();
				list.add(new PDetection(uid, icon, appName, bNormal));
			}
			return list;
		}

		@Override
		protected void onPostExecute(List<PDetection> trafficList) {
			ListView lvDetection = (ListView) findViewById(R.id.lvDetection);
			mDetectionAdapter = new DetectionAdapter(DetectionActivity.this,
					trafficList);
			lvDetection.setAdapter(mDetectionAdapter);
			lvDetection.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					// TODO Auto-generated method stub
					ListView lv = (ListView) arg0;
					DetectionAdapter detectionAdapter = (DetectionAdapter) lv
							.getAdapter();
					PDetection pDetection = detectionAdapter.mListItems
							.get(arg2);
					Intent intent = new Intent(mContext, ActivityUsage.class);
					intent.putExtra("ParentClassName", mContext.getClass()
							.getName());
					intent.putExtra(ActivityUsage.cUid, pDetection.uid);
					mContext.startActivity(intent);
				}
			});
			// setListViewHeightBasedOnChildren(lvDetection);
			// Dismiss progress dialog
			if (mProcessDialog.isShowing())
				try {
					mProcessDialog.dismiss();
				} catch (IllegalArgumentException ignored) {
				}
		}
	}

	// Solve display bug with ScrollView & ListView 没用
	public static void setListViewHeightBasedOnChildren(ListView listView) {
		DetectionAdapter listAdapter = (DetectionAdapter) listView.getAdapter();
		if (listAdapter == null) {
			// pre-condition
			return;
		}

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight
				+ (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
	}

	public String readfromFile(String fileName) {
		String result = "";
		try {
			File targetFile = new File(getApplicationContext().getFilesDir()
					.getAbsolutePath() + File.separator + fileName);
			FileInputStream fin = new FileInputStream(targetFile);
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			result = EncodingUtils.getString(buffer, "UTF-8");
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
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

	private void writeRawToFile(String targetFile, int mode) {
		try {
			InputStream in = this.getResources().openRawResource(
					R.raw.svm_model);
			FileOutputStream out = new FileOutputStream(new File(targetFile));

			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private String formatLibsvm(List<Integer> listUid, final List<String> listAPI) {
		String libsvm = "";
		Log.i(TAG, "Fetch xmonitor usage log for third party apps");
		for (Integer uid : listUid) {
			// Map<String, Integer> apiCount = new HashMap<String, Integer>();
			Map<String, Integer> apiCount = new TreeMap<String, Integer>(
	                new Comparator<String>() {
	                    public int compare(String obj1, String obj2) {
	                        return listAPI.indexOf(obj1)-listAPI.indexOf(obj2);
	                    }
	                });
			
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

	private String run_svm_predict(String modelFile, String testFile) {

		String testing_set_file = getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/" + testFile;
		String model_file = getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/" + modelFile;
		String result_file = getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/" + cPredictResult;

		String[] predict_argv = { testing_set_file, model_file, result_file };

		try {
			Log.i(TAG, ">>>>>>>> Loading model from raw folder ...");
			writeRawToFile(model_file, 744);
			Log.i(TAG, ">>>>>>>> Predicting ...");
			svm_predict.svm_predict_main(predict_argv);
			Log.i(TAG, ">>>>>>>> Predicting  done...");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result_file;
	}

	// Adapter
	public class DetectionAdapter extends BaseAdapter {
		private Context mContext;
		private List<PDetection> mListItems;
		private LayoutInflater mInflater;

		public DetectionAdapter(Context context, List<PDetection> list) {
			mContext = context;
			mListItems = list;
			mInflater = LayoutInflater.from(mContext);
		}

		@Override
		public int getCount() {
			return mListItems.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			if (convertView == null)
				convertView = mInflater.inflate(R.layout.detectionentry, null);
			ImageView imgIcon = (ImageView) convertView
					.findViewById(R.id.imgIcon);
			TextView tvApp = (TextView) convertView.findViewById(R.id.tvApp);
			TextView tvDetection = (TextView) convertView
					.findViewById(R.id.tvDetection);

			PDetection pDetection = mListItems.get(position);

			tvApp.setText(pDetection.uid + " " + pDetection.appName);
			imgIcon.setImageDrawable(pDetection.icon);
			tvDetection.setText(pDetection.normal ? R.string.detection_normal
					: R.string.detection_malware);
			if (pDetection.normal == false)
				convertView.setBackgroundColor(getResources().getColor(
						R.color.color_dangerous_dark));
			else
				convertView.setBackgroundColor(getResources().getColor(
						R.color.transparent));

			return convertView;
		}
	}
}
