package isc.whu.defender.softmgr;

import isc.whu.defender.MonitorService;
import isc.whu.defender.R;
import isc.whu.defender.trafmon.TrafficActivity;
import isc.whu.defender.xmonitor.ApplicationInfoEx;
import isc.whu.defender.xmonitor.Hook;
import isc.whu.defender.xmonitor.HookManager;
import isc.whu.defender.xmonitor.PRestriction;
import isc.whu.defender.xmonitor.RState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.android.internal.util.AsyncTask;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.drm.ProcessedData;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class SoftDetail extends Activity {
	String TAG = " XDroid-sda ";
	private ApplicationInfoEx mAppInfo = null;
	private String mMethodName = null;
	private boolean mTrusted = false;
	private RestrictionAdapter mPrivacyListAdapter = null;
	private RestrictionAdapter mSafetyListAdapter = null;
	private RestrictionAdapter mNetListAdapter = null;
	private RestricionListTask mRestrictListTask = null;
	private List<String> mRestrictionList = new ArrayList<String>();

	private final int MODE_PRIVACY = 1;
	private final int MODE_SAFETY = 2;
	private final int MODE_NETWORK = 3;

	public static final String cUid = "Uid";
	public static final String cRestrictionName = "RestrictionName";
	public static final String cMethodName = "MethodName";
	public static final String cRestrictionList = "Restrictions";
	public static final String cAction = "Action";
	public static final int cActionClear = 1;
	public static final int cActionSettings = 2;
	public static final int cActionRefresh = 3;
	public static final String cActionAccept = "Accept";
	public static final String cActionPrompt = "Prompt";
	public static final String cActionReject = "Reject";

	private boolean mPackageChangeReceiverRegistered = false;

	private BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
			if (mAppInfo.getUid() == uid)
				finish();
		}
	};

	private PopupWindow mPopupWindow = null;
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
		// Check privacy service client
		if (!MonitorService.checkClient())
			return;

		// Set layout
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sm_detail);
		mContext = this;

		// Get arguments
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
			return;
		}

		int uid = extras.getInt(cUid);
		String restrictionName = (extras.containsKey(cRestrictionName) ? extras
				.getString(cRestrictionName) : null);
		String mMethodName = (extras.containsKey(cMethodName) ? extras
				.getString(cMethodName) : null);
		mRestrictionList = (extras.containsKey(cRestrictionList) ? extras
				.getStringArrayList(cRestrictionList) : null);
		// Get app info
		mAppInfo = new ApplicationInfoEx(this, uid);
		if (mAppInfo.getPackageName().size() == 0) {
			finish();
			return;
		}

		// Set title
		setTitle(String.format("%s - %s", getString(R.string.app_name),
				TextUtils.join(", ", mAppInfo.getApplicationName())));

		// Display app name and version
		TextView tvAppName = (TextView) findViewById(R.id.sm_item_appname);
		tvAppName.setText(mAppInfo.toString());
		TextView tvVersion = (TextView) findViewById(R.id.sm_item_version);
		tvVersion.setText("版本：" + mAppInfo.getPackageVersionName(this));

		// Background color
		if (mAppInfo.isSystem()) {
			LinearLayout llInfo = (LinearLayout) findViewById(R.id.sm_soft_title);
			llInfo.setBackgroundColor(getResources().getColor(
					R.color.color_dangerous_light));
		}

		// Display app icon
		final ImageView imgIcon = (ImageView) findViewById(R.id.sm_item_appicon);
		imgIcon.setImageDrawable(mAppInfo.getIcon(this));

		// Start task to get app list
		RestricionListTask mRestrictListTask = new RestricionListTask();
		mRestrictListTask.executeOnExecutor(mExecutor, (Object) null);

		// Listen for package add/remove
		IntentFilter iff = new IntentFilter();
		iff.addAction(Intent.ACTION_PACKAGE_REMOVED);
		iff.addDataScheme("package");
		registerReceiver(mPackageChangeReceiver, iff);
		mPackageChangeReceiverRegistered = true;

		// Set trust
		CheckBox cbTrust = (CheckBox) findViewById(R.id.sm_cb_trust);
		final TextView tvTrust = (TextView) findViewById(R.id.sm_tv_trust);
		mTrusted = HookManager.getSettingBool(mAppInfo.getUid(),
				HookManager.cSettingTrust, false, false);
		cbTrust.setChecked(mTrusted);
		// Listen checkbox checked-accept not checked-prompt
		cbTrust.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					final boolean isChecked) {
				// update user restriction
				new AsyncTask<Object, Object, Object>() {
					@Override
					protected void onPreExecute() {
						mProcessDialog = new ProgressDialog(mContext);
						mProcessDialog
								.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						mProcessDialog
								.setMessage(getString(R.string.msg_loading));
						mProcessDialog.setIndeterminate(true);
						mProcessDialog.setCancelable(false);
						mProcessDialog.show();
					}

					@Override
					protected Object doInBackground(Object... arg0) {
						HookManager.setSetting(mAppInfo.getUid(),
								HookManager.cSettingTrust,
								Boolean.toString(isChecked));
						mTrusted = isChecked;
						List<PRestriction> listPRestriction = new ArrayList<PRestriction>();

						if (isChecked) {
							for (String restrionName : mRestrictionList)
								HookManager.deleteRestrictions(
										mAppInfo.getUid(), restrionName, false);
							HookManager.setSetting(mAppInfo.getUid(),
									HookManager.cSettingModifyTime,
									Long.toString(System.currentTimeMillis()));
						} else {
							HookManager.setSetting(mAppInfo.getUid(),
									HookManager.cSettingOnDemand,
									Boolean.toString(true));
							for (String restrionName : mRestrictionList)
								listPRestriction.add(new PRestriction(mAppInfo
										.getUid(), restrionName, null, false,
										false));
							HookManager.setRestrictionList(listPRestriction);
							HookManager.setSetting(
									mAppInfo.getUid(),
									HookManager.cSettingState,
									Integer.toString(SoftmgrActivity.STATE_CHANGED));
							HookManager.setSetting(mAppInfo.getUid(),
									HookManager.cSettingModifyTime,
									Long.toString(System.currentTimeMillis()));
						}

						List<RestrictionAdapter> adapters = new ArrayList<RestrictionAdapter>();
						adapters.add(mPrivacyListAdapter);
						adapters.add(mSafetyListAdapter);
						adapters.add(mNetListAdapter);
						for (RestrictionAdapter adapter : adapters) {
							for (Map<String, Object> map : adapter.mListItems) {
								map.remove(cAction);
								map.put(cAction,
										getResources().getDrawable(
												isChecked ? R.drawable.accept
														: R.drawable.prompt));
							}
						}
						return null;
					}

					@Override
					protected void onPostExecute(Object result) {
						// Needed to update children
						mPrivacyListAdapter.notifyDataSetChanged();
						mSafetyListAdapter.notifyDataSetChanged();
						mNetListAdapter.notifyDataSetChanged();
						tvTrust.setText(isChecked ? R.string.sm_item_trust_enable
								: R.string.sm_item_trust_disable);
						// Dismiss progress dialog
						if (mProcessDialog.isShowing())
							try {
								mProcessDialog.dismiss();
							} catch (IllegalArgumentException ignored) {
							}
					}
				}.executeOnExecutor(mExecutor);
			}
		});
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mSafetyListAdapter != null)
			mSafetyListAdapter.notifyDataSetChanged();
		if (mPrivacyListAdapter != null)
			mPrivacyListAdapter.notifyDataSetChanged();
		if (mNetListAdapter != null)
			mNetListAdapter.notifyDataSetChanged();
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
			newIntent = new Intent(SoftDetail.this, Class.forName(className));

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return newIntent;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mPackageChangeReceiverRegistered) {
			unregisterReceiver(mPackageChangeReceiver);
			mPackageChangeReceiverRegistered = false;
		}
	}

	public class RestrictionAdapter extends BaseAdapter {
		private Context mContext;
		private List<Map<String, Object>> mListItems;
		private LayoutInflater mInflater;

		public RestrictionAdapter(Context context,
				List<Map<String, Object>> list) {
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
				convertView = mInflater.inflate(R.layout.sm_dlist, null);
			TextView tvType = (TextView) convertView
					.findViewById(R.id.sm_detail_type);
			TextView tvDescription = (TextView) convertView
					.findViewById(R.id.sm_detail_description);
			final ImageView imgAction = (ImageView) convertView
					.findViewById(R.id.sm_detail_action);
			LinearLayout llRestrctionName = (LinearLayout) convertView
					.findViewById(R.id.sm_detail_ll);

			final String restrictionName = (String) mListItems.get(position)
					.get(cRestrictionName);
			int restrictId = getResources().getIdentifier(
					"restrict_" + restrictionName, "string", getPackageName());
			int descriptionId = getResources().getIdentifier(
					"description_" + restrictionName, "string",
					getPackageName());
			tvType.setText(restrictId);
			tvDescription.setText(descriptionId);
			imgAction.setImageDrawable((Drawable) mListItems.get(position).get(
					cAction));
			llRestrctionName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (!mTrusted)
						initPopWindow(view, restrictionName, position);
				}
			});
			return convertView;
		}

		private void initPopWindow(View view, final String restrictionName,
				final int position) {
			try {
				View contentView = LayoutInflater.from(getApplicationContext())
						.inflate(R.layout.sm_popup, null);
				LinearLayout popuplayout = (LinearLayout) contentView
						.findViewById(R.id.sm_popup_layout);
				// 当菜单出现时，最外层布局接受Touch事件
				popuplayout.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						hidePopupWindow();
						return false;
					}
				});
				// 实例化并设置PopupWindow属性
				mPopupWindow = new PopupWindow(popuplayout,
						LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				mPopupWindow.setAnimationStyle(R.style.popwindow_anim_style);
				mPopupWindow.setContentView(contentView);

				final LinearLayout llAllow = (LinearLayout) contentView
						.findViewById(R.id.sm_popup_allow);
				final LinearLayout llPrompt = (LinearLayout) contentView
						.findViewById(R.id.sm_popup_prompt);
				final LinearLayout llReject = (LinearLayout) contentView
						.findViewById(R.id.sm_popup_reject);
				// 当菜单出现时焦点会落在3个LinearLayout上
				OnKeyListener mKeyListener = new OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						// TODO Auto-generated method stub
						if (event.getAction() == KeyEvent.ACTION_DOWN) {
							switch (keyCode) {
							case KeyEvent.KEYCODE_MENU:
								return hidePopupWindow();
							case KeyEvent.KEYCODE_BACK:
								return hidePopupWindow();
							}
						}
						return false;
					}
				};
				// popupwindow在初始时，会检测background是否为null,如果是onTouch or onKey
				// events就不会响应
				popuplayout.setOnKeyListener(mKeyListener);
				OnClickListener mClickListener = new View.OnClickListener() {
					@Override
					public void onClick(final View v) {

						new AsyncTask<Object, Object, Object>() {
							@Override
							protected void onPreExecute() {
								mProcessDialog = new ProgressDialog(mContext);
								mProcessDialog
										.setProgressStyle(ProgressDialog.STYLE_SPINNER);
								mProcessDialog
										.setMessage(getString(R.string.msg_loading));
								mProcessDialog.setIndeterminate(true);
								mProcessDialog.setCancelable(false);
								mProcessDialog.show();
							}

							// update user restriction
							@Override
							protected Object doInBackground(Object... arg0) {
								boolean bChanged = false;
								RState rstate = new RState(mAppInfo.getUid(),
										restrictionName, null);
								if (v == llPrompt && rstate.asked) {
									HookManager.setSetting(mAppInfo.getUid(),
											HookManager.cSettingOnDemand,
											Boolean.toString(true));
									rstate.toggleAsked();
									bChanged = true;
								} else if (!rstate.asked) {
									rstate.toggleAsked();
									bChanged = true;
								} else if ((v == llAllow && rstate.restricted)
										|| (v == llReject && !rstate.restricted)) {
									rstate.toggleRestriction();
									bChanged = true;
								}
								return bChanged;
							}

							@Override
							protected void onPostExecute(Object result) {
								if ((Boolean) result) {
									// Needed to update children
									mListItems.get(position).remove(cAction);
									int action = v == llAllow ? R.drawable.accept
											: (v == llReject ? R.drawable.reject
													: R.drawable.prompt);
									mListItems.get(position).put(cAction,
											getResources().getDrawable(action));
									notifyDataSetChanged();
								}
								// Dismiss progress dialog
								if (mProcessDialog.isShowing())
									try {
										mProcessDialog.dismiss();
									} catch (IllegalArgumentException ignored) {
									}
							}
						}.executeOnExecutor(mExecutor);
						hidePopupWindow();
					}
				};

				llAllow.setOnClickListener(mClickListener);
				llPrompt.setOnClickListener(mClickListener);
				llReject.setOnClickListener(mClickListener);
				mPopupWindow.setOutsideTouchable(true);
				mPopupWindow.setFocusable(true);

				int[] location = new int[2];
				view.getLocationOnScreen(location);
				// ScreenSize except titlebar and notification bar
				View v = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
				int height = v.getHeight();
				// get the Height of PopupWindow
				LayoutInflater mInflater = (LayoutInflater) getApplicationContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View mRootView = (ViewGroup) mInflater.inflate(
						R.layout.sm_popup, null);
				mRootView.measure(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT);
				int rootHeight = mRootView.getMeasuredHeight();
				// space not enough,popupWindow show up-forward
				if (height - location[1] < view.getBottom())
					mPopupWindow.showAtLocation(view, Gravity.NO_GRAVITY, 0,
							location[1] - rootHeight);
				else
					mPopupWindow.showAsDropDown(view);// 显示在按钮下方
			} catch (Exception e) {
				e.printStackTrace();
				Log.v("Popupwindow", "Err:" + e.toString());
			}
		}

		private boolean hidePopupWindow() {
			if (null != mPopupWindow && mPopupWindow.isShowing()) {
				mPopupWindow.dismiss();
				mPopupWindow = null;
				return true;
			}
			return false;
		}
	}

	// Tasks
	private class RestricionListTask extends
			AsyncTask<Object, Integer, Map<String, List<Map<String, Object>>>> {
		String[] categoryName = new String[] { "privacy", "safety", "net" };

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
		protected Map<String, List<Map<String, Object>>> doInBackground(
				Object... params) {
			HashMap<String, List<String>> restrionCategory = new HashMap<String, List<String>>();
			Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();
			restrionCategory.put("privacy", Arrays
					.asList("ReadMessage", "ReadContacts", "ReadCalllog",
							"ReadLocation", "ReadSERIAL"));
			restrionCategory.put("safety", Arrays.asList("SendMessage",
					"CallPhone", "PhoneState", "AudioRecord", "WebHistory",
					"TakePhoto"));
			restrionCategory.put("net",
					Arrays.asList("2G3GNetwork", "WifiNetwork"));
			for (Map.Entry entry : restrionCategory.entrySet()) {
				String key = (String) entry.getKey();
				List<String> value = (List<String>) entry.getValue();
				List<Map<String, Object>> restrictions = new ArrayList<Map<String, Object>>();
				for (String rRestrictionName : value) {
					if (mRestrictionList.contains(rRestrictionName)) {
						RState rstate = new RState(mAppInfo.getUid(),
								rRestrictionName, null);
						Map<String, Object> pair = new HashMap<String, Object>();
						int action = !rstate.asked ? R.drawable.prompt
								: (rstate.restricted ? R.drawable.reject
										: R.drawable.accept);
						pair.put(cRestrictionName, rRestrictionName);
						pair.put(cAction, getResources().getDrawable(action));
						restrictions.add(pair);
					}
				}
				result.put(key, restrictions);
			}
			return result;
		}

		@Override
		protected void onPostExecute(
				Map<String, List<Map<String, Object>>> restrictionList) {
			// Fill privacy list view adapter
			final ListView lvPrivacy = (ListView) findViewById(R.id.sm_lv_privacy);
			mPrivacyListAdapter = new RestrictionAdapter(SoftDetail.this,
					restrictionList.get("privacy"));
			lvPrivacy.setAdapter(mPrivacyListAdapter);
			setListViewHeightBasedOnChildren(lvPrivacy);

			final ListView lvSafety = (ListView) findViewById(R.id.sm_lv_safety);
			mSafetyListAdapter = new RestrictionAdapter(SoftDetail.this,
					restrictionList.get("safety"));
			lvSafety.setAdapter(mSafetyListAdapter);
			setListViewHeightBasedOnChildren(lvSafety);

			final ListView lvNet = (ListView) findViewById(R.id.sm_lv_network);
			mNetListAdapter = new RestrictionAdapter(SoftDetail.this,
					restrictionList.get("net"));
			lvNet.setAdapter(mNetListAdapter);
			setListViewHeightBasedOnChildren(lvNet);
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
		RestrictionAdapter listAdapter = (RestrictionAdapter) listView
				.getAdapter();
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

	// Adapters

	public int getThemed(int attr) {
		TypedValue tv = new TypedValue();
		getTheme().resolveAttribute(attr, tv, true);
		return tv.resourceId;
	}
}