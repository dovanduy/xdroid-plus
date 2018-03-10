package isc.whu.defender.scanner;

import isc.whu.defender.MainActivity;
import isc.whu.defender.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class AppNotification {
	private Context ctx;
	private NotificationManager manager;
	
	public AppNotification(Context ctx) {
		this.ctx = ctx;
	}
	
	public void setNotification(int i, String uid) {
		manager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Notification notification = new Notification();
		
		String appName = getPkgName(uid);
		if(appName != null) {
			//Set the icon
			notification.icon = R.drawable.unsafe_warning;
			notification.tickerText = appName + " may be suspecious";
			
			Intent intent = new Intent(ctx, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(ctx, i, intent, PendingIntent.FLAG_ONE_SHOT);
			notification.setLatestEventInfo(ctx, "Warning!", appName + " may be suspecious, please view the detail!", pendingIntent);
			
			manager.notify(i, notification);
			
		}
			
	}
	
	private String getPkgName(String uid) {
		PackageManager pm = ctx.getPackageManager();
		return pm.getNameForUid(Integer.parseInt(uid));
	}
}
