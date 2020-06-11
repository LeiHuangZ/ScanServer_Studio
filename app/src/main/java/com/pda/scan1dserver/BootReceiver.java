package com.pda.scan1dserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

/***
 * 开机自启动接收者
 * @author admin
 *
 */
public class BootReceiver extends BroadcastReceiver {
	static final String ACTION = "android.intent.action.BOOT_COMPLETED";
	ScanConfig config ;
	@Override
	public void onReceive(Context context, Intent intent) {
		config = new ScanConfig(context) ;
		if(intent.getAction().equals(ACTION)){
			if(config.isOpen()){
//	            Intent intent1 = new Intent(context, FloatWindow.class);
//	            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//	            context.startService(intent1);
				PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("isfirstboot", true).apply();
				Intent intent2 = new Intent(context, Scan1DService.class);
				intent2.addFlags(10086);
				context.startService(intent2);
			}

		}

	}

}
