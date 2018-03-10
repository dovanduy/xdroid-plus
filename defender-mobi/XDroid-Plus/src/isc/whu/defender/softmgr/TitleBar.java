package isc.whu.defender.softmgr;

import isc.whu.defender.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TitleBar extends LinearLayout {

	// private String Text = "";
	// private String Action = "";
	private TextView mTv;
	private Button mBtn;

	public TitleBar(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public TitleBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		try {
			LayoutInflater.from(context)
					.inflate(R.layout.title_bar, this, true);
			TypedArray ta = context.obtainStyledAttributes(attrs,
					R.styleable.TitleBar);
			CharSequence text = ta.getText(R.styleable.TitleBar_tb_title);
			mTv = (TextView) findViewById(R.id.tb_title);
			mBtn = (Button) findViewById(R.id.tb_action);
			if (text != null)
				mTv.setText(text);
			CharSequence action = ta.getText(R.styleable.TitleBar_action);
			if (action != null)
				mBtn.setText(action);
			else
				mBtn.setVisibility(View.GONE);
			ta.recycle();
		} catch (Exception e) {
			Log.v("Title Bar Err", e.toString());
		}
		// int resouceId = -1;
		//
		// TextView tv = new TextView(context);
		// Button btn = new Button(context);
		//
		// resouceId = attrs.getAttributeResourceValue(null, "title", 0);
		// if (resouceId > 0) {
		// Text = context.getResources().getText(resouceId).toString();
		// } else {
		// Text = "";
		// }
		// tv.setText(Text);
		//
		// resouceId = attrs.getAttributeResourceValue(null, "action", 0);
		// if (resouceId > 0) {
		// Action = context.getResources().getText(resouceId).toString();
		// btn.setText(Action);
		// } else {
		// Action = "";
		// btn.setVisibility(View.GONE);
		// }
		// addView(tv);
		// addView(btn);
	}
}
