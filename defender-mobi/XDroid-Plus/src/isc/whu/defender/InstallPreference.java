package isc.whu.defender;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class InstallPreference extends Preference {
	private Context mContext;
	public Button btnInstall, btnUninstall;
	private View.OnClickListener mInstallListener;
	private View.OnClickListener mUninstallListener;

	public InstallPreference(Context context) {
		super(context);
		mContext = context;
	}

	public InstallPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public InstallPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewGroup = inflater.inflate(R.layout.tab_setting, null);
		LinearLayout ll = (LinearLayout) viewGroup
				.findViewById(R.id.llInstallFrame);
		btnInstall = (Button) viewGroup.findViewById(R.id.btnInstall);
		btnUninstall = (Button) viewGroup.findViewById(R.id.btnUninstall);
		btnInstall.setOnClickListener(mInstallListener);
		btnUninstall.setOnClickListener(mUninstallListener);
		return ll;
	}

	public void setInstallListener(View.OnClickListener listener) {
		this.mInstallListener = listener;
	}

	public void setUnintallListenerr(View.OnClickListener listener) {
		this.mUninstallListener = listener;
	}
}
