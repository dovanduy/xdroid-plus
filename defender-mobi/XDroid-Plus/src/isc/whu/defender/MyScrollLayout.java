package isc.whu.defender;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class MyScrollLayout extends ViewGroup {

	private static final String TAG = "Scroll";
	private VelocityTracker mVelocityTracker; // 用于判断甩动手势
	private static final int SNAP_VELOCITY = 600;
	private Scroller mScroller; // 滑动控制
	private int mCurScreen;
	private int mDefaultScreen = 0;
	private float mLastMotionX;
	private float mLastMotionY;
	private int mTouchSlop;

	/**
	 * True if the user is currently dragging this ScrollView around. This is
	 * not the same as 'is being flinged', which can be checked by
	 * mScroller.isFinished() (flinging begins when the user lifts his finger).
	 */
	private boolean mIsBeingDragged = false;
	private OnViewChangeListener mOnViewChangeListener;

	public MyScrollLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public MyScrollLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public MyScrollLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}

	private void init(Context context) {
		mCurScreen = mDefaultScreen;
		// mTouchSlop =
		// ViewConfiguration.get(getContext()).getScaledTouchSlop();
		mScroller = new Scroller(context);
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		if (changed) {
			int childTop = 0;
			final int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				final View childView = getChildAt(i);
				if (childView.getVisibility() != View.GONE) {
					final int childWidth = childView.getMeasuredWidth();
					final int childHeight = childView.getMeasuredHeight();
					childView.layout(0, childTop, childWidth, childTop
							+ childHeight);
					childTop += childHeight;
				}
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int height = MeasureSpec.getSize(heightMeasureSpec);
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		scrollTo(0, mCurScreen * height);
	}

	public void snapToDestination() {
		final int screenHeight = getHeight();
		final int destScreen = (getScrollY() + screenHeight / 2) / screenHeight;
		snapToScreen(destScreen);
	}

	public void snapToScreen(int whichScreen) {
		// get the valid layout page
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		if (getScrollY() != (whichScreen * getHeight())) {
			int delta = whichScreen * getHeight() - getScrollY();
			Log.i(TAG, "getHeight=" + getHeight() + ",whichScreen"
					+ whichScreen + ",getScrollY()" + getScrollY());
			if (whichScreen == 1)
				delta = getHeight() / 3 - getScrollY();
			mScroller.startScroll(0, getScrollY(), 0, delta,
					Math.abs(delta) * 2);
			mCurScreen = whichScreen;
			Log.i(TAG, "delta=" + delta);
			invalidate(); // Redraw the layout
			if (mOnViewChangeListener != null) {
				mOnViewChangeListener.OnViewChange(mCurScreen);
			}
		}
	}

	@Override
	public void computeScroll() {
		// TODO Auto-generated method stub
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onInterceptTouchEvent-slop:" + mTouchSlop);

		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
			// Log.v(TAG, "onInterceptTouchEvent:short cut:return true");
			return true;
		}

		final float x = ev.getX();
		final float y = ev.getY();

		switch (action) {
		case MotionEvent.ACTION_MOVE:
			// Log.v(TAG, "onInterceptTouchEvent:ACTION_MOVE");
			final int yDiff = (int) Math.abs(mLastMotionY - y);
			if (yDiff > mTouchSlop) {
				mLastMotionX = x;
				mLastMotionY = y;
				mIsBeingDragged = true;
			}
			break;

		case MotionEvent.ACTION_DOWN:
			// Log.v(TAG, "onInterceptTouchEvent:ACTION_DOWN");
			mLastMotionX = x;
			mLastMotionY = y;
			/*
			 * If being flinged and user touches the screen, initiate drag;
			 * otherwise don't. mScroller.isFinished should be false when being
			 * flinged.
			 */
			mIsBeingDragged = !mScroller.isFinished();
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// Log.v(TAG, "onInterceptTouchEvent:ACTION_DOWN");
			mIsBeingDragged = false;
			break;
		}
		// Log.v(TAG, "onInterceptTouchEvent:return " + mIsBeingDragged);
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// Log.v(TAG, "onTouchEvent  ACTION_DOWN");
			if (!mIsBeingDragged) {
				Log.v(TAG, "onTouchEvent:mIsBeingDragged false return");
				return false;
			}
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
				mVelocityTracker.addMovement(event);
			}
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			mLastMotionX = x;
			mLastMotionY = y;
			break;

		case MotionEvent.ACTION_MOVE:
			// Log.v(TAG, "onTouchEvent  ACTION_MOVE");
			int deltaY = (int) (mLastMotionY - y);
			if (IsCanMove(deltaY)) {
				if (mVelocityTracker != null) {
					mVelocityTracker.addMovement(event);
				}
				mLastMotionX = x;
				mLastMotionY = y;
				scrollBy(0, deltaY);
			}
			break;
		case MotionEvent.ACTION_UP:
			// Log.v(TAG, "onTouchEvent  ACTION_UP");
			if (mIsBeingDragged) {
				int velocityY = 0;
				if (mVelocityTracker != null) {
					mVelocityTracker.addMovement(event);
					mVelocityTracker.computeCurrentVelocity(1000);
					velocityY = (int) mVelocityTracker.getYVelocity();
				}
				if (velocityY > SNAP_VELOCITY && mCurScreen > 0) {
					Log.v(TAG, "snap down");
					snapToScreen(mCurScreen - 1);
				} else if (velocityY < -SNAP_VELOCITY
						&& mCurScreen < getChildCount() - 1) {
					Log.v(TAG, "snap up");
					snapToScreen(mCurScreen + 1);
				} else {
					snapToDestination();
				}

				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				mIsBeingDragged = false;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsBeingDragged && getChildCount() > 0) {
				mIsBeingDragged = false;
				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
			}
			break;
		}
		return true;
	}

	private boolean IsCanMove(int deltaY) {
		// if (getScrollX() <= 0 && deltaX < 0) {
		if (getScrollY() <= 0 && deltaY < 0) {
			return false;
		}
		// if (getScrollX() >= (getChildCount() - 1) * getWidth() && deltaX > 0)
		// {
		if (getScrollY() >= (getChildCount() - 1) * getHeight() && deltaY > 0) {
			return false;
		}
		return true;
	}

	public void SetOnViewChangeListener(OnViewChangeListener listener) {
		mOnViewChangeListener = listener;
	}

	public int GetCurScreen() {
		return mCurScreen;
	}
}