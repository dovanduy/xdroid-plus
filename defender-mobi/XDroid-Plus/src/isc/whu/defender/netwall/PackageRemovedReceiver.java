package isc.whu.defender.netwall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PackageRemovedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
			Log.v("System.out", "Broadcast: package_removed");
			boolean replace = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
			if(!replace) {
				final int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
				if(uid != 0) {
					NetwallApi.applicationRemoved(context, uid);
					NetwallApi.apps = null;
				}
			}
		} else if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
			Log.v("System.out", "Broadcast: package_added");
			NetwallApi.apps = null;
		}
	}
}
