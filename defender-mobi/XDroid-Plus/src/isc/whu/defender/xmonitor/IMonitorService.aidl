package isc.whu.defender.xmonitor;

import isc.whu.defender.xmonitor.PRestriction;
import isc.whu.defender.xmonitor.PSetting;

interface IMonitorService {
	int getVersion();
	List /* String */ check();
	void reportError(String message);

	void setRestriction(in PRestriction restriction);
	void setRestrictionList(in List<PRestriction> listRestriction);
	PRestriction getRestriction(in PRestriction restriction, boolean usage, String secret);
	List<PRestriction> getRestrictionList(in PRestriction selector);
	void deleteRestrictions(int uid, String restrictionName);

	long getUsage(in List<PRestriction> restriction);
	List<PRestriction> getUsageList(int uid, String restrictionName);
	void deleteUsage(int uid);

	void setSetting(in PSetting setting);
	void setSettingList(in List<PSetting> listSetting);
	PSetting getSetting(in PSetting setting);
	List<PSetting> getSettingList(int uid);
	void deleteSettings(int uid);

	void clear();
	void dump(int uid);
}