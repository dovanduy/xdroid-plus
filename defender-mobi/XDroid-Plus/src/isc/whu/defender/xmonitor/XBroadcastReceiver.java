package isc.whu.defender.xmonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class XBroadcastReceiver extends XHook {
	private Methods mMethod;
	private String mActionName;

	private XBroadcastReceiver(Methods method, String restrictionName,
			String actionName) {
		super(restrictionName, method.name(), actionName);
		mMethod = method;
		mActionName = actionName;
	}

	private XBroadcastReceiver(Methods method, String restrictionName,
			String actionName, int sdk) {
		super(restrictionName, method.name(), actionName, sdk);
		mMethod = method;
		mActionName = actionName;
	}

	public String getClassName() {
		return "android.content.BroadcastReceiver";
	}

	// @formatter:off

	// public final void abortBroadcast()
	// frameworks/base/core/java/android/content/BroadcastReceiver.java

	// @formatter:on

	private enum Methods {
		abortBroadcast
	};

	@SuppressLint("InlinedApi")
	public static List<XHook> getInstances() {
		List<XHook> listHook = new ArrayList<XHook>();
		listHook.add(new XBroadcastReceiver(Methods.abortBroadcast,
				HookManager.cReadMessage, null, 1));

		return listHook;
	}

	@Override
	@SuppressLint("DefaultLocale")
	protected void before(XParam param) throws Throwable {
		if (mMethod == Methods.abortBroadcast) {
			boolean restricted = isRestricted(param);
			if (restricted) {
				if (mMethod == Methods.abortBroadcast)
					param.setResult(true);
				else
					param.setResult(null);
				return;
			}
		} else
			Util.log(this, Log.WARN, "Unknown method=" + param.method.getName());
	}

	@Override
	protected void after(XParam param) throws Throwable {
		// Do nothing
	}
}
