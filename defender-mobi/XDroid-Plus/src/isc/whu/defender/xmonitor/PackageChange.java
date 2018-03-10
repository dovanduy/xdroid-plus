package isc.whu.defender.xmonitor;

import isc.whu.defender.MonitorService;
import isc.whu.defender.R;
import isc.whu.defender.softmgr.SoftDetail;
import isc.whu.defender.softmgr.SoftmgrActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PackageChange extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {
		try {
			// Check uri
			Uri inputUri = intent.getData();
			if (inputUri.getScheme().equals("package")) {
				// Get data
				int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
				boolean replacing = intent.getBooleanExtra(
						Intent.EXTRA_REPLACING, false);
				NotificationManager notificationManager = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);

				Util.log(null, Log.WARN,
						"Package change action=" + intent.getAction()
								+ " replacing=" + replacing + " uid=" + uid);

				// Check action
				if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
					// Get data
					ApplicationInfoEx appInfo = new ApplicationInfoEx(context,
							uid);
					String packageName = inputUri.getSchemeSpecificPart();

					// Default deny new user apps
					if (MonitorService.getClient() != null
							&& appInfo.getPackageName().size() == 1) {
						if (!replacing) {
							// Delete existing restrictions
							HookManager.deleteRestrictions(uid, null, false);
							HookManager.deleteSettings(uid);
							HookManager.deleteUsage(uid);

							boolean ondemand = HookManager.getSettingBool(0,
									HookManager.cSettingOnDemand, true, false);

							// Enable on demand
							if (ondemand)
								HookManager.setSetting(uid,
										HookManager.cSettingOnDemand,
										Boolean.toString(true));

							// Restrict new non-system apps
							if (!appInfo.isSystem())
								HookManager.applyTemplate(uid, null, true);
						}
					}

					// Mark as new/changed
					HookManager.setSetting(uid, HookManager.cSettingState,
							Integer.toString(SoftmgrActivity.STATE_ATTENTION));

					// New/update notification
					boolean notify = HookManager.getSettingBool(uid,
							HookManager.cSettingNotify, true, false);
					if (!replacing || notify) {
						Intent resultIntent = new Intent(context,
								SoftDetail.class);
						resultIntent.putExtra(SoftDetail.cUid, uid);

						// Build pending intent
						PendingIntent pendingIntent = PendingIntent
								.getActivity(context, uid, resultIntent,
										PendingIntent.FLAG_UPDATE_CURRENT);

						// Build result intent settings
						Intent resultIntentSettings = new Intent(context,
								SoftDetail.class);
						resultIntentSettings.putExtra(SoftDetail.cUid, uid);
						resultIntentSettings.putExtra(SoftDetail.cAction,
								SoftDetail.cActionSettings);

						// Build pending intent settings
						PendingIntent pendingIntentSettings = PendingIntent
								.getActivity(context, uid - 10000,
										resultIntentSettings,
										PendingIntent.FLAG_UPDATE_CURRENT);

						// Build result intent clear
						Intent resultIntentClear = new Intent(context,
								SoftDetail.class);
						resultIntentClear.putExtra(SoftDetail.cUid, uid);
						resultIntentClear.putExtra(SoftDetail.cAction,
								SoftDetail.cActionClear);

						// Build pending intent clear
						PendingIntent pendingIntentClear = PendingIntent
								.getActivity(context, uid + 10000,
										resultIntentClear,
										PendingIntent.FLAG_UPDATE_CURRENT);

						// Title
						String title = String.format("%s %s %s", context
								.getString(replacing ? R.string.msg_update
										: R.string.msg_new), appInfo
								.getApplicationName(packageName), appInfo
								.getPackageVersionName(context, packageName));
						if (!replacing)
							title = String.format("%s %s", title,
									context.getString(R.string.msg_applied));

						// Build notification
						NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
								context);
						notificationBuilder
								.setSmallIcon(R.drawable.ic_launcher);
						notificationBuilder.setContentTitle(context
								.getString(R.string.app_name));
						notificationBuilder.setContentText(title);
						notificationBuilder.setContentIntent(pendingIntent);
						notificationBuilder.setWhen(System.currentTimeMillis());
						notificationBuilder.setAutoCancel(true);

						// Actions
						notificationBuilder.addAction(
								android.R.drawable.ic_menu_edit,
								context.getString(R.string.menu_app_settings),
								pendingIntentSettings);
						notificationBuilder.addAction(
								android.R.drawable.ic_menu_delete,
								context.getString(R.string.menu_clear),
								pendingIntentClear);

						// Notify
						Notification notification = notificationBuilder.build();
						notificationManager.notify(appInfo.getUid(),
								notification);
					}

				} else if (intent.getAction().equals(
						Intent.ACTION_PACKAGE_REMOVED)
						&& !replacing) {
					// Package removed
					notificationManager.cancel(uid);

					// Delete restrictions
					ApplicationInfoEx appInfo = new ApplicationInfoEx(context,
							uid);
					if (MonitorService.getClient() != null
							&& appInfo.getPackageName().size() == 0) {
						HookManager.deleteRestrictions(uid, null, false);
						HookManager.deleteSettings(uid);
						HookManager.deleteUsage(uid);
					}

				} else if (intent.getAction().equals(
						Intent.ACTION_PACKAGE_REPLACED)) {
					// Notify reboot required
					String packageName = inputUri.getSchemeSpecificPart();
					if (packageName.equals(context.getPackageName())) {
						// Start package update
						Intent changeIntent = new Intent();
						changeIntent.setClass(context, UpdateService.class);
						changeIntent.putExtra(UpdateService.cAction,
								UpdateService.cActionUpdated);
						context.startService(changeIntent);

						// Build notification
						NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
								context);
						notificationBuilder
								.setSmallIcon(R.drawable.ic_launcher);
						notificationBuilder.setContentTitle(context
								.getString(R.string.app_name));
						notificationBuilder.setContentText(context
								.getString(R.string.msg_reboot));
						notificationBuilder.setWhen(System.currentTimeMillis());
						notificationBuilder.setAutoCancel(true);
						Notification notification = notificationBuilder.build();

						// Notify
						notificationManager.notify(Util.NOTIFY_RESTART,
								notification);
					}
				}
			}
		} catch (Throwable ex) {
			Util.bug(null, ex);
		}
	}
}
