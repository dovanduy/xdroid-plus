package isc.whu.defender;

import java.util.LinkedList;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public final class NotificationUtil {
	private static Context sContext = null;
	private static NotificationManager sNotificationManager;

	public static final int NOTIFICATION_MODULE_NOT_ACTIVATED_YET = 0;
	public static final int NOTIFICATION_MODULES_UPDATED = 1;

	private static final int PENDING_INTENT_OPEN_MODULES = 0;
	private static final int PENDING_INTENT_OPEN_INSTALL = 1;
	private static final int PENDING_INTENT_SOFT_REBOOT = 2;
	private static final int PENDING_INTENT_REBOOT = 3;
	private static final int PENDING_INTENT_ACTIVATE_MODULE_AND_REBOOT = 4;

	public static void init() {
		if (sContext != null)
			throw new IllegalStateException("NotificationUtil has already been initialized");

		sContext = XDroidDefender.getInstance();
		sNotificationManager = (NotificationManager) sContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public static void cancel(int id) {
		sNotificationManager.cancel(id);
	}

	public static void cancelAll() {
		sNotificationManager.cancelAll();
	}
}
