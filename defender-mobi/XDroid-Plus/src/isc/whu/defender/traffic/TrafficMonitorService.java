package isc.whu.defender.traffic;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.IBinder;

public class TrafficMonitorService extends Service {

	private Handler objHandler = new Handler();
	private static final String TAG = "MyService";
		
	private Runnable mTasks = new Runnable() {
		public void run()
		{
			refresh(); 
			objHandler.postDelayed(mTasks, 30000);
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		Get();
		objHandler.postDelayed(mTasks, 0);
		super.onStart(intent, startId);
	}

	private void refresh() {
		// TODO Auto-generated method stub

		Calendar c = Calendar.getInstance();

		int mHour = c.get(Calendar.HOUR_OF_DAY);
		int mMinute = c.get(Calendar.MINUTE);
		if (mHour == 12 && mMinute == 00) {
			//当时间为午夜12点时记录当前/proc/uid_stat/<uid>/(tcp_snd/tcp_rcv)的流量
			//也就是说包含前一天的流量
			Get();
		}
	}

	private void Get() {
		List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
		
		System.out.println(packageInfos.size());
		
		DBAdapter formerdb = new DBAdapter(this);
		formerdb.open();
		
		Iterator iterator = packageInfos.iterator();

		while(iterator.hasNext()){
			PackageInfo pInfo = (PackageInfo)iterator.next();
			if(pInfo.applicationInfo.uid >= 10000 &&
					PackageManager.PERMISSION_GRANTED == getPackageManager().checkPermission(Manifest.permission.INTERNET, pInfo.packageName)) {
			
				double download = getUidRcv(pInfo.applicationInfo.uid);
				double upload = getUidSnd(pInfo.applicationInfo.uid);
			
				Calendar c = Calendar.getInstance();
				int mYear = c.get(Calendar.YEAR);
				int mMonth = c.get(Calendar.MONTH) + 1;
				int mDay = c.get(Calendar.DAY_OF_MONTH);
				String mdate = mYear + "-" + mMonth + "-" + mDay;
			
				//0表示上传, 1表示下载
				if(formerdb.getTitle(pInfo.applicationInfo.uid, mdate, 0) == -1)
					formerdb.insertTitle(pInfo.applicationInfo.uid, mdate, 0, upload);
				if(formerdb.getTitle(pInfo.applicationInfo.uid, mdate, 1) == -1)
					formerdb.insertTitle(pInfo.applicationInfo.uid, mdate, 1, download);
			}
		}
		formerdb.close();

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		objHandler.removeCallbacks(mTasks);
		super.onDestroy();
	}

	private long getUidRcv(int uid) {
		String rcvFile = "/proc/uid_stat/" + uid + "/tcp_rcv";
		
		FileReader fReader = null;
		
		try {
			fReader = new FileReader(rcvFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		
		BufferedReader bReader = new BufferedReader(fReader);
		String line = null;
		try {
			line = bReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Long.parseLong(line);
	}
	
	private long getUidSnd(int uid) {
		String rcvFile = "/proc/uid_stat/" + uid + "/tcp_snd";
		
		FileReader fReader = null;
		
		try {
			fReader = new FileReader(rcvFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		
		BufferedReader bReader = new BufferedReader(fReader);
		String line = null;
		try {
			line = bReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Long.parseLong(line);
	}
}
