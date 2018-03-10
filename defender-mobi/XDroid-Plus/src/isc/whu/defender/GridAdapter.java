package isc.whu.defender;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GridAdapter extends BaseAdapter {
	private Context context;
	private String[] itemString;
	private int[] itemIcons;

	public GridAdapter(Context con, String[] itemString, int[] itemIcons) {
		context = con;
		this.itemString = itemString;
		this.itemIcons = itemIcons;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return itemIcons.length;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return itemString[position];
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.item1, null);
		ImageView iv = (ImageView) v.findViewById(R.id.item_grid);
		TextView tv = (TextView) v.findViewById(R.id.item_text);
		iv.setImageResource(itemIcons[position]);
		tv.setText(itemString[position]); 
		return v;
	}
}
