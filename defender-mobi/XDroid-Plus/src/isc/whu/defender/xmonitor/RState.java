package isc.whu.defender.xmonitor;

import isc.whu.defender.softmgr.SoftmgrActivity;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class RState {
	public int mUid;
	public String mRestrictionName;
	public String mMethodName;
	public boolean restricted;
	public boolean asked = false;
	public boolean partialRestricted = false;
	public boolean partialAsk = false;

	public RState(int uid, String restrictionName, String methodName) {
		mUid = uid;
		mRestrictionName = restrictionName;
		mMethodName = methodName;

		// Get if on demand
		boolean onDemand = HookManager.getSettingBool(0,
				HookManager.cSettingOnDemand, true, false);
		if (onDemand)
			onDemand = HookManager.getSettingBool(-uid,
					HookManager.cSettingOnDemand, false, false);

		boolean allRestricted = true;
		boolean someRestricted = false;
		boolean allAsk = true;
		boolean someAsk = false;

		if (methodName == null) {
			if (restrictionName == null) {
				// Examine the category state
				asked = true;
				for (String rRestrictionName : HookManager.getRestrictions()) {
					PRestriction query = HookManager.getRestrictionEx(uid,
							rRestrictionName, null);
					allRestricted = (allRestricted && query.restricted);
					someRestricted = (someRestricted || query.restricted);
					if (!query.asked)
						asked = false;
				}
			} else {
				// Examine the category/method states
				PRestriction query = HookManager.getRestrictionEx(uid,
						restrictionName, null);
				someRestricted = query.restricted;
				someAsk = !query.asked;
				for (PRestriction restriction : HookManager.getRestrictionList(
						uid, restrictionName)) {
					allRestricted = (allRestricted && restriction.restricted);
					someRestricted = (someRestricted || restriction.restricted);
					allAsk = (allAsk && !restriction.asked);
					someAsk = (someAsk || !restriction.asked);
				}
				asked = query.asked;
			}
		} else {
			// Examine the method state
			PRestriction query = HookManager.getRestrictionEx(uid,
					restrictionName, methodName);
			allRestricted = query.restricted;
			someRestricted = false;
			asked = query.asked;
		}

		// restricted = (allRestricted || someRestricted);
		restricted = someRestricted;
		asked = (!onDemand || !HookManager.isApplication(uid) || asked);
		partialRestricted = (!allRestricted && someRestricted);
		partialAsk = (onDemand && HookManager.isApplication(uid) && !allAsk && someAsk);
	}

	public void toggleRestriction() {
		if (mMethodName == null) {
			// Get restrictions to change
			List<String> listRestriction;
			if (mRestrictionName == null)
				listRestriction = HookManager.getRestrictions();
			else {
				listRestriction = new ArrayList<String>();
				listRestriction.add(mRestrictionName);
			}

			// Change restriction
			if (restricted)
				HookManager.deleteRestrictions(mUid, mRestrictionName,
						(mRestrictionName == null));
			else {
				for (String restrictionName : listRestriction)
					HookManager.setRestriction(mUid, restrictionName, null,
							true, false);
				HookManager.updateState(mUid);
			}
		} else {
			PRestriction query = HookManager.getRestrictionEx(mUid,
					mRestrictionName, null);
			HookManager.setRestriction(mUid, mRestrictionName, mMethodName,
					!restricted, query.asked);
			HookManager.updateState(mUid);
		}
	}

	public void toggleAsked() {
		asked = !asked;
		if (mRestrictionName == null)
			HookManager.setSetting(mUid, HookManager.cSettingOnDemand,
					Boolean.toString(!asked));
		else {
			// Avoid re-doing all exceptions for dangerous functions
			List<PRestriction> listPRestriction = new ArrayList<PRestriction>();
			listPRestriction.add(new PRestriction(mUid, mRestrictionName,
					mMethodName, restricted, asked));
			HookManager.setRestrictionList(listPRestriction);
			HookManager.setSetting(mUid, HookManager.cSettingState,
					Integer.toString(SoftmgrActivity.STATE_CHANGED));
			HookManager.setSetting(mUid, HookManager.cSettingModifyTime,
					Long.toString(System.currentTimeMillis()));
		}
	}
}
