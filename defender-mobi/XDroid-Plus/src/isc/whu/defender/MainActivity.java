package isc.whu.defender;

import isc.whu.defender.common.CommonTools;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements OnViewChangeListener,
		OnClickListener {

	protected static final String TAG = "XDroid-app";

	private Context mContext;
	private MyScrollLayout mScrollLayout;
	private int mViewCount;
	private int mCurSel;
	private LinearLayout linearCenter;
	// private FrameLayout frame_sysclear;
	private MainListItem listItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainscreen);

		mContext = this;

		mScrollLayout = (MyScrollLayout) findViewById(R.id.ScrollLayout);
		listItem = (MainListItem) findViewById(R.id.mainscreen_list_beh_logger);
		linearCenter = (LinearLayout) findViewById(R.id.center);
		// frame_sysclear = (FrameLayout) findViewById(R.id.main_sysclear);
		mScrollLayout.SetOnViewChangeListener(this);
		mViewCount = mScrollLayout.getChildCount();
		mCurSel = 0;
		

		/*
		 * 设置主动防御的状态按钮
		 */
		Button btn = (Button) findViewById(R.id.mainscreen_btn_proactive);

		final boolean xposedActive = XDroidDefender
				.getXposedAppProcessVersion() >= CommonTools
				.getJarLatestVersion();
		// Toast.makeText(
		// mContext,
		// xposedActive + ":"
		// + XDroidDefender.getXposedAppProcessVersion() + "? >="
		// + CommonTools.getJarLatestVersion(), Toast.LENGTH_LONG)
		// .show();
		if (!xposedActive) { // 主动防御未启动
			btn.setText(R.string.click_to_start_proactive);
		} else {
			btn.setText(R.string.proactive_has_started);
		}

		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!xposedActive) {
					Intent intent = new Intent(MainActivity.this,
							SettingsActivity.class);
					startActivity(intent);
				}
			}
		});
		// start service Malware Detection : SVMScanner
		// startService(new Intent(this, SVMScanner.class));
	}

	private void setCurPoint(int index) {
		if (index < 0 || index > mViewCount - 1 || mCurSel == index) {
			return;
		}
	}

	@Override
	public void OnViewChange(int view) {
		// TODO Auto-generated method stub
		setCurPoint(view);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int pos = (Integer) (v.getTag());
		setCurPoint(pos);
		mScrollLayout.snapToScreen(pos);
	}

}