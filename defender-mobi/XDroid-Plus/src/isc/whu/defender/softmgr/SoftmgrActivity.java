package isc.whu.defender.softmgr;

import isc.whu.defender.MainActivity;
import isc.whu.defender.MonitorService;
import isc.whu.defender.R;
import isc.whu.defender.SettingsActivity;

import com.android.internal.util.AsyncTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import isc.whu.defender.trafmon.TrafficActivity;
import isc.whu.defender.xmonitor.Hook;
import isc.whu.defender.xmonitor.HookManager;
import isc.whu.defender.xmonitor.PRestriction;
import isc.whu.defender.xmonitor.ApplicationInfoEx;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class SoftmgrActivity extends Activity {
	/** Called when the activity is first created. */
	private Spinner spRestriction = null;
	private AppListAdapter mAppAdapter = null;
	private int mSortMode;
	private boolean mSortInvert;
	private int mProgressWidth = 0;
	private int mProgress = 0;

	private TextView mTv;
	private ListView lv;
	private Button mBtn;

	public static final int STATE_ATTENTION = 0;
	public static final int STATE_CHANGED = 1;
	public static final int STATE_SHARED = 2;

	private static final int SORT_BY_NAME = 0;
	private static final int SORT_BY_UID = 1;
	private static final int SORT_BY_INSTALL_TIME = 2;
	private static final int SORT_BY_UPDATE_TIME = 3;
	private static final int SORT_BY_MODIFY_TIME = 4;

	List<Integer> uni_listitemID = new ArrayList<Integer>();
	private String TAG = " （Softmgr） ";

	private boolean mPackageChangeReceiverRegistered = false;
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

	private Comparator<ApplicationInfoEx> mSorter = new Comparator<ApplicationInfoEx>() {
		@Override
		public int compare(ApplicationInfoEx appInfo0,
				ApplicationInfoEx appInfo1) {
			int sortOrder = mSortInvert ? -1 : 1;
			switch (mSortMode) {
			case SORT_BY_NAME:
				return sortOrder * appInfo0.compareTo(appInfo1);
			case SORT_BY_UID:
				// default lowest first
				return sortOrder * (appInfo0.getUid() - appInfo1.getUid());
			case SORT_BY_INSTALL_TIME:
				// default newest first
				Long iTime0 = appInfo0.getInstallTime(SoftmgrActivity.this);
				Long iTime1 = appInfo1.getInstallTime(SoftmgrActivity.this);
				return sortOrder * iTime1.compareTo(iTime0);
			case SORT_BY_UPDATE_TIME:
				// default newest first
				Long uTime0 = appInfo0.getUpdateTime(SoftmgrActivity.this);
				Long uTime1 = appInfo1.getUpdateTime(SoftmgrActivity.this);
				return sortOrder * uTime1.compareTo(uTime0);
			case SORT_BY_MODIFY_TIME:
				// default newest first
				Long mTime0 = appInfo0
						.getModificationTime(SoftmgrActivity.this);
				Long mTime1 = appInfo1
						.getModificationTime(SoftmgrActivity.this);
				return sortOrder * mTime1.compareTo(mTime0);
			}
			return 0;
		}
	};

	private BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			SoftmgrActivity.this.recreate();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Check privacy service client
		if (!MonitorService.checkClient()) {
			Log.e(TAG, "MoniterService not registered. Force exit! ");
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
					SoftmgrActivity.this);
			alertDialogBuilder.setTitle(R.string.menu_not_installed);
			alertDialogBuilder.setMessage(R.string.msg_sure);
			alertDialogBuilder.setIcon(R.drawable.warning);
			alertDialogBuilder.setPositiveButton(
					getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(SoftmgrActivity.this,
									SettingsActivity.class);
							startActivity(intent);
						}
					});
			alertDialogBuilder.setNegativeButton(
					getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			return;
		}

		// Set layout
		setContentView(R.layout.sm_main);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// Set title
		setTitle(String.format("%s - %s", getString(R.string.app_name),
				getString(R.string.menu_manage)));

		// 绑定Layout里面的ListView
		lv = (ListView) findViewById(R.id.sm_lv_applist);
		mTv = (TextView) findViewById(R.id.sm_tv_install_AppCount);

		// Start task to get app list
		AppListTask appListTask = new AppListTask();
		appListTask.executeOnExecutor(mExecutor, (Object) null);

		// Listen for package add/remove
		IntentFilter iff = new IntentFilter();
		iff.addAction(Intent.ACTION_PACKAGE_ADDED);
		iff.addAction(Intent.ACTION_PACKAGE_REMOVED);
		iff.addDataScheme("package");
		registerReceiver(mPackageChangeReceiver, iff);
		mPackageChangeReceiverRegistered = true;

		// Default Display third apps
		HookManager.setSetting(0, HookManager.cSettingFUser,
				Boolean.toString(true));
		HookManager.setSetting(0, HookManager.cSettingFSystem,
				Boolean.toString(false));

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mAppAdapter != null)
			mAppAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (mAppAdapter != null)
			mAppAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mPackageChangeReceiverRegistered) {
			unregisterReceiver(mPackageChangeReceiver);
			mPackageChangeReceiverRegistered = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if (inflater != null && MonitorService.checkClient()) {
			inflater.inflate(R.menu.main, menu);
			return true;
		} else
			return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			int itemId = item.getItemId();
			if (itemId == android.R.id.home) {
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
			} else if (itemId == R.id.menu_clear_db) {
				optionClearDB();
				return true;
			} else {
				return super.onOptionsItemSelected(item);
			}
		} catch (Throwable ex) {
			Log.e(null, ex.toString());
			return true;
		}
	}

	@Override
	public Intent getSupportParentActivityIntent() {
		Intent parentIntent = getIntent();
		String className = parentIntent.getStringExtra("ParentClassName");
		Intent newIntent = null;
		try {
			newIntent = new Intent(SoftmgrActivity.this,
					Class.forName(className));

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return newIntent;
	}

	// Options
	private void optionClearDB() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				SoftmgrActivity.this);
		alertDialogBuilder.setTitle(R.string.menu_clear_db);
		alertDialogBuilder.setMessage(R.string.msg_sure);
		alertDialogBuilder.setIcon(R.drawable.warning);
		alertDialogBuilder.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new AsyncTask<Object, Object, Object>() {
							@Override
							protected Object doInBackground(Object... arg0) {
								HookManager.clear();
								return null;
							}

							@Override
							protected void onPostExecute(Object result) {
								// spRestriction.setSelection(0);
								SoftmgrActivity.this.recreate();
							}
						}.executeOnExecutor(mExecutor);
					}
				});
		alertDialogBuilder.setNegativeButton(
				getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	// Tasks
	private class AppListTask extends
			AsyncTask<Object, Integer, List<ApplicationInfoEx>> {
		private String mRestrictionName;
		private ProgressDialog mProgressDialog;

		@Override
		protected List<ApplicationInfoEx> doInBackground(Object... params) {
			mRestrictionName = null;

			// Delegate
			return ApplicationInfoEx.getXApplicationList(SoftmgrActivity.this,
					mProgressDialog);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// Show progress dialog
			mProgressDialog = new ProgressDialog(SoftmgrActivity.this);
			mProgressDialog.setMessage(getString(R.string.msg_loading));
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setProgressNumberFormat(null);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(List<ApplicationInfoEx> listApp) {
			if (!SoftmgrActivity.this.isFinishing()) {
				// Set app total sum
				// mTv.setText("共计安装了" + listApp.size() + "款软件");
				// Display app list
				mAppAdapter = new AppListAdapter(SoftmgrActivity.this,
						R.layout.sm_vlist, listApp, mRestrictionName);
				ListView lvApp = (ListView) findViewById(R.id.sm_lv_applist);
				lvApp.setAdapter(mAppAdapter);

				// Dismiss progress dialog
				if (mProgressDialog.isShowing())
					try {
						mProgressDialog.dismiss();
					} catch (IllegalArgumentException ignored) {
					}

				// Restore state
				SoftmgrActivity.this.selectRestriction(0);
			}

			super.onPostExecute(listApp);
		}
	}

	private void selectRestriction(int pos) {
		if (mAppAdapter != null) {
			String restrictionName = (pos == 0 ? null : (String) HookManager
					.getRestrictions(this).values().toArray()[pos - 1]);
			mAppAdapter.setRestrictionName(restrictionName);
			HookManager.setSetting(0, HookManager.cSettingSelectedCategory,
					restrictionName);
			applyFilter();
		}
	}

	private void applyFilter() {
		if (mAppAdapter != null) {
			// EditText etFilter = (EditText) findViewById(R.id.etFilter);
			ProgressBar pbFilter = (ProgressBar) findViewById(R.id.pbFilter);
			TextView tvStats = (TextView) findViewById(R.id.tvStats);
			TextView tvState = (TextView) findViewById(R.id.tvState);

			// Get settings
			boolean fUsed = HookManager.getSettingBool(0,
					HookManager.cSettingFUsed, false, false);
			boolean fInternet = HookManager.getSettingBool(0,
					HookManager.cSettingFInternet, false, false);
			boolean fRestriction = HookManager.getSettingBool(0,
					HookManager.cSettingFRestriction, false, false);
			boolean fRestrictionNot = HookManager.getSettingBool(0,
					HookManager.cSettingFRestrictionNot, false, false);
			boolean fPermission = HookManager.getSettingBool(0,
					HookManager.cSettingFPermission, true, false);
			boolean fOnDemand = HookManager.getSettingBool(0,
					HookManager.cSettingFOnDemand, false, false);
			boolean fOnDemandNot = HookManager.getSettingBool(0,
					HookManager.cSettingFOnDemandNot, false, false);
			boolean fUser = HookManager.getSettingBool(0,
					HookManager.cSettingFUser, true, false);
			boolean fSystem = HookManager.getSettingBool(0,
					HookManager.cSettingFSystem, false, false);

			String filter = String.format(
					"%s\n%b\n%b\n%b\n%b\n%b\n%b\n%b\n%b\n%b", "", fUsed,
					fInternet, fRestriction, fRestrictionNot, fPermission,
					fOnDemand, fOnDemandNot, fUser, fSystem);
			pbFilter.setVisibility(ProgressBar.VISIBLE);
			tvStats.setVisibility(TextView.GONE);

			// Adjust progress state width
			RelativeLayout.LayoutParams tvStateLayout = (RelativeLayout.LayoutParams) tvState
					.getLayoutParams();
			tvStateLayout.addRule(RelativeLayout.LEFT_OF, R.id.pbFilter);

			mAppAdapter.getFilter().filter(filter);

			invalidateOptionsMenu();
		}
	}

	@SuppressLint("DefaultLocale")
	private class AppListAdapter extends ArrayAdapter<ApplicationInfoEx> {
		private Context mContext;
		private boolean mSelecting = false;
		private List<ApplicationInfoEx> mListAppAll;
		private List<ApplicationInfoEx> mListAppSelected = new ArrayList<ApplicationInfoEx>();
		private String mRestrictionName;
		private LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		private AtomicInteger mFiltersRunning = new AtomicInteger(0);
		private int mHighlightColor;

		public AppListAdapter(Context context, int resource,
				List<ApplicationInfoEx> objects, String initialRestrictionName) {
			super(context, resource, objects);
			mContext = context;
			mListAppAll = new ArrayList<ApplicationInfoEx>();
			mListAppAll.addAll(objects);
			mRestrictionName = initialRestrictionName;

			TypedArray ta1 = context.getTheme().obtainStyledAttributes(
					new int[] { android.R.attr.colorLongPressedHighlight });
			mHighlightColor = ta1.getColor(0, 0xFF00FF);
			ta1.recycle();
		}

		public void setRestrictionName(String restrictionName) {
			mRestrictionName = restrictionName;
		}

		public void showStats() {
			TextView tvStats = (TextView) findViewById(R.id.tvStats);
			String stats = String.format("%d/%d", this.getCount(),
					mListAppAll.size());
			if (mListAppSelected.size() > 0)
				stats += String.format(" (%d)", mListAppSelected.size());
			tvStats.setText(stats);
		}

		@Override
		public Filter getFilter() {
			return new AppFilter();
		}

		public void addAll(Collection<? extends ApplicationInfoEx> values) {
			for (ApplicationInfoEx value : values) {
				add(value);
			}
		}

		private class AppFilter extends Filter {
			public AppFilter() {
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				int filtersRunning = mFiltersRunning.addAndGet(1);
				FilterResults results = new FilterResults();

				// Get arguments
				String[] components = ((String) constraint).split("\\n");
				String fName = components[0];
				boolean fUsed = Boolean.parseBoolean(components[1]);
				boolean fInternet = Boolean.parseBoolean(components[2]);
				boolean fRestricted = Boolean.parseBoolean(components[3]);
				boolean fRestrictedNot = Boolean.parseBoolean(components[4]);
				boolean fPermission = Boolean.parseBoolean(components[5]);
				boolean fOnDemand = Boolean.parseBoolean(components[6]);
				boolean fOnDemandNot = Boolean.parseBoolean(components[7]);
				boolean fUser = Boolean.parseBoolean(components[8]);
				boolean fSystem = Boolean.parseBoolean(components[9]);

				// Match applications
				int current = 0;
				int max = AppListAdapter.this.mListAppAll.size();
				List<ApplicationInfoEx> lstApp = new ArrayList<ApplicationInfoEx>();
				for (ApplicationInfoEx xAppInfo : AppListAdapter.this.mListAppAll) {
					// Check if another filter has been started
					if (filtersRunning != mFiltersRunning.get())
						return null;

					// Send progress info to main activity
					current++;
					if (current % 5 == 0) {
						final int position = current;
						final int maximum = max;
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setProgress(getString(R.string.msg_applying),
										position, maximum);
							}
						});
					}

					// Get if name contains
					boolean contains = false;
					if (!fName.equals(""))
						contains = (xAppInfo.toString().toLowerCase()
								.contains(((String) fName).toLowerCase()));

					// Get if used
					boolean used = false;
					if (fUsed)
						used = (HookManager.getUsage(xAppInfo.getUid(),
								mRestrictionName, null) != 0);

					// Get if internet
					boolean internet = false;
					if (fInternet)
						internet = xAppInfo.hasInternet(mContext);

					// Get some restricted
					boolean someRestricted = false;
					if (fRestricted)
						for (PRestriction restriction : HookManager
								.getRestrictionList(xAppInfo.getUid(),
										mRestrictionName))
							if (restriction.restricted) {
								someRestricted = true;
								break;
							}

					// Get Android permission
					boolean permission = false;
					if (fPermission)
						if (mRestrictionName == null)
							permission = true;
						else if (HookManager.hasPermission(mContext, xAppInfo,
								mRestrictionName)
								|| HookManager.getUsage(xAppInfo.getUid(),
										mRestrictionName, null) > 0)
							permission = true;

					// Get if onDemand
					boolean onDemand = false;
					if (fOnDemand
							&& HookManager.isApplication(xAppInfo.getUid())) {
						onDemand = HookManager.getSettingBool(
								-xAppInfo.getUid(),
								HookManager.cSettingOnDemand, false, false);
						if (onDemand && mRestrictionName != null)
							onDemand = !HookManager.getRestrictionEx(
									xAppInfo.getUid(), mRestrictionName, null).asked;
					}

					// Get if user
					boolean user = false;
					if (fUser)
						user = !xAppInfo.isSystem();

					// Get if system
					boolean system = false;
					if (fSystem)
						system = xAppInfo.isSystem();

					// Apply filters
					if ((fName.equals("") ? true : contains)
							&& (fUsed ? used : true)
							&& (fInternet ? internet : true)
							&& (fRestricted ? (fRestrictedNot ? !someRestricted
									: someRestricted) : true)
							&& (fPermission ? permission : true)
							&& (fOnDemand ? (fOnDemandNot ? !onDemand
									: onDemand) : true)
							&& (fUser ? user : true)
							&& (fSystem ? system : true))
						lstApp.add(xAppInfo);
				}

				// Check again whether another filter has been started
				if (filtersRunning != mFiltersRunning.get())
					return null;

				// Apply current sorting
				Collections.sort(lstApp, mSorter);

				// Last check whether another filter has been started
				if (filtersRunning != mFiltersRunning.get())
					return null;

				synchronized (this) {
					results.values = lstApp;
					results.count = lstApp.size();
				}

				return results;
			}

			@Override
			@SuppressWarnings("unchecked")
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				if (results != null) {
					clear();
					TextView tvStats = (TextView) findViewById(R.id.tvStats);
					TextView tvState = (TextView) findViewById(R.id.tvState);
					ProgressBar pbFilter = (ProgressBar) findViewById(R.id.pbFilter);
					pbFilter.setVisibility(ProgressBar.GONE);
					tvStats.setVisibility(TextView.VISIBLE);

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setProgress(getString(R.string.title_restrict), 0,
									1);
						}
					});

					// Adjust progress state width
					RelativeLayout.LayoutParams tvStateLayout = (RelativeLayout.LayoutParams) tvState
							.getLayoutParams();
					tvStateLayout.addRule(RelativeLayout.LEFT_OF, R.id.tvStats);

					if (results.values == null)
						notifyDataSetInvalidated();
					else {
						addAll((ArrayList<ApplicationInfoEx>) results.values);
						notifyDataSetChanged();
					}
					AppListAdapter.this.showStats();
				}
			}
		}

		private class ViewHolder {
			private View row;
			private int position;
			public LinearLayout llAppType;
			public ImageView imgIcon;
			public TextView tvName;
			public ProgressBar pbRunning;
			public TextView tvPermissionSum;
			public LinearLayout llName;
			public List<String> mRestrictionList;

			public ViewHolder(View theRow, int thePosition) {
				row = theRow;
				position = thePosition;
				llAppType = (LinearLayout) row.findViewById(R.id.sm_llAppType);
				imgIcon = (ImageView) row.findViewById(R.id.sm_img_appicon);
				tvName = (TextView) row.findViewById(R.id.sm_main_appname);
				tvPermissionSum = (TextView) row
						.findViewById(R.id.sm_main_permission_sum);
				mRestrictionList = new ArrayList<String>();
			}
		}

		private class HolderTask extends AsyncTask<Object, Object, Object> {
			private int position;
			private ViewHolder holder;
			private ApplicationInfoEx xAppInfo = null;
			private int permissions;

			public HolderTask(int thePosition, ViewHolder theHolder,
					ApplicationInfoEx theAppInfo) {
				position = thePosition;
				holder = theHolder;
				xAppInfo = theAppInfo;
			}

			@Override
			protected Object doInBackground(Object... params) {
				if (xAppInfo != null) {
					String RestrictionNames[] = { "ReadMessage",
							"ReadContacts", "ReadCalllog", "ReadLocation",
							"ReadSERIAL", "SendMessage", "CallPhone",
							"PhoneState", "AudioRecord", "WebHistory",
							"TakePhoto", "2G3GNetwork", "WifiNetwork" };
					int sum = 0;
					holder.mRestrictionList.clear();
					for (int i = 0; i < RestrictionNames.length; i++)
						for (Hook md : HookManager
								.getHooks(RestrictionNames[i])) {
							boolean hasPermission = HookManager.hasPermission(
									mContext, new ApplicationInfoEx(mContext,
											xAppInfo.getUid()), md);
							if (hasPermission) {
								holder.mRestrictionList
										.add(RestrictionNames[i]);
								sum++;
								break;
							}
						}
					permissions = sum;
					return holder;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object result) {
				if (holder.position == position && result != null) {
					// Display icon
					holder.imgIcon.setImageDrawable(xAppInfo
							.getIcon(SoftmgrActivity.this));
					holder.imgIcon.setVisibility(View.VISIBLE);
					holder.tvPermissionSum.setText(permissions + "项权限");
				}
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.sm_vlist, null);
				holder = new ViewHolder(convertView, position);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
				holder.position = position;
			}

			// Get info
			final ApplicationInfoEx xAppInfo = getItem(holder.position);

			// Handle details click
			holder.llAppType.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent intentSettings = new Intent(SoftmgrActivity.this,
							SoftDetail.class);
					intentSettings.putExtra(SoftDetail.cUid, xAppInfo.getUid());
					intentSettings.putExtra(SoftDetail.cRestrictionName,
							mRestrictionName);
					intentSettings.putStringArrayListExtra(
							SoftDetail.cRestrictionList,
							(ArrayList<String>) holder.mRestrictionList);
					intentSettings.putExtra("ParentClassName", mContext.getClass()
							.getName());
					SoftmgrActivity.this.startActivity(intentSettings);
				}
			});

			// Set data
			holder.row.setBackgroundColor(Color.TRANSPARENT);
			holder.llAppType.setBackgroundColor(Color.TRANSPARENT);
			holder.imgIcon.setVisibility(View.INVISIBLE);
			holder.tvName.setText(xAppInfo.toString());
			holder.tvName.setTypeface(null, Typeface.NORMAL);
			holder.tvName.setEnabled(false);
			// holder.llName.setEnabled(false);

			// Async update
			new HolderTask(position, holder, xAppInfo).executeOnExecutor(
					mExecutor, (Object) null);

			return convertView;
		}
	}

	// Share operations progress listener

	private void setProgress(String text, int progress, int max) {
		// Set up the progress bar
		if (mProgressWidth == 0) {
			final View vProgressEmpty = (View) findViewById(R.id.vProgressEmpty);
			mProgressWidth = vProgressEmpty.getMeasuredWidth();
		}
		// Display stuff
		TextView tvState = (TextView) findViewById(R.id.tvState);
		if (text != null)
			tvState.setText(text);
		if (max == 0)
			max = 1;
		mProgress = (int) ((float) mProgressWidth) * progress / max;

		View vProgressFull = (View) findViewById(R.id.vProgressFull);
		vProgressFull.getLayoutParams().width = mProgress;
	}
}