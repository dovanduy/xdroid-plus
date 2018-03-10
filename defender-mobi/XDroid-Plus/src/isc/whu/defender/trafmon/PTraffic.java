package isc.whu.defender.trafmon;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
public class PTraffic extends Object {
	public int uid;
	public Drawable icon;
	public String appName;
	public double upTraffic;
	public double upLink;

	// The extra is never needed in the result

	public PTraffic() {
	}

	public PTraffic(PTraffic other) {
		uid = other.uid;
		icon = other.icon;
		appName = other.appName;
		upTraffic = other.upTraffic;
		upLink = other.upLink;
	}

	public PTraffic(int _uid, double _upTraffic, double _upLink) {
		uid = _uid;
		upTraffic = _upTraffic;
		upLink = _upLink;
		appName = "";
		icon = null;
	}

	public PTraffic(int _uid, Drawable _draw, String _name, double _upTraffic,
			double _upLink) {
		uid = _uid;
		upTraffic = _upTraffic;
		upLink = _upLink;
		appName = _name;
		icon = _draw;
	}

	@Override
	@SuppressLint("DefaultLocale")
	public String toString() {
		return String.format("%d/%s=%0.2f/%0.2f", uid, appName, upTraffic,
				upLink);
	}
}
