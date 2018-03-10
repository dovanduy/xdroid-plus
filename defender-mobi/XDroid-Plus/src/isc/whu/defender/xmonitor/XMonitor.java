package isc.whu.defender.xmonitor;

import isc.whu.defender.MonitorService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;
import static de.robv.android.xposed.XposedHelpers.findClass;

@SuppressLint("DefaultLocale")
public class XMonitor implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	private static String mSecret = null;

	private static boolean mAccountManagerHooked = false;
	private static boolean mActivityManagerHooked = false;
	private static boolean mClipboardManagerHooked = false;
	private static boolean mConnectivityManagerHooked = false;
	private static boolean mLocationManagerHooked = false;
	private static boolean mPackageManagerHooked = false;
	private static boolean mSensorManagerHooked = false;
	private static boolean mTelephonyManagerHooked = false;
	private static boolean mWindowManagerHooked = false;
	private static boolean mWiFiManagerHooked = false;

	private static List<String> mListHookError = new ArrayList<String>();
	public static final ClassLoader BOOTCLASSLOADER = ClassLoader
			.getSystemClassLoader();

	@SuppressLint("InlinedApi")
	public void initZygote(StartupParam startupParam) throws Throwable {
		Util.log(null, Log.WARN,
				String.format("Load %s", startupParam.modulePath));

		// Check for LBE security master
		if (Util.hasLBE()) {
			Util.log(null, Log.ERROR, "LBE installed");
			return;
		}

		// Generate secret
		mSecret = Long.toHexString(new Random().nextLong());

		// System server
		try {
			// frameworks/base/services/java/com/android/server/SystemServer.java
			Class<?> cSystemServer = Class
					.forName("com.android.server.SystemServer");
			Method mMain = cSystemServer.getDeclaredMethod("main",
					String[].class);
			XposedBridge.hookMethod(mMain, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					MonitorService.register(mListHookError, mSecret);
				}
			});
		} catch (Throwable ex) {
			Util.bug(null, ex);
		}

		// Activity manager service
		hookAll(XActivityManagerService.getInstances(), mSecret);

		// Audio record
		hookAll(XAudioRecord.getInstances(), mSecret);

		// Camera
		hookAll(XCamera.getInstances(), mSecret);

		// Content resolver
		hookAll(XContentResolver.getInstances(), mSecret);

		// InetAddress
		hookAll(XInetAddress.getInstances(), mSecret);

		// Media recorder
		hookAll(XMediaRecorder.getInstances(), mSecret);

		// Network info
		hookAll(XNetworkInfo.getInstances(), mSecret);

		// Network interface
		hookAll(XNetworkInterface.getInstances(), mSecret);

		// SMS manager
		hookAll(XSmsManager.getInstances(), mSecret);

		// Intent receive
		hookAll(XActivityThread.getInstances(), mSecret);

		// Intent send
		hookAll(XActivity.getInstances(), mSecret);

		// Broadcast Receiver
		hookAll(XBroadcastReceiver.getInstances(), mSecret);

		// Dex Loader
		// hookAll(XDexFile.getInstances(), mSecret);

		// ClassLoader
		// hookAll(XClassLoader.getInstances(), mSecret);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		// Check for LBE security master
		if (Util.hasLBE())
			return;
		// Log load
		Util.log(null, Log.INFO, String.format("Load package=%s uid=%d",
				lpparam.packageName, Process.myUid()));

		// Skip hooking self
		String self = XMonitor.class.getPackage().getName();
		if (lpparam.packageName.equals(self)) {
			hookAll(XUtilHook.getInstances(), lpparam.classLoader, mSecret);
			return;
		}

		// Build SERIAL
		if (HookManager.getRestriction(null, Process.myUid(),
				HookManager.cIdentification, "SERIAL", mSecret))
			XposedHelpers.setStaticObjectField(Build.class, "SERIAL",
					HookManager.getDefacedProp(Process.myUid(), "SERIAL"));

		// Advertising Id
		try {
			Class.forName(
					"com.google.android.gms.ads.identifier.AdvertisingIdClient$Info",
					false, lpparam.classLoader);
			hookAll(XAdvertisingIdClientInfo.getInstances(),
					lpparam.classLoader, mSecret);
		} catch (Throwable ignored) {
		}

		// User activity
		try {
			Class.forName(
					"com.google.android.gms.location.ActivityRecognitionClient",
					false, lpparam.classLoader);
			hookAll(XActivityRecognitionClient.getInstances(),
					lpparam.classLoader, mSecret);
		} catch (Throwable ignored) {
		}

		// Google auth
		try {
			Class.forName("com.google.android.gms.auth.GoogleAuthUtil", false,
					lpparam.classLoader);
			hookAll(XGoogleAuthUtil.getInstances(), lpparam.classLoader,
					mSecret);
		} catch (Throwable ignored) {
		}

		// Location client
		try {
			Class.forName("com.google.android.gms.location.LocationClient",
					false, lpparam.classLoader);
			hookAll(XLocationClient.getInstances(), lpparam.classLoader,
					mSecret);
		} catch (Throwable ignored) {
		}
	}

	public static void handleGetSystemService(XHook hook, String name,
			Object instance) {
		Util.log(
				hook,
				Log.INFO,
				"getSystemService " + name + "="
						+ instance.getClass().getName() + " uid="
						+ Binder.getCallingUid());

		if ("android.telephony.MSimTelephonyManager".equals(instance.getClass()
				.getName())) {
			Util.log(hook, Log.WARN, "Telephone service="
					+ Context.TELEPHONY_SERVICE);
			Class<?> clazz = instance.getClass();
			while (clazz != null) {
				Util.log(hook, Log.WARN, "Class " + clazz);
				for (Method method : clazz.getDeclaredMethods())
					Util.log(hook, Log.WARN, "Declared " + method);
				clazz = clazz.getSuperclass();
			}
		}

		if (name.equals(Context.ACCOUNT_SERVICE)) {
			// Account manager
			if (!mAccountManagerHooked) {
				hookAll(XAccountManager.getInstances(instance), mSecret);
				mAccountManagerHooked = true;
			}
		} else if (name.equals(Context.ACTIVITY_SERVICE)) {
			// Activity manager
			if (!mActivityManagerHooked) {
				hookAll(XActivityManager.getInstances(instance), mSecret);
				mActivityManagerHooked = true;
			}
		} else if (name.equals(Context.CLIPBOARD_SERVICE)) {
			// Clipboard manager
			if (!mClipboardManagerHooked) {
				XMonitor.hookAll(XClipboardManager.getInstances(instance),
						mSecret);
				mClipboardManagerHooked = true;
			}
		} else if (name.equals(Context.CONNECTIVITY_SERVICE)) {
			// Connectivity manager
			if (!mConnectivityManagerHooked) {
				hookAll(XConnectivityManager.getInstances(instance), mSecret);
				mConnectivityManagerHooked = true;
			}
		} else if (name.equals(Context.LOCATION_SERVICE)) {
			// Location manager
			if (!mLocationManagerHooked) {
				hookAll(XLocationManager.getInstances(instance), mSecret);
				mLocationManagerHooked = true;
			}
		} else if (name.equals("PackageManager")) {
			// Package manager
			if (!mPackageManagerHooked) {
				hookAll(XPackageManager.getInstances(instance), mSecret);
				mPackageManagerHooked = true;
			}
		} else if (name.equals(Context.SENSOR_SERVICE)) {
			// Sensor manager
			if (!mSensorManagerHooked) {
				hookAll(XSensorManager.getInstances(instance), mSecret);
				mSensorManagerHooked = true;
			}
		} else if (name.equals(Context.TELEPHONY_SERVICE)) {
			// Telephony manager
			if (!mTelephonyManagerHooked) {
				hookAll(XTelephonyManager.getInstances(instance), mSecret);
				mTelephonyManagerHooked = true;
			}
		} else if (name.equals(Context.WINDOW_SERVICE)) {
			// Window manager
			if (!mWindowManagerHooked) {
				XMonitor.hookAll(XWindowManager.getInstances(instance), mSecret);
				mWindowManagerHooked = true;
			}
		} else if (name.equals(Context.WIFI_SERVICE)) {
			// WiFi manager
			if (!mWiFiManagerHooked) {
				XMonitor.hookAll(XWifiManager.getInstances(instance), mSecret);
				mWiFiManagerHooked = true;
			}
		}
	}

	public static void hookAll(List<XHook> listHook, String secret) {
		for (XHook hook : listHook)
			hook(hook, secret);
	}

	public static void hookAll(List<XHook> listHook, ClassLoader classLoader,
			String secret) {
		for (XHook hook : listHook)
			hook(hook, classLoader, secret);
	}

	private static void hook(XHook hook, String secret) {
		hook(hook, null, secret);
	}

	private static void hook(final XHook hook, ClassLoader classLoader,
			String secret) {
		// Check SDK version
		Util.log(null, Log.INFO,
				"hook(" + hook.getClassName() + "." + hook.getMethodName()
						+ ")");
		Hook md = null;
		String message = null;
		if (hook.getRestrictionName() == null) {
			if (hook.getSdk() == 0)
				message = "No SDK specified for " + hook;
		} else {
			md = HookManager.getHook(hook.getRestrictionName(),
					hook.getSpecifier());
			if (md == null)
				message = "Hook not found " + hook;
			else if (hook.getSdk() != 0)
				message = "SDK not expected for " + hook;
		}
		if (message != null) {
			mListHookError.add(message);
			Util.log(hook, Log.ERROR, message);
		}

		int sdk = 0;
		if (hook.getRestrictionName() == null)
			sdk = hook.getSdk();
		else if (md != null)
			sdk = md.getSdk();

		if (Build.VERSION.SDK_INT < sdk)
			return;

		// Provide secret
		hook.setSecret(secret);

		try {
			// Create hook method
			XC_MethodHook methodHook = new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					try {
						if (Process.myUid() <= 0)
							return;
						XParam xparam = XParam.fromXposed(param);
						hook.before(xparam);
						if (xparam.hasResult())
							param.setResult(xparam.getResult());
						if (xparam.hasThrowable())
							param.setThrowable(xparam.getThrowable());
						param.setObjectExtra("xextra", xparam.getExtras());
					} catch (Throwable ex) {
						Util.bug(null, ex);
					}
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					Util.log(null, Log.INFO, "afterHookedMethod");
					if (!param.hasThrowable())
						try {
							if (Process.myUid() <= 0)
								return;
							XParam xparam = XParam.fromXposed(param);
							xparam.setExtras(param.getObjectExtra("xextra"));
							Util.log(null, Log.INFO,
									"afterHookedMethod: after()");
							hook.after(xparam);
							Util.log(null, Log.INFO,
									"afterHookedMethod: after() complete");
							if (xparam.hasResult())
								param.setResult(xparam.getResult());
							if (xparam.hasThrowable())
								param.setThrowable(xparam.getThrowable());
						} catch (Throwable ex) {
							Util.bug(null, ex);
						}
				}
			};
			// Find class
			Class<?> hookClass = null;
			try {
				// hookClass = Class.forName(hook.getClassName());
				hookClass = findClass(hook.getClassName(), classLoader);
				// hookClass = Class.forName(
				// Util.toCanonicalName(hook.getClassName()), false,
				// classLoader);
			} catch (Throwable ex) {
				message = String.format("Class not found for %s", hook);
				mListHookError.add(message);
				Util.log(hook, hook.isOptional() ? Log.WARN : Log.ERROR,
						message);
			}

			// Get members
			List<Member> listMember = new ArrayList<Member>();
			Class<?> clazz = hookClass;
			while (clazz != null) {
				if (hook.getMethodName() == null) {
					for (Constructor<?> constructor : clazz
							.getDeclaredConstructors())
						if (Modifier.isPublic(constructor.getModifiers()) ? hook
								.isVisible() : !hook.isVisible())
							listMember.add(constructor);
					break;
				} else {
					for (Method method : clazz.getDeclaredMethods())
						if (method.getName().equals(hook.getMethodName())
								&& (Modifier.isPublic(method.getModifiers()) ? hook
										.isVisible() : !hook.isVisible()))
							listMember.add(method);
				}
				clazz = clazz.getSuperclass();
			}

			// Hook members
			for (Member member : listMember)
				try {
					if (Modifier.isAbstract(member.getModifiers()))
						Util.log(hook, Log.ERROR,
								String.format("Abstract: %s", member));
					else
						XposedBridge.hookMethod(member, methodHook);
				} catch (NoSuchFieldError ex) {
					Util.bug(hook, ex);
				} catch (Throwable ex) {
					mListHookError.add(ex.toString());
					Util.bug(hook, ex);
				}

			// Check if members found
			if (listMember.isEmpty()
					&& !hook.getClassName()
							.startsWith("com.google.android.gms")) {
				message = String.format("Method not found for %s", hook);
				if (!hook.isOptional())
					mListHookError.add(message);
				Util.log(hook, hook.isOptional() ? Log.WARN : Log.ERROR,
						message);
			}
		} catch (Throwable ex) {
			mListHookError.add(ex.toString());
			Util.bug(hook, ex);
		}
	}

	// WORKAROUND: when a native lib is loaded after hooking, the hook is undone

	private static List<XC_MethodHook.Unhook> mUnhookNativeMethod = new ArrayList<XC_MethodHook.Unhook>();

	@SuppressWarnings("unused")
	private static void registerNativeMethod(final XHook hook, Method method,
			XC_MethodHook.Unhook unhook) {
		if (Process.myUid() > 0) {
			synchronized (mUnhookNativeMethod) {
				mUnhookNativeMethod.add(unhook);
				Util.log(hook, Log.INFO,
						"Native " + method + " uid=" + Process.myUid());
			}
		}
	}

	@SuppressWarnings("unused")
	private static void hookCheckNative() {
		try {
			XC_MethodHook hook = new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					if (Process.myUid() > 0)
						try {
							synchronized (mUnhookNativeMethod) {
								Util.log(null, Log.INFO,
										"Loading " + param.args[0] + " uid="
												+ Process.myUid() + " count="
												+ mUnhookNativeMethod.size());
								for (XC_MethodHook.Unhook unhook : mUnhookNativeMethod) {
									XposedBridge.hookMethod(
											unhook.getHookedMethod(),
											unhook.getCallback());
									unhook.unhook();
								}
							}
						} catch (Throwable ex) {
							Util.bug(null, ex);
						}
				}
			};

			Class<?> runtimeClass = Class.forName("java.lang.Runtime");
			for (Method method : runtimeClass.getDeclaredMethods())
				if (method.getName().equals("load")
						|| method.getName().equals("loadLibrary")) {
					XposedBridge.hookMethod(method, hook);
					Util.log(null, Log.WARN, "Hooked " + method);
				}
		} catch (Throwable ex) {
			Util.bug(null, ex);
		}
	}
}