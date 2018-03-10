package isc.whu.defender.xmonitor;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class XActivityRecognitionClient extends XHook {
	private Methods mMethod;

	private XActivityRecognitionClient(Methods method, String restrictionName) {
		super(restrictionName, method.name(), String.format("GMS.%s", method.name()));
		mMethod = method;
	}

	private XActivityRecognitionClient(Methods method, String restrictionName, int sdk) {
		super(restrictionName, method.name(), String.format("GMS.%s", method.name()), sdk);
		mMethod = method;
	}

	public String getClassName() {
		return "com.google.android.gms.location.ActivityRecognitionClient";
	}

	// @formatter:off

	// public void removeActivityUpdates(PendingIntent callbackIntent)
	// public void requestActivityUpdates(long detectionIntervalMillis, PendingIntent callbackIntent)
	// http://developer.android.com/reference/com/google/android/gms/location/ActivityRecognitionClient.html

	// @formatter:on

	private enum Methods {
		removeActivityUpdates, requestActivityUpdates
	};

	public static List<XHook> getInstances() {
		List<XHook> listHook = new ArrayList<XHook>();
		listHook.add(new XActivityRecognitionClient(Methods.removeActivityUpdates, null, 1));
		listHook.add(new XActivityRecognitionClient(Methods.requestActivityUpdates, HookManager.cLocation));
		return listHook;
	}

	@Override
	protected void before(XParam param) throws Throwable {
		if (mMethod == Methods.removeActivityUpdates) {
			if (isRestricted(param, HookManager.cLocation, "GMS.requestActivityUpdates"))
				param.setResult(null);

		} else if (mMethod == Methods.requestActivityUpdates) {
			if (isRestricted(param))
				param.setResult(null);

		} else

			Util.log(this, Log.WARN, "Unknown method=" + param.method.getName());
	}

	@Override
	protected void after(XParam param) throws Throwable {
		// Do nothing
	}
}
