package isc.whu.defender;

import isc.whu.defender.scanner.DetectionActivity;
import isc.whu.defender.softmgr.SoftmgrActivity;
import isc.whu.defender.traffic.TrafficMonitorService;
import isc.whu.defender.trafmon.TrafficActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainListItem extends LinearLayout {
	private ImageView iv_icon;
	private TextView tv_title;
	private TextView tv_summary;
	private int resouceId = -1;
	private String title;
	private String TAG = "ListItem";
	private Context mContext;

	private float mLastMotionX;
	private float mLastMotionY;

	private boolean mIsBeingDragged = false;

	public MainListItem(Context context) {
		this(context, null);
	}

	public MainListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		setFocusable(true);
		LayoutInflater.from(context).inflate(R.layout.list_item, this, true);
		iv_icon = (ImageView) findViewById(R.id.list_item_icon);
		tv_title = (TextView) findViewById(R.id.list_item_title);
		tv_summary = (TextView) findViewById(R.id.list_item_summary);
		TypedArray typeArray = context.obtainStyledAttributes(attrs,
				R.styleable.MainListItem);

		int N = typeArray.getIndexCount();
		for (int i = 0; i < N; i++) {
			int attr = typeArray.getIndex(i);
			switch (attr) {
			case R.styleable.MainListItem__title:
				resouceId = typeArray.getResourceId(
						R.styleable.MainListItem__title, 0);
				title = (resouceId > 0 ? typeArray.getResources().getText(
						resouceId) : typeArray
						.getString(R.styleable.MainListItem__title)).toString();
				tv_title.setText(resouceId > 0 ? typeArray.getResources()
						.getText(resouceId) : typeArray
						.getString(R.styleable.MainListItem__title));
				// addView(tv_title);
				break;
			case R.styleable.MainListItem__summary:
				resouceId = typeArray.getResourceId(
						R.styleable.MainListItem__summary, 0);
				tv_summary.setText(resouceId > 0 ? typeArray.getResources()
						.getText(resouceId) : typeArray
						.getString(R.styleable.MainListItem__summary));
				// addView(tv_summary);
				break;
			case R.styleable.MainListItem__icon:
				Drawable icon = typeArray
						.getDrawable(R.styleable.MainListItem__icon);
				iv_icon.setImageDrawable(icon);
				// addView(iv_icon);
				break;
			}
		}
		typeArray.recycle();
		/*
		 * this.setOnClickListener(new OnClickListener() {
		 * 
		 * public void onClick(View v) { Log.v("Activity", title); } });
		 */
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onInterceptTouchEvent");

		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
			// Log.v(TAG, "onInterceptTouchEvent:short cut:return true");
			return false;
		}

		final float x = ev.getX();
		final float y = ev.getY();
		// Log.v(TAG, "oldx:" + mLastMotionX + "oldy:" + mLastMotionY + "-x:" +
		// x + ",y:" + y);

		switch (action) {
		case MotionEvent.ACTION_MOVE:
			// Log.v(TAG, "ACTION_MOVE");
			final int yDiff = (int) Math.abs(mLastMotionY - y);
			if (yDiff > 5) {
				mLastMotionX = x;
				mLastMotionY = y;
				mIsBeingDragged = true;
			}
			break;
		case MotionEvent.ACTION_DOWN:
			// Log.v(TAG, "ACTION_DOWN");
			mLastMotionX = x;
			mLastMotionY = y;
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// Log.v(TAG, "onInterceptTouchEvent:ACTION_DOWN");
			mIsBeingDragged = false;
			break;
		}
		// Log.v(TAG, "onInterceptTouchEvent:return " + mIsBeingDragged);
		return mIsBeingDragged;
	}

	/**
	 * 根据点击的列表项的ID打开相应模块的界面
	 * 
	 * @param id
	 *            列表项的ID
	 */
	private void doStart(int id) {
		Intent intent;
		if (id == R.id.mainscreen_list_malware_detection) {
			intent = new Intent(mContext, DetectionActivity.class);
			intent.putExtra("ParentClassName", mContext.getClass().getName());
			mContext.startActivity(intent);
		} else if (id == R.id.mainscreen_list_app_management) {
			intent = new Intent(mContext, SoftmgrActivity.class);
			intent.putExtra("ParentClassName", mContext.getClass().getName());
			mContext.startActivity(intent);
		} else if (id == R.id.mainscreen_list_beh_logger) {
			intent = new Intent(mContext, ActivityUsage.class);
			intent.putExtra("ParentClassName", mContext.getClass().getName());
			mContext.startActivity(intent);
		} else if (id == R.id.mainscreen_list_traffic_monitor) {
			intent = new Intent(mContext, TrafficActivity.class);
			intent.putExtra("ParentClassName", mContext.getClass().getName());
			mContext.startActivity(intent);
		} else if (id == R.id.mainscreen_list_system_setttings) {
			intent = new Intent(mContext, SettingsActivity.class);
			mContext.startActivity(intent);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		final int action = event.getAction();
		// Log.v(TAG, "onTouchEvent");
		final float x = event.getX();
		final float y = event.getY();
		// Log.v(TAG, "oldx:" + mLastMotionX + "oldy:" + mLastMotionY + "-x:" +
		// x + ",y:" + y);
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mLastMotionX = x;
			mLastMotionY = y;
			// Log.v(TAG, "ACTION_DOWN");
			break;
		case MotionEvent.ACTION_MOVE:
			// Log.v(TAG, "ACTION_MOVE");
			break;
		case MotionEvent.ACTION_UP:
			// Log.v(TAG, "ACTION_UP");
			doStart(this.getId());
		}
		return true;
	}

	public String getTitle() {
		return title;
	}
}
