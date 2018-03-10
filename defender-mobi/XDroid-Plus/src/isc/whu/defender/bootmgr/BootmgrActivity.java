package isc.whu.defender.bootmgr;

import isc.whu.defender.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Tab页面手势滑动切换以及动画效果
 * 
 * @author D.Winter
 * 
 */
public class BootmgrActivity extends Activity {
	// ViewPager is provided in android-support-v4.jar
	private ViewPager mPager;		//页卡内容
	private List<View> listViews; 	// Tab页面列表
	private ImageView cursor;		// 动画图片
	private TextView t1, t2;		// 页卡头标
	private int offset = 0;			// 动画图片偏移量
	private int currIndex = 0;		// 当前页卡编号
	private int bmpW;				// 动画图片宽度
	private Context mContext;
	
	List<Map<String, Object>> mListNormalApp = new ArrayList<Map<String, Object>>();
	List<Map<String, Object>> mListSystemApp = new ArrayList<Map<String, Object>>(); 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bootmgr_main);

		mContext = this;
		
		InitImageView();
		InitTextView();
		InitViewPager();
		
		populateListData();
	}
	
	/**
	 * 初始化头标
	 */
	private void InitTextView() {
		t1 = (TextView) findViewById(R.id.text1);
		t2 = (TextView) findViewById(R.id.text2);

		t1.setOnClickListener(new MyOnClickListener(0));
		t2.setOnClickListener(new MyOnClickListener(1));
	}

	/**
	 * 初始化ViewPager
	 */
	private void InitViewPager() {
		mPager = (ViewPager) findViewById(R.id.vPager);
		listViews = new ArrayList<View>();
		LayoutInflater mInflater = getLayoutInflater();
		listViews.add(mInflater.inflate(R.layout.bootmgr_page1, null));
		listViews.add(mInflater.inflate(R.layout.bootmgr_page2, null));
		mPager.setAdapter(new MyPagerAdapter(listViews));
		mPager.setCurrentItem(0);
		mPager.setOnPageChangeListener(new MyOnPageChangeListener());
	}

	/**
	 * 初始化动画
	 */
	private void InitImageView() {
		cursor = (ImageView) findViewById(R.id.cursor);
		bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.cursor).getWidth();// 获取图片宽度
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;// 获取分辨率宽度
		offset = (screenW / 2 - bmpW) / 2;// 计算偏移量
		Matrix matrix = new Matrix();
		matrix.postTranslate(offset, 0);
		cursor.setImageMatrix(matrix);// 设置动画初始位置
	}
	
	
	/**
	 * fill data in list
	 */
	private void populateListData() {
		PackageManager pm = this.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED, null);
		// we can get all enabled BOOT_COMPLETED broadcast receivers here
		List<ResolveInfo> infoList = pm.queryBroadcastReceivers(intent, 0);
		
		// how can we get all BOOT_COMPLETED receivers, including the ones have been disabled?
		List<PackageInfo> allPkgList = pm.getInstalledPackages(PackageManager.GET_RECEIVERS);
		for (PackageInfo pkg : allPkgList) {
			ActivityInfo[] activities = pkg.receivers;
			if (activities == null)
				continue;
			for ( ActivityInfo a : activities)
				System.out.println(a.name + " " + a.enabled);
			
		}
		
		Iterator<ResolveInfo> it = infoList.iterator();
		
		while (it.hasNext()) {
			ResolveInfo info = it.next();
			
			Map<String, Object> map = new HashMap<String, Object>();
			Drawable icon = info.activityInfo.loadIcon(pm).getCurrent();
			
			map.put("pkg_name", info.activityInfo.packageName);
			map.put("cls_name", info.activityInfo.name); 		// package.name.receiver
			map.put("app_icon", icon);
			map.put("app_name", info.activityInfo.loadLabel(pm));
//			map.put("")
			
			if ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
				mListSystemApp.add(map);
			} else {
				mListNormalApp.add(map);
			}
			
		}	
		
	}
	
	/**
	 * ViewPager适配器
	 */
	public class MyPagerAdapter extends PagerAdapter {
		public List<View> mListViews;

		public MyPagerAdapter(List<View> mListViews) {
			this.mListViews = mListViews;
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView(mListViews.get(arg1));
		}

		@Override
		public void finishUpdate(View arg0) {
		}

		@Override
		public int getCount() {
			return mListViews.size();
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			((ViewPager) arg0).addView(mListViews.get(arg1), 0);
			
			if (arg1 == 0) { 	// normal apps
				ListView lv = (ListView) arg0.findViewById(R.id.bootmgr_page1_listview);
				lv.setAdapter(new MyAdapter(mContext, mListNormalApp));
			} else if (arg1 == 1) { // system apps
				ListView lv = (ListView) arg0.findViewById(R.id.bootmgr_page2_listview);
				lv.setAdapter(new MyAdapter(mContext, mListSystemApp));
			}
			
			return mListViews.get(arg1);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == (arg1);
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {
		}
	}

	/**
	 * 头标点击监听
	 */
	public class MyOnClickListener implements View.OnClickListener {
		private int index = 0;

		public MyOnClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			mPager.setCurrentItem(index);
		}
	};

	/**
	 * 页卡切换监听
	 */
	public class MyOnPageChangeListener implements OnPageChangeListener {

		int one = offset * 2 + bmpW;// 页卡1 -> 页卡2 偏移量

		@Override
		public void onPageSelected(int arg0) {
			Animation animation = null;
			switch (arg0) {
			case 0:
				if (currIndex == 1) {
					animation = new TranslateAnimation(one, 0, 0, 0);
				} 
				break;
			case 1:
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, one, 0, 0);
				} 
				break;
			/*
			case 2:
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, two, 0, 0);
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, two, 0, 0);
				}
				break;
				*/
			}
			currIndex = arg0;
			animation.setFillAfter(true);// True:图片停在动画结束位置
			animation.setDuration(300);
			cursor.startAnimation(animation);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}
	
	
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						//applyOrSaveRules();
					case DialogInterface.BUTTON_NEGATIVE:
						BootmgrActivity.this.finish();
						break;
					}
				}
			};

			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle(R.string.unsaved_changes)
					.setMessage(R.string.unsaved_changes_message)
					.setPositiveButton(R.string.apply, dialogClickListener)
					.setNegativeButton(R.string.discard, dialogClickListener)
					.show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}