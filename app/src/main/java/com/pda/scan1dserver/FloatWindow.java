package com.pda.scan1dserver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class FloatWindow extends Service {

	private WindowManager wm;

	private float mTouchX;
	private float mTouchY;
	private float x;
	private float y;
	private float mStartX;
	private float mStartY;

	LinearLayout mFloatLayout;
	LayoutParams wmParams;
	Button btnScan;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		createFloatWindow();
		initReceiver();
		Intent toService = new Intent(this, Scan1DService.class);
		toService.addFlags(1693);
		startService(toService);

		super.onCreate();
	}

	@Override
	public void onDestroy() {
		wm.removeView(mFloatLayout);
		if (receiver != null) {
			unregisterReceiver(receiver);
		}
		super.onDestroy();
	}

	private void createFloatWindow() {
		wmParams = new LayoutParams();
		wm = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);

		wmParams.type = LayoutParams.TYPE_PHONE;
		wmParams.format = PixelFormat.RGBA_8888;
		wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
		wmParams.gravity = Gravity.START | Gravity.TOP;
		wmParams.x = dm.widthPixels;
		wmParams.y = dm.heightPixels / 2;
		wmParams.width = 60;
		wmParams.height = 60;
		LayoutInflater inflater = LayoutInflater.from(getApplication());
		mFloatLayout = (LinearLayout) inflater.inflate(R.layout.float_window, null);
		mFloatLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				x = event.getRawX();
				y = event.getRawY();
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					Log.e("", "ACTION_DOWN");
					mTouchX = event.getX();
					mTouchY = event.getY();
					mStartX = x;
					mStartY = y;

					mFloatLayout.setBackground(ContextCompat.getDrawable(FloatWindow.this, R.drawable.bg_close));
					break;
				case MotionEvent.ACTION_MOVE:
					if ((x - mStartX) < 5 && (y - mStartY) < 5) {
					}
					break;
				case MotionEvent.ACTION_UP:
					mFloatLayout.setBackground(ContextCompat.getDrawable(FloatWindow.this, R.drawable.bg));
					// 鐐瑰嚮鏈夋晥
					if ((x - mStartX) < 5 && (y - mStartY) < 5) {
						Intent to1D = new Intent(FloatWindow.this, Scan1DService.class);
						to1D.putExtra("h711", true);
						to1D.putExtra("keyDown", true);
						startService(to1D);
					} else {
						Log.e("onclice", "finish++++");
						updateView();
						mFloatLayout.setBackground(ContextCompat.getDrawable(FloatWindow.this, R.drawable.bg));
						mTouchX = mTouchY = 0;
					}
					break;
				}
				return true;
			}
		});
		// 娣诲姞mFloatLayout
		wm.addView(mFloatLayout, wmParams);

	}

	// 绉诲姩鎮诞绐楀彛
	private void updateView() {
		wmParams.x = (int) (x - mTouchX);
		wmParams.y = (int) (y - mTouchY);
		wm.updateViewLayout(mFloatLayout, wmParams);
	}

	// 娉ㄥ唽骞挎挱
	private MReceiver receiver;

	private void initReceiver() {
		receiver = new MReceiver();
		IntentFilter filter = new IntentFilter("com.example.scanservice.FloatWindow");
		registerReceiver(receiver, filter);
	}

	// 骞挎挱鎺ユ敹鑰�
	private class MReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			stopSelf();
		}

	}

}
