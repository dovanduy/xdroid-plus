package isc.whu.defender.trafmon;

import isc.whu.defender.R;
import isc.whu.defender.softmgr.Activity;
import isc.whu.defender.xmonitor.ApplicationInfoEx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.AsyncTask;

public class TrafficActivity extends Activity {

	private TrafficAdapter mTrafficAdapter = null;
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
		setContentView(R.layout.trafficlist);
		mContext = this;

		// Set title
		setTitle(String.format("%s - %s", getString(R.string.app_name),
				getString(R.string.title_traffic_monitor)));

		// Start task to get app list
		TrafficTask mTrafficTask = new TrafficTask();
		mTrafficTask.executeOnExecutor(mExecutor, (Object) null);

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
			newIntent = new Intent(TrafficActivity.this,
					Class.forName(className));

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return newIntent;
	}

	// Tasks
	private class TrafficTask extends
			AsyncTask<Object, Integer, List<PTraffic>> {

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
		protected List<PTraffic> doInBackground(Object... params) {
			List<PTraffic> list = new ArrayList<PTraffic>();
			// test- furture modify
			// Get third party app's uid
			PackageManager pm = mContext.getPackageManager();
			List<ApplicationInfo> listAppInfo = pm
					.getInstalledApplications(PackageManager.GET_META_DATA);
			List<Integer> listUid = new ArrayList<Integer>();
			for (ApplicationInfo appInfo : listAppInfo) {
				if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)//非系统程序
					listUid.add(appInfo.uid);
			}
			
			TrafficClient client = new TrafficClient();
			SparseArray<PTraffic> appsTraffic = client.getAppsTraffic();
			
			for (int uid : listUid) {
				ApplicationInfoEx appInfo = new ApplicationInfoEx(mContext, uid);
				Drawable icon = appInfo.getIcon(mContext);
				String name = appInfo.getApplicationName().toString();
				
				if (appInfo.getPackageName().toString().equalsIgnoreCase("[isc.whu.defender]")) {
					continue;
				}
				
				if (appsTraffic.get(uid) != null) {
					PTraffic appTraffic = appsTraffic.get(uid);
					appTraffic.icon = icon;
					appTraffic.appName = name;
					list.add(appTraffic);
				} else {
					list.add(new PTraffic(uid, icon, name, 0.0, 0.0));
				}
			}
			/*
			for (int uid : listUid) {
				ApplicationInfoEx appInfo = new ApplicationInfoEx(mContext, uid);
				Drawable icon = appInfo.getIcon(mContext);
				String name = appInfo.getApplicationName().toString();
				if (name.contains("微博"))
					list.add(new PTraffic(uid, icon, appInfo
							.getApplicationName().toString(), 0.21, 0.16));
				if (name.contains("Kid"))
					list.add(new PTraffic(uid, icon, appInfo
							.getApplicationName().toString(), 0.73, 0.81));
			}
			*/
			return list;
		}

		@Override
		protected void onPostExecute(List<PTraffic> trafficList) {
			ListView lvTraffic = (ListView) findViewById(R.id.lvTraffic);
			mTrafficAdapter = new TrafficAdapter(TrafficActivity.this,
					trafficList);
			lvTraffic.setAdapter(mTrafficAdapter);
			lvTraffic.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					// TODO Auto-generated method stub
					ListView lv = (ListView) arg0;
					TrafficAdapter trafficAdapter = (TrafficAdapter) lv
							.getAdapter();
					PTraffic pTraffic = trafficAdapter.mListItems.get(arg2);
					String text = "UID："
							+ pTraffic.uid
							+ "\n应用程序名："
							+ pTraffic.appName.replace('[', ' ').replace(']',
									' ') + "\n上传流量/总流量=" + pTraffic.upTraffic
							+ "\n上传型连接/总连接=" + pTraffic.upLink;
					Toast.makeText(TrafficActivity.this, text,
							Toast.LENGTH_LONG).show();
				}
			});
			// setListViewHeightBasedOnChildren(lvTraffic);
			// Dismiss progress dialog
			if (mProcessDialog.isShowing())
				try {
					mProcessDialog.dismiss();
				} catch (IllegalArgumentException ignored) {
				}
		}
	}

	// Solve display bug with ScrollView & ListView
	public static void setListViewHeightBasedOnChildren(ListView listView) {
		TrafficAdapter listAdapter = (TrafficAdapter) listView.getAdapter();
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

	// Adapter
	public class TrafficAdapter extends BaseAdapter {
		private Context mContext;
		private List<PTraffic> mListItems;
		private LayoutInflater mInflater;

		public TrafficAdapter(Context context, List<PTraffic> list) {
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
				convertView = mInflater.inflate(R.layout.trafficentry, null);
			ImageView imgIcon = (ImageView) convertView
					.findViewById(R.id.imgIcon);
			TextView tvApp = (TextView) convertView.findViewById(R.id.tvApp);
			TextView tvUpTraffic = (TextView) convertView
					.findViewById(R.id.tvUpTraffic);
			TextView tvUpLink = (TextView) convertView
					.findViewById(R.id.tvUpLink);

			PTraffic pTraffic = mListItems.get(position);
			tvApp.setText(pTraffic.uid + " " + pTraffic.appName);
			imgIcon.setImageDrawable(pTraffic.icon);
			tvUpTraffic.setText(pTraffic.upTraffic + "");
			tvUpLink.setText(pTraffic.upLink + "");

			return convertView;
		}
	}
}
