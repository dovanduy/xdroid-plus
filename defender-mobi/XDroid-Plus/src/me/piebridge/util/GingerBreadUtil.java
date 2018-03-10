package me.piebridge.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;

public class GingerBreadUtil {

	private static Field scanField(Class<?> clazz, String... names) {
		for (String name : names) {
			Field field;
			try {
				field = clazz.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException e) {
			}
			try {
				field = clazz.getField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException e) {
			}
		}
		return null;
	}

	public static void recreate(Activity activity) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
			recreateHC(activity);
		} else {
			try {
				recreateGB(activity);
			} catch (InvocationTargetException e) {
				e.getTargetException().printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static void recreateHC(Activity activity) {
		((Activity) activity).recreate();
	}

	private static void recreateGB(Activity activity) throws IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Field Activity$mToken = scanField(Activity.class, "mToken");
		IBinder mToken = (IBinder) Activity$mToken.get(activity);
		Field Activity$mMainThread = scanField(Activity.class, "mMainThread");
		Object mMainThread = Activity$mMainThread.get(activity);
		Field ActivityThread$mAppThread = scanField(mMainThread.getClass(), "mAppThread");
		Object mAppThread = ActivityThread$mAppThread.get(mMainThread);
		Method method = mAppThread.getClass().getMethod("scheduleRelaunchActivity",
			IBinder.class, List.class, List.class, int.class, boolean.class, Configuration.class);
		method.invoke(mAppThread, mToken, null, null, 0, false, null);
	}

}
