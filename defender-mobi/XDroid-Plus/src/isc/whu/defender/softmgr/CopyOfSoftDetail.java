package isc.whu.defender.softmgr;

import isc.whu.defender.MonitorService;
import isc.whu.defender.R;
import isc.whu.defender.xmonitor.ApplicationInfoEx;
import isc.whu.defender.xmonitor.Hook;
import isc.whu.defender.xmonitor.HookManager;
import isc.whu.defender.xmonitor.RState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.android.internal.util.AsyncTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class CopyOfSoftDetail extends Activity {
	String TAG = " XDroid-sda ";
	private ApplicationInfoEx mAppInfo = null;
	private String mMethodName = null;
	private boolean mTrusted = false;
	private RestrictionAdapter mPrivacyListAdapter = null;
	private RestrictionAdapter mSafetyListAdapter = null;
	private RestrictionAdapter mNetListAdapter = null;
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
	private int mUid = 1111;
	private Context mContext = null;
	private PackageInfo pkgInfo;

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

	int img[] = new int[] { R.drawable.accept, R.drawable.reject,
			R.drawable.prompt };

	// public static final int ACTION_ALLOW = 1;
	// public static final int ACTION_REFUSE = 2;
	// public static final int ACTION_PROMPT = 3;
	// @AppBehTbl.java
	public static HashMap<Integer, String> codeMap = new HashMap<Integer, String>();
	static {
		codeMap.put(11, "使用网络进行粗略定位");
		codeMap.put(12, "使用GPS进行精准定位");
		codeMap.put(2, "访问短信数据库,读取短信");
		codeMap.put(3, "接收短信");
		codeMap.put(4, "发送短信");
		codeMap.put(5, "读取联系人");
		codeMap.put(13, "读取通话记录");
		codeMap.put(6, "读取浏览器历史记录");
		codeMap.put(7, "读取手机状态,包括IMEI等");
		codeMap.put(8, "拨打电话");
		codeMap.put(9, "拍照或者摄像");
		codeMap.put(10, "通话和背景声音录音");
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Check privacy service client
		if (!MonitorService.checkClient())
			return;

		// Set layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
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
		RestricionListTask restrictListTask = new RestricionListTask();
		restrictListTask.executeOnExecutor(mExecutor, (Object) null);

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
				HookManager.cSettingTrust, false, true);
		cbTrust.setChecked(mTrusted);
		// Listen checkbox checked-accept not checked-prompt
		cbTrust.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					final boolean isChecked) {
				// update user restriction
				new AsyncTask<Object, Object, Object>() {
					@Override
					protected Object doInBackground(Object... arg0) {
						HookManager.setSetting(mAppInfo.getUid(),
								HookManager.cSettingTrust,
								Boolean.toString(isChecked));
						mTrusted = isChecked;
						for (String restrionName : mRestrictionList)
							HookManager.setRestriction(mAppInfo.getUid(),
									restrionName, null, !isChecked, isChecked);
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
					}
				}.executeOnExecutor(mExecutor);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mPrivacyListAdapter != null)
			mPrivacyListAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mPackageChangeReceiverRegistered) {
			unregisterReceiver(mPackageChangeReceiver);
			mPackageChangeReceiverRegistered = false;
		}
	}

	// Tasks
	private class RestricionListTask extends
			AsyncTask<Object, Integer, Map<String, List<String>>> {
		String[] categoryName = new String[] { "privacy", "safety", "net" };

		@Override
		protected Map<String, List<String>> doInBackground(Object... params) {
			Map<String, List<String>> restrionCategory = new HashMap<String, List<String>>();
			Map<String, List<String>> result = new HashMap<String, List<String>>();
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
				List<String> restrictions = new ArrayList<String>();
				for (String rRestrictionName : value) {
					if (mRestrictionList.contains(rRestrictionName))
						restrictions.add(rRestrictionName);
				}
				result.put(key, restrictions);
			}
			return result;
		}

		@Override
		protected void onPostExecute(Map<String, List<String>> restrictionList) {
			// Fill privacy list view adapter
			final ListView lvPrivacy = (ListView) findViewById(R.id.sm_lv_privacy);
			mPrivacyListAdapter = new RestrictionAdapter(CopyOfSoftDetail.this,
					R.layout.sm_dlist, restrictionList.get("privacy"),
					mAppInfo, mMethodName);
			lvPrivacy.setAdapter(mPrivacyListAdapter);
			setListViewHeightBasedOnChildren(lvPrivacy);

			final ListView lvSafety = (ListView) findViewById(R.id.sm_lv_safety);
			mSafetyListAdapter = new RestrictionAdapter(CopyOfSoftDetail.this,
					R.layout.sm_dlist, restrictionList.get("safety"), mAppInfo,
					mMethodName);
			lvSafety.setAdapter(mSafetyListAdapter);
			setListViewHeightBasedOnChildren(lvSafety);

			final ListView lvNet = (ListView) findViewById(R.id.sm_lv_network);
			mNetListAdapter = new RestrictionAdapter(CopyOfSoftDetail.this,
					R.layout.sm_dlist, restrictionList.get("net"), mAppInfo,
					mMethodName);
			lvNet.setAdapter(mNetListAdapter);
			setListViewHeightBasedOnChildren(lvNet);
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
	private class RestrictionAdapter extends ArrayAdapter<String> {
		private ApplicationInfoEx mAppInfo;
		private String[] mSelectedRestriction;
		private String mSelectedMethodName;
		private List<String> mListRestriction;
		private HashMap<Integer, List<Hook>> mHook;
		private LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public RestrictionAdapter(Context context, int resource,
				List<String> objects, ApplicationInfoEx appInfo,
				String selectedMethodName) {
			super(context, resource, objects);
			mAppInfo = appInfo;
			mListRestriction = objects;
			mSelectedMethodName = selectedMethodName;
			mHook = new LinkedHashMap<Integer, List<Hook>>();
		}

		private class ViewHolder {
			private View row;
			private int position;
			public TextView tvType;
			public TextView tvDescription;
			public ImageView imgAction;
			public LinearLayout llRestrctionName;

			private ViewHolder(View theRow, int Position) {
				row = theRow;
				position = Position;
				tvType = (TextView) row.findViewById(R.id.sm_detail_type);
				tvDescription = (TextView) row
						.findViewById(R.id.sm_detail_description);
				imgAction = (ImageView) row.findViewById(R.id.sm_detail_action);
				llRestrctionName = (LinearLayout) row
						.findViewById(R.id.sm_detail_ll);

			}
		}

		private class HolderTask extends AsyncTask<Object, Object, Object> {
			private int position;
			private ViewHolder holder;
			private String restrictionName;
			private boolean used;
			private boolean permission;
			private RState rstate;
			private boolean ondemand;

			public HolderTask(int thePosition, ViewHolder theHolder,
					String theRestrictionName) {
				position = thePosition;
				holder = theHolder;
				restrictionName = theRestrictionName;
			}

			@Override
			protected Object doInBackground(Object... params) {
				if (restrictionName != null) {
					// Get info
					int userId = isc.whu.defender.xmonitor.Util
							.getUserId(Process.myUid());
					used = (HookManager.getUsage(mAppInfo.getUid(),
							restrictionName, null) != 0);
					permission = HookManager.hasPermission(CopyOfSoftDetail.this,
							mAppInfo, restrictionName);
					rstate = new RState(mAppInfo.getUid(), restrictionName,
							null);
					ondemand = (HookManager.isApplication(mAppInfo.getUid()) && HookManager
							.getSettingBool(userId,
									HookManager.cSettingOnDemand, true, false));
					if (ondemand)
						ondemand = HookManager.getSettingBool(
								-mAppInfo.getUid(),
								HookManager.cSettingOnDemand, false, false);
					return holder;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object result) {
				if (holder.position == position && result != null) {
					// Display restriction
					if (!rstate.asked) {
						//
						HookManager.setSetting(mAppInfo.getUid(),
								HookManager.cSettingOnDemand,
								Boolean.toString(true));
						holder.imgAction.setImageDrawable(getResources()
								.getDrawable(R.drawable.prompt));
					} else if (rstate.restricted || rstate.partialRestricted)
						holder.imgAction.setImageDrawable(getResources()
								.getDrawable(R.drawable.reject));
					else
						holder.imgAction.setImageDrawable(getResources()
								.getDrawable(R.drawable.accept));
					holder.imgAction.setVisibility(View.VISIBLE);
					holder.llRestrctionName
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									if (!mTrusted)
										initPopWindow(view,
												mListRestriction.get(position),
												holder);
								}
							});
				}
			}

			private void initPopWindow(View view, String restrictionName,
					ViewHolder viewholder) {
				try {
					View contentView = LayoutInflater.from(
							getApplicationContext()).inflate(R.layout.sm_popup,
							null);
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
							LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);
					mPopupWindow
							.setAnimationStyle(R.style.popwindow_anim_style);
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
							// update user restriction
							new AsyncTask<Object, Object, Object>() {
								@Override
								protected Object doInBackground(Object... arg0) {
									boolean bChanged = false;
									String touchedView = (v == llPrompt ? "Prompt"
											: (v == llAllow ? "Allow"
													: "Reject"));
									Log.i("SoftDetail", "PopupWindow("
											+ touchedView + ") asked:"
											+ rstate.asked + ", restricted:"
											+ rstate.restricted);
									if (v == llPrompt && rstate.asked) {
										HookManager.setSetting(
												mAppInfo.getUid(),
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
										notifyDataSetChanged();
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
					View v = getWindow()
							.findViewById(Window.ID_ANDROID_CONTENT);
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
						mPopupWindow.showAtLocation(view, Gravity.NO_GRAVITY,
								0, location[1] - rootHeight);
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

		@Override
		public int getCount() {
			return mListRestriction.size();
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			final ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.sm_dlist, null);
				holder = new ViewHolder(convertView, position);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
				holder.position = position;
			}

			if (mListRestriction.size() != 0) {
				// Display restriction name=type-description
				String restrictionName = mListRestriction.get(position);
				int restrictId = getResources().getIdentifier(
						"restrict_" + restrictionName, "string",
						getPackageName());
				int descriptionId = getResources().getIdentifier(
						"description_" + restrictionName, "string",
						getPackageName());
				holder.tvType.setText(restrictId);
				holder.tvDescription.setText(descriptionId);
				// Async update
				new HolderTask(position, holder, mListRestriction.get(position))
						.executeOnExecutor(mExecutor, (Object) null);
			}
			return convertView;
		}
	}

	public int getThemed(int attr) {
		TypedValue tv = new TypedValue();
		getTheme().resolveAttribute(attr, tv, true);
		return tv.resourceId;
	}
}