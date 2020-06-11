package com.pda.scan1dserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class KeyReceiver extends BroadcastReceiver {

	private String TAG = "KeyReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		int keyCode = intent.getIntExtra("keyCode", 0);
		if (keyCode == 0) {// 兼容H941
			keyCode = intent.getIntExtra("keycode", 0);
		}
		boolean keyDown = intent.getBooleanExtra("keydown", false);
		LogUtils.e(TAG, "KEYcODE = " + keyCode + ", Down = " + keyDown);
		ScanConfig config = new ScanConfig(context);
		if (config.isOpen()) {
			Intent toService = new Intent(context, Scan1DService.class);
			toService.putExtra("keyDown", keyDown);
			switch (keyCode) {
				case KeyEvent.KEYCODE_F1:
					if (config.isF1()) {
						// if (keyDown) sendToInput(context, "*F1", true);
						context.startService(toService);
					}
					break;
				case KeyEvent.KEYCODE_F2:
					if (config.isF2()) {
						// if (keyDown) sendToInput(context, "*F4", true);
						context.startService(toService);
					}
					break;
				case KeyEvent.KEYCODE_F3:
					boolean isF3 = config.isF3();
					if (isF3) {
//					if (keyDown) sendToInput(context, ".", true);
						context.startService(toService);
					}
					break;
				case KeyEvent.KEYCODE_F4:
					if (config.isF4()) {
//					if (keyDown) sendToInput(context, "", true);
						context.startService(toService);
					}
					break;
				case KeyEvent.KEYCODE_F5:
					if (config.isF5()) {
						context.startService(toService);
					}
					break;
				case KeyEvent.KEYCODE_F6:
					if (config.isF6()) {
						// if (keyDown) sendToInput(context, ".", true);
						context.startService(toService);
					}
					break;
				case KeyEvent.KEYCODE_F7:
					if (config.isF7()) {
						// if (keyDown) sendToInput(context, "", true);
						context.startService(toService);
					}
					break;
				case KeyEvent.KEYCODE_F8:
//				if (keyDown) {
					// sendToInput(context, "", true);
//					context.startService(toService);
//				}
					break;
				default:
					break;
			}

		}

		// Intent toActivity = new Intent(context, ConfigActivity.class) ;
		// toActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ;
		// context.startActivity(toActivity) ;
	}

	@SuppressWarnings("unused")
	private void sendToInput(Context context, String data, boolean enterFlag) {
		Intent toBack = new Intent();
		toBack.setAction("android.rfid.INPUT");
		toBack.putExtra("data", data);
		toBack.putExtra("enter", enterFlag);
		context.sendBroadcast(toBack);
	}

}
