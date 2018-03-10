package isc.whu.defender.xmonitor;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.util.Log;

public class XClassLoader extends XHook {
	private Methods mMethod;
	private String mActionName;

	private XClassLoader(Methods method, String restrictionName,
			String actionName) {
		super(restrictionName, method.name(), actionName);
		mMethod = method;
		mActionName = actionName;
	}

	private XClassLoader(Methods method, String restrictionName,
			String actionName, int sdk) {
		super(restrictionName, method.name(), actionName, sdk);
		mMethod = method;
		mActionName = actionName;
	}

	public String getClassName() {
		return "java.lang.ClassLoader";
	}

	// @formatter:off

	// public Class<?> loadClass(String className) throws ClassNotFoundException
	// /libcore/luni/src/main/java/java/lang/ClassLoader.java

	// @formatter:on

	private enum Methods {
		loadClass
	};

	@SuppressLint("InlinedApi")
	public static List<XHook> getInstances() {
		List<XHook> listHook = new ArrayList<XHook>();
		listHook.add(new XClassLoader(Methods.loadClass, HookManager.cSystem,
				null, 1));
		return listHook;
	}

	@Override
	@SuppressLint("DefaultLocale")
	protected void before(XParam param) throws Throwable {
		if (mMethod == Methods.loadClass) {
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
