package isc.whu.defender.netwall;

import isc.whu.defender.R;
import isc.whu.defender.netwall.NetwallApi.NetApps;

import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;



public class NetwallActivity extends Activity implements
		OnCheckedChangeListener {

	private final int ENABLE_FIREWALL = 0;
	private final int APPLY_RULES = 1;

	private ListView listView = null;
	private boolean changed = false;

	private CheckBox all3g;
	private CheckBox allWiFi;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.netwall_main);
		NetwallApi.assertBinaries(this);

	}

	void pre() {
		all3g = (CheckBox) findViewById(R.id.all3g);
		all3g.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// isAll3GClicked = true;
				boolean isChecked = all3g.isChecked();
				if (NetwallApi.apps != null) {
					for (NetApps app : NetwallApi.apps) {
						if (app.selected_3g != isChecked) {
							app.selected_3g = isChecked;
							changed = true;
						}
					}
				}

				if (listView != null) {
					for (int i = 0; i < listView.getChildCount(); i++) {
						CheckBox checkBox = (CheckBox) listView.getChildAt(i)
								.findViewById(R.id.itemcheck_3g);
						checkBox.setChecked(isChecked);
					}
				}
			}
		});

		allWiFi = (CheckBox) findViewById(R.id.allwifi);
		allWiFi.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// isAllWiFiClicked = true;
				boolean isChecked = allWiFi.isChecked();
				if (NetwallApi.apps != null) {
					for (NetApps app : NetwallApi.apps) {
						if (app.selected_wifi != isChecked) {
							app.selected_wifi = isChecked;
							changed = true;
						}
					}
				}

				if (listView != null) {
					for (int i = 0; i < listView.getChildCount(); i++) {
						CheckBox checkBox = (CheckBox) listView.getChildAt(i)
								.findViewById(R.id.itemcheck_wifi);
						checkBox.setChecked(isChecked);
					}
				}
			}
		});

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (this.listView == null)
			this.listView = (ListView) findViewById(R.id.listview);
		LoadApplications();
		pre();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		this.listView.setAdapter(null);
	}

	private void LoadApplications() {
		final Resources res = getResources();
		if (NetwallApi.apps == null) {
			final ProgressDialog progress = ProgressDialog.show(this,
					res.getString(R.string.working),
					res.getString(R.string.loading), true);
			final Handler hander = new Handler() {
				public void handleMessage(Message msg) {
					try {
						progress.dismiss();
					} catch (Exception e) {
					}
					showApplications();
				}
			};
			new Thread() {
				public void run() {
					NetwallApi.getApps(NetwallActivity.this);
					hander.sendEmptyMessage(0);
				}
			}.start();
		} else {
			showApplications();
		}
	}

	private void showApplications() {
		this.changed = false;
		final NetApps[] NetApps = NetwallApi.apps;

		Arrays.sort(NetApps, new Comparator<NetApps>() {
			public int compare(NetApps app1, NetApps app2) {
				if ((app1.selected_wifi | app1.selected_3g) == (app2.selected_wifi | app2.selected_3g)) {
					return String.CASE_INSENSITIVE_ORDER.compare(app1.names[0],
							app2.names[0]);
				}
				if (app1.selected_wifi || app1.selected_3g) {
					return -1;
				}
				return 1;
			}
		});

		final LayoutInflater inflater = getLayoutInflater();
		final ListAdapter adapter = new ArrayAdapter<NetApps>(this,
				R.layout.netwall_item, R.id.itemtext, NetApps) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// TODO Auto-generated method stub
				ListItem item;
				if (convertView == null) {
					convertView = inflater.inflate(R.layout.netwall_item,
							parent, false);
					item = new ListItem();
					item.icon = (ImageView) convertView.findViewById(R.id.icon);
					item.item_wifi = (CheckBox) convertView
							.findViewById(R.id.itemcheck_wifi);
					item.item_3g = (CheckBox) convertView
							.findViewById(R.id.itemcheck_3g);
					item.text = (TextView) convertView
							.findViewById(R.id.itemtext);
					item.item_wifi
							.setOnCheckedChangeListener(NetwallActivity.this);
					item.item_3g
							.setOnCheckedChangeListener(NetwallActivity.this);
					convertView.setTag(item);

				} else {
					item = (ListItem) convertView.getTag();
				}
				final NetApps app = NetApps[position];
				item.icon.setImageDrawable(app.icon);
				item.text.setText(app.toString());
				final CheckBox item_wifi = item.item_wifi;
				item_wifi.setTag(app);
				item_wifi.setChecked(app.selected_wifi);

				final CheckBox item_3g = item.item_3g;
				item_3g.setTag(app);
				item_3g.setChecked(app.selected_3g);
				return convertView;
			}

		};
		this.listView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		boolean enabled = NetwallApi.isEnabled(NetwallActivity.this);
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		if(enabled) 
			inflater.inflate(R.menu.netwall_enabled_menu , menu);
		else
			inflater.inflate(R.menu.netwall_disabled_menu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		boolean enabled = NetwallApi.isEnabled(NetwallActivity.this);
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		if(enabled) 
			inflater.inflate(R.menu.netwall_enabled_menu , menu);
		else
			inflater.inflate(R.menu.netwall_disabled_menu, menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		int itemId = item.getItemId();
		if (itemId == R.id.menu_enable
				|| itemId == R.id.menu_disable) {
			enableOrDisable();
		} else if (itemId == R.id.menu_apply
				|| itemId == R.id.menu_save) {
			applyOrSaveRules();
		}
		return true;
	}

	private void enableOrDisable() {
		final boolean enabled = !NetwallApi.isEnabled(this);
		NetwallApi.setEnabled(this, enabled);
		if (enabled) {
			applyOrSaveRules();
		} else {
			purgeRules();
		}
	}

	private void applyOrSaveRules() {
		final Resources res = getResources();
		final boolean enabled = NetwallApi.isEnabled(this);
		final ProgressDialog progress = ProgressDialog.show(this,
				res.getString(R.string.working),
				res.getString(R.string.loading), true);
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				try {
					progress.dismiss();
				} catch (Exception e) {
				}
				if (enabled) {
					if (NetwallApi.hasRootAccess(NetwallActivity.this, true)
							&& NetwallApi
									.applyIptablesRules(NetwallActivity.this)) {
						Toast.makeText(NetwallActivity.this, "规则已应用",
								Toast.LENGTH_SHORT).show();
					} else {
						NetwallApi.setEnabled(NetwallActivity.this, false);
					}
				} else {
					NetwallApi.saveRules(NetwallActivity.this);
					Toast.makeText(NetwallActivity.this, "规则已保存",
							Toast.LENGTH_SHORT).show();
				}
				NetwallActivity.this.changed = false;
			}
		};
		handler.sendEmptyMessageDelayed(0, 100);
	}

	private void purgeRules() {
		final Resources res = getResources();
		final ProgressDialog progress = ProgressDialog.show(this,
				res.getString(R.string.working),
				res.getString(R.string.loading), true);
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				try {
					progress.dismiss();
				} catch (Exception e) {
				}
				if (!NetwallApi.hasRootAccess(NetwallActivity.this, true))
					return;
				if (NetwallApi.purgeIptables(NetwallActivity.this)) {
					Toast.makeText(NetwallActivity.this, "规则已删除",
							Toast.LENGTH_SHORT).show();
				}
			}
		};
		handler.sendEmptyMessageDelayed(0, 100);
	}

	private static class ListItem {
		private ImageView icon;
		private CheckBox item_wifi;
		private CheckBox item_3g;
		private TextView text;
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		final NetApps app = (NetApps) buttonView.getTag();
		if (app != null) {
			int id = buttonView.getId();
			if (id == R.id.itemcheck_wifi) {
				if (app.selected_wifi != isChecked) {
					app.selected_wifi = isChecked;
					this.changed = true;
				}
			} else if (id == R.id.itemcheck_3g) {
				if (app.selected_3g != isChecked) {
					app.selected_3g = isChecked;
					this.changed = true;
				}
			}
			updateCheckBox();
		}
	}

	private void updateCheckBox() {

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				all3g = (CheckBox) findViewById(R.id.all3g);
				allWiFi = (CheckBox) findViewById(R.id.allwifi);
				switch (msg.what) {
				case 3:
					all3g.setChecked(true);
					allWiFi.setChecked(true);
					break;
				case 2:
					all3g.setChecked(true);
					allWiFi.setChecked(false);
					break;
				case 1:
					all3g.setChecked(false);
					allWiFi.setChecked(true);
					break;
				case 0:
					all3g.setChecked(false);
					allWiFi.setChecked(false);
					break;
				}
			}
		};
		new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (NetwallApi.apps != null) {
					int isAll3G = 2, isAllWiFi = 1;
					for (NetApps app : NetwallApi.apps) {
						if (!app.selected_3g) {
							isAll3G = 0;
							break;
						}
					}
					for (NetApps app : NetwallApi.apps) {
						if (!app.selected_wifi) {
							isAllWiFi = 0;
							break;
						}
					}

					handler.sendEmptyMessage(isAll3G + isAllWiFi);
				}
			}
		}.start();
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		// TODO Auto-generated method stub
		if (this.changed && (keyCode == KeyEvent.KEYCODE_BACK)) {
			final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						applyOrSaveRules();
					case DialogInterface.BUTTON_NEGATIVE:
						NetwallApi.apps = null;
						finish();
						break;
					}
				}
			};

			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle(R.string.unsaved_changes)
					.setMessage(R.string.unsaved_changes_message)
					.setPositiveButton(R.string.apply, dialogClickListener)
					.setNegativeButton(R.string.discard, dialogClickListener)
					.show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
