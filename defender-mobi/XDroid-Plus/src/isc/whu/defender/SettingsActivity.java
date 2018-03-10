package isc.whu.defender;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SettingsActivity extends FragmentActivity {

	private static final String TAG = "XDroid-app";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setTitle(String.format("%s - %s", getString(R.string.app_name),
				getString(R.string.menu_setting)));
		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment()).commit();
	}
}
