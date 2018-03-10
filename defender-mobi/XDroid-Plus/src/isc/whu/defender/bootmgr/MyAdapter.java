package isc.whu.defender.bootmgr;

import isc.whu.defender.R;
import isc.whu.defender.common.CommonTools;

import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private List<Map<String, Object>> mData;

	public MyAdapter(Context context, List<Map<String, Object>> list) {
		this.mInflater = LayoutInflater.from(context);
		mData = list;
	}

	public int getCount() {
		return mData.size();
	}

	public Object getItem(int arg0) {
		return null;
	}

	public long getItemId(int arg0) {
		return 0;
	}

	public View getView(int pos, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();

			convertView = mInflater.inflate(R.layout.bootmgr_list_item, null);
			holder.appIcon = (ImageView) convertView
					.findViewById(R.id.app_icon);
			holder.appName = (TextView) convertView.findViewById(R.id.app_name);
			holder.chkBox = (CheckBox) convertView.findViewById(R.id.autoboot);
			holder.pkgName = (String) mData.get(pos).get("package_name");
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Drawable appIcon 	= (Drawable) mData.get(pos).get("app_icon");
		String appName 		= (String) mData.get(pos).get("app_name");
		String pkgName 		= (String) mData.get(pos).get("pkg_name");
		String clsName 		= (String) mData.get(pos).get("cls_name");
		
		ComponentName c = new ComponentName(pkgName, clsName);

		holder.appIcon.setImageDrawable(appIcon);
		holder.appName.setText(appName);
		
		holder.chkBox.setOnCheckedChangeListener(new MyOnCheckedChangeListener(c));
		
		return convertView;
	}
	
	private class MyOnCheckedChangeListener implements OnCheckedChangeListener {
		String pkgName;
		String clsName;
		public MyOnCheckedChangeListener(ComponentName c) {
			super();
			pkgName = c.getPackageName();
			clsName = c.getClassName();
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			String type = isChecked ? "enable" : "disable";
			String cmd = String.format("su -c \"pm %s %s/%s\"", type, pkgName, clsName);
			System.out.println(cmd);
//			CommonTools.runRootCommand(cmd);
		}
	}
}

class ViewHolder {
	public ImageView appIcon;
	public TextView appName;
	public CheckBox chkBox;
	public String pkgName;
}