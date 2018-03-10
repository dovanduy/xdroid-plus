package isc.whu.defender.scanner;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;

public class PDetection extends Object {
	public int uid;
	public Drawable icon;
	public String appName;
	public boolean normal;

	// The extra is never needed in the result

	public PDetection() {
	}

	public PDetection(PDetection other) {
		uid = other.uid;
		icon = other.icon;
		appName = other.appName;
		normal = other.normal;
	}

	public PDetection(int _uid, boolean _normal) {
		uid = _uid;
		normal = _normal;
		appName = "";
		icon = null;
	}

	public PDetection(int _uid, Drawable _draw, String _name, boolean _normal) {
		uid = _uid;
		appName = _name;
		icon = _draw;
		normal = _normal;
	}

	@Override
	@SuppressLint("DefaultLocale")
	public String toString() {
		return String.format("%d/%s %s", uid, appName, normal ? "正常" : "恶意");
	}
}
