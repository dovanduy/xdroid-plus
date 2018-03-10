package isc.whu.defender.bootmgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

/**
 * Read broadcast receivers of installed packages
 * 
 * The former implementation is using PackageManager.queryBroadcastReceivers(), but 
 * this method cannot retrieve receiver components that have been disabled.
 * 
 * It's relatively simple to parse the AndroidManifest.xml
 * files of every package ourselves and extract the data we need.
 *
 * Parts of this were adapted from autostarts:
 *      https://github.com/miracle2k/android-autostarts
 * 
 * @author shaoyuru@whu.edu.cn
 * 
 */
public class ReceiverReader {
	
	private static final String TAG = "ReceiverReader";
	private static final String SDK_XML_NS = 
		"http://schemas.android.com/apk/res/android";
	
	private static enum ParserState { Unknown, InMainfest,
		InApplication, InReceiver, InIntentFilter, InAction	}
	
	private final Context mCtx;
	private final PackageManager mPkgMgr;

	private XmlResourceParser mCurrentXML;
	private Resources mCurrentResources;
	private ArrayList<ComponentInfoLite> mResult;
	
	// Parse state flags
	PackageInfo mPkgInfo = null;
	String mCurrentAppLabel = null;
	ParserState mCurrentState = ParserState.Unknown;
	
	/**
	 * Constructor
	 * @param ctx Application context object
	 */
	public ReceiverReader(Context ctx) {
		this.mCtx = ctx;
		mPkgMgr = mCtx.getPackageManager();
	}
	
	public ArrayList<ComponentInfoLite> load() {
		mResult = new ArrayList<ComponentInfoLite>();
		List<PackageInfo> pkgs = mPkgMgr.getInstalledPackages(PackageManager.GET_DISABLED_COMPONENTS);
		Iterator<PackageInfo> it = pkgs.iterator();
		
		while (it.hasNext()) {
			PackageInfo pkgInfo = it.next();
			Log.v(TAG, "Processing package " + pkgInfo.packageName);
			parsePackage(pkgInfo);
		}
		
		return mResult;
	}
	
	private void parsePackage(PackageInfo pkgInfo) {
		XmlResourceParser xml = null;
		Resources resources = null;
		try {
			AssetManager assetMgr = mCtx.createPackageContext(pkgInfo.packageName, 0).getAssets();
			xml = assetMgr.openXmlResourceParser("AndroidManifest.xml");
			resources = new Resources(assetMgr, mCtx.getResources().getDisplayMetrics(), null);
			
		} catch (IOException e) {
			Log.e(TAG, "unable to open manifest for resources for " + pkgInfo.packageName);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "unable to open manifest for resources for " + pkgInfo.packageName);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		
		if (xml == null)
			return;
		
	}
	
}

class ComponentInfoLite {
	private ComponentName cName;
	private boolean enabled;
	
	public ComponentInfoLite(ComponentName name, boolean enabled) {
		this.cName = name;
		this.enabled = enabled;
	}
}