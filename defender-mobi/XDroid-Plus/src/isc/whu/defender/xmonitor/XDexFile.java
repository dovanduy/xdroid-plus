package isc.whu.defender.xmonitor;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.util.Log;

public class XDexFile extends XHook {
	private Methods mMethod;
	private String mActionName;

	private XDexFile(Methods method, String restrictionName, String actionName) {
		super(restrictionName, method.name(), actionName);
		mMethod = method;
		mActionName = actionName;
	}

	private XDexFile(Methods method, String restrictionName, String actionName,
			int sdk) {
		super(restrictionName, method.name(), actionName, sdk);
		mMethod = method;
		mActionName = actionName;
	}

	public String getClassName() {
		return "dalvik.system.DexClassLoader";
	}

	// @formatter:off

	// public Class loadClass(String name, ClassLoader loader)
	// static public DexFile loadDex(String sourcePathName, String
	// outputPathName, int flags) throws IOException
	// /libcore/dalvik/src/main/java/dalvik/system/DexFile.java

	// @formatter:on

	private enum Methods {
		loadClass, loadDex
	};

	@SuppressLint("InlinedApi")
	public static List<XHook> getInstances() {
		List<XHook> listHook = new ArrayList<XHook>();
		listHook.add(new XDexFile(Methods.loadClass, HookManager.cSystem, null,
				1));
		listHook.add(new XDexFile(Methods.loadDex, HookManager.cSystem, null, 1));
		return listHook;
	}

	@Override
	@SuppressLint("DefaultLocale")
	protected void before(XParam param) throws Throwable {
		if (mMethod == Methods.loadDex || mMethod == Methods.loadClass) {
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
