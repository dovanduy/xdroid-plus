package isc.whu.defender;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			System.out.println("Boot completed...");
			
			if (new File(XDroidDefender.BASE_DIR + "bin/sniffer").exists()) {
				RootUtil mRootUtil = new RootUtil();
				if (mRootUtil.startShell()) {
					mRootUtil.execute(XDroidDefender.BASE_DIR + "bin/sniffer", null);
				}
			}
		}

	}

}
