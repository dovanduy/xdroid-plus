package isc.whu.defender.policymgr;

import isc.whu.defender.R;

import java.io.File;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PolicyActivity extends PreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener {

	private String policySwitchKey;
	private String pwdQualityKey;
	private String pwdMinLengthKey;
	private String resetPwdKey;
	private String maxPwdAttempsKey;
	private String remoteCmdKey;
	
	private CheckBoxPreference switchChkbox;
	private ListPreference pwdQualityList;
	private EditTextPreference pwdMinLengthEt;
	private EditTextPreference resetPwdEt;
	private EditTextPreference maxPwdAttemptsEt;
	private EditTextPreference remoteCmdEt;
	
	private DevicePolicyManager mDPM;
	private ComponentName mAdminActivity;
	
	private final int RESULT_ENABLE = 1;
	
    final static int mPasswordQualityValues[] = new int[] {
    	DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED,
    	DevicePolicyManager.PASSWORD_QUALITY_SOMETHING,
    	DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
    	DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC,
    	DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.dev_admin_settings);
		setContentView(R.layout.policymgr_main);

		// getPreferenceManager().setSharedPreferencesName("policy");
//        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);        
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminActivity = new ComponentName(PolicyActivity.this, AdminDeviceReceiver.class);

        // get key strings
		policySwitchKey = getResources().getString(R.string.policy_switch_key);
		pwdQualityKey = getResources().getString(R.string.pwd_quality_key);
		pwdMinLengthKey = getResources().getString(R.string.pwd_min_length_key);
		resetPwdKey = getResources().getString(R.string.reset_pwd_key);
		maxPwdAttempsKey = getResources().getString(R.string.max_pwd_attempts_key);
		remoteCmdKey = getResources().getString(R.string.remote_cmd_key);
		
		// get prefs
		switchChkbox = (CheckBoxPreference) findPreference(policySwitchKey);
		pwdQualityList = (ListPreference) findPreference(pwdQualityKey);
		pwdMinLengthEt = (EditTextPreference) findPreference(pwdMinLengthKey);
		resetPwdEt = (EditTextPreference) findPreference(resetPwdKey);
		maxPwdAttemptsEt = (EditTextPreference) findPreference(maxPwdAttempsKey);
		remoteCmdEt = (EditTextPreference) findPreference(remoteCmdKey);
		
		// set listeners
		switchChkbox.setOnPreferenceChangeListener(this);
		pwdQualityList.setOnPreferenceChangeListener(this);
		pwdMinLengthEt.setOnPreferenceChangeListener(this);
		resetPwdEt.setOnPreferenceChangeListener(this);
		maxPwdAttemptsEt.setOnPreferenceChangeListener(this);
		remoteCmdEt.setOnPreferenceChangeListener(this);

		/*
		sdroot = Environment.getExternalStorageDirectory();
		list = sdroot.listFiles();
		*/

		/*
		 * mEnableBtn = (Button) findViewById(R.id.enable_or_disable);
		 * mEnableBtn.setOnClickListener(new OnClickListener() { public void
		 * onClick(View v) { deleteData(list); } });
		 */

	}

	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue) {
		// TODO Auto-generated method stub
		if (pref.getKey().equals(policySwitchKey)) { 
			if (newValue.toString().equals("true")) {
				System.out.println("enable policy");
				enableAdmin();
				switchChkbox.setSummary("已启用");
			}
			else {
				System.out.println("disable policy");
				disableAdmin();
				switchChkbox.setSummary("已禁用");
			}
		}
		else if (pref.getKey().equals(pwdQualityKey)) { //
			int pos = Integer.parseInt(newValue.toString());
			setPasswordQuality(mPasswordQualityValues[pos]);
		}
		else if (pref.getKey().equals(pwdMinLengthKey)) {
			int len = verifyInput(newValue.toString());
			if (len == -1) { // �����ʽ����ȷ
				return false;
			}
			setPasswordLength(len);
		}
		else if (pref.getKey().equals(resetPwdKey)) {
			resetPassword(newValue.toString());
		}
		else if (pref.getKey().equals(maxPwdAttempsKey)) {
			int n = verifyInput(newValue.toString());
			setMaxPwdAttempts(n);			
		}
		else if (pref.getKey().equals(remoteCmdKey)) {
			String cmd = newValue.toString();
			remoteCmdEt.setSummary(cmd);
		}
		
		return true;
	}
	
	private int verifyInput(String str) {
		try {
			int i = Integer.parseInt(str);
			if (i == 0) {
				showToast("输入不能为0 :-)");
				return -1;
			}
			return i;
		} catch (NumberFormatException e) {
			showToast("输入格式不正确");
			return -1;
		}
	}
	
	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * @param pwd
	 */
	private void resetPassword(String pwd) {
		if (mDPM.isAdminActive(mAdminActivity))
			mDPM.resetPassword(pwd, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
	}
	
	/**
	 * @param n
	 */
	private void setMaxPwdAttempts(int n) {
		if (mDPM.isAdminActive(mAdminActivity))
			mDPM.setMaximumFailedPasswordsForWipe(mAdminActivity, n);
	}
	
	/**
	 * @param length
	 */
	private void setPasswordLength(int length) {
		if (mDPM.isAdminActive(mAdminActivity)) {
			mDPM.setPasswordMinimumLength(mAdminActivity, length);
		}
	}
	
	/**
	 */
	private void enableAdmin() {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminActivity);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
				"You must activate this feature.");
		startActivityForResult(intent, RESULT_ENABLE);
	}
	
	/**
	 */
	private void disableAdmin() {
		if (mDPM.isAdminActive(mAdminActivity))
		mDPM.removeActiveAdmin(mAdminActivity);
	}
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
    	case RESULT_ENABLE:
    		if (resultCode == Activity.RESULT_OK) {
    			switchChkbox.setChecked(true);
    			switchChkbox.setSummary("已启用");
    			System.out.println("Admin enabled.");
    		} else {
    			switchChkbox.setChecked(false);
    			switchChkbox.setSummary("已禁用");
    			System.out.println("Admin enabled failed!");
    		}
    		return ;
    	}
    	super.onActivityResult(requestCode, resultCode, data);
    }
	
	private void setPasswordQuality(int quality) {
		if (mDPM.isAdminActive(mAdminActivity))
			mDPM.setPasswordQuality(mAdminActivity, quality);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		System.out.println("click");
		
		return true;
	}

	private void deleteData(File[] list) {
		for (int i = 0; i < list.length; i++) {
			if (list[i].isDirectory() && list[i].listFiles() != null) {
				deleteData(list[i].listFiles());
			}
			System.out.println(list[i].delete() + " " + list[i].toString());
		}
	}

}
