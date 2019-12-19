package com.pda.scan1dserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;

import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;

import cn.pda.scan.ScanThread;
import cn.pda.serialport.Tools;

/**
 * @author LeiHuang
 */
public class Scan1DService extends Service {

	private ScanThread scan = null;

	private ScanConfig scanConfig;
	private String prefixStr;
	private String surfixStr;

	private ThreadFactory threadFactory = Executors.defaultThreadFactory();
	private ExecutorService mExecutorService = new ThreadPoolExecutor(3, 200, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());

	private MyHandler mHandler = new MyHandler(Scan1DService.this);
	private SoundPoolMgr mSoundPoolMgr;

	private static class MyHandler extends Handler {
		private WeakReference<Scan1DService> mWeakReference;

		public MyHandler(Scan1DService scan1dService) {
			mWeakReference = new WeakReference<Scan1DService>(scan1dService);
		}

		@Override
		public void handleMessage(android.os.Message msg) {
			Scan1DService scan1dService = mWeakReference.get();
			scan1dService.prefixStr = scan1dService.scanConfig.getPrefix();
			scan1dService.surfixStr = scan1dService.scanConfig.getSurfix();
			if (msg.what == ScanThread.SCAN) {
				byte[] dataBytes = msg.getData().getByteArray("dataBytes");
				if (dataBytes == null){
				    return;
                }
				String data = "";
                String encoding = scan1dService.scanConfig.getEncoding();
                try {
                    data = new String(dataBytes, 0, dataBytes.length, encoding);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                data = filter(data);
				// input prefix
				if (scan1dService.prefixStr.contains("\t0A0D")) {
					scan1dService.sendToInput("\t", true);
				} else if ("0A0D".equals(scan1dService.prefixStr)) {
					scan1dService.sendToInput("", true);
				} else {
					scan1dService.sendToInput(scan1dService.prefixStr, false);
				}
				// input barcode
				scan1dService.sendToInput(data, false);
				// input prefix
				if (scan1dService.surfixStr.contains("\t0A0D")) {
					scan1dService.sendToInput("\t", true);
				} else if ("".equals(scan1dService.surfixStr) || scan1dService.surfixStr.length() == 0) {
					// sendToInput("", fa) ;
				} else if ("0A0D".startsWith(scan1dService.surfixStr)) {
					scan1dService.sendToInput("", true);
				} else {
					scan1dService.sendToInput(scan1dService.surfixStr, false);
				}
				if (scan1dService.scanConfig.isVoice()) {
					scan1dService.mSoundPoolMgr.play(1);
				}
				scan1dService.isRuning = false;
			} else if (msg.what == ScanThread.SWITCH_INPUT) {
				LogUtils.i(scan1dService.TAG, "handleMessage, SWITCH_INPUT >>>>>> ");
				scan1dService.isRuning = false;
			}
		}
	}

	private String TAG = "Scan1DService";

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onCreate() {
		LogUtils.e(TAG, "onCreate");
		scanConfig = new ScanConfig(this);
		mSoundPoolMgr = SoundPoolMgr.getInstance(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.rfid.KILL_SERVER");
		registerReceiver(killReceiver, filter);
		// listner screen on/off
		IntentFilter screenFilter = new IntentFilter();
		screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		screenFilter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(powerModeReceiver, screenFilter);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		LogUtils.e(TAG, "onDestroy");
		unregisterReceiver(killReceiver);
		unregisterReceiver(powerModeReceiver);
		super.onDestroy();
	}

	private boolean isRuning = false;

	private boolean mIsKeyPressed = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogUtils.e(TAG, "+++ onstart command++++");
		if (intent == null || intent.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK) {
			if (scan == null) {
				try {
					scan = new ScanThread(mHandler);
					scan.start();
				} catch (Exception e) {
					e.printStackTrace();
					EventBus.getDefault().post(MainActivity.FLAG_OPEN_FAIL);
				}
			}
			return Service.START_STICKY;
		}
		boolean keyDown = intent.getBooleanExtra("keyDown", false);
		LogUtils.i(TAG, "onStartCommand >>>>>> keyDown = " + keyDown);
		LogUtils.i(TAG, "onStartCommand >>>>>> isRunning = " + isRuning);
		if (keyDown && !isRuning && !mIsKeyPressed) {
			mIsKeyPressed = true;
			if (scan == null) {
				try {
					scan = new ScanThread(mHandler);
					scan.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			isRuning = true;
			scan.scan();
		} else if (!keyDown && mIsKeyPressed){
			mIsKeyPressed = false;
		}
		return Service.START_STICKY;
	}

	private void softInput(final String dataStr){
		mExecutorService.execute(new Runnable() {
			@Override
			public void run() {
			    String dataStr1 = dataStr;
			    String endChar0 = "\r";
			    String endChar1 = "\n";
			    if (dataStr1.contains(endChar0)){
                    dataStr1 = dataStr1.replace("\r", "");
                }
			    if (dataStr1.contains(endChar1)){
                    dataStr1 = dataStr1.replace("\n", "");

                }
				Instrumentation instrumentation = new Instrumentation();
				// input prefix
				if (prefixStr.contains("\t0A0D")) {
					instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB);
					instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
				} else if ("0A0D".equals(prefixStr)) {
					instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
				} else {
					instrumentation.sendStringSync(prefixStr);
				}
				instrumentation.sendStringSync(dataStr1);
				// input prefix
				if (surfixStr.contains("\t0A0D")) {
					instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB);
					instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
				} else if ("".equals(surfixStr) || surfixStr.length() == 0) {
					// sendToInput("", fa) ;
				} else if ("0A0D".startsWith(surfixStr)) {
					instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
				} else {
					instrumentation.sendStringSync(surfixStr);
				}
			}
		});
	}

	private void sendToInput(final String data, final boolean enterFlag) {
		Intent toBack = new Intent();
		toBack.setAction("android.rfid.INPUT");
		toBack.putExtra("data", data);
		toBack.putExtra("enter", enterFlag);
		sendBroadcast(toBack);
	}

	private BroadcastReceiver killReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			LogUtils.e(TAG, "onReceive, action: " + intent.getAction());
			if (intent.getBooleanExtra("kill", false)) {
				if (scan != null) {
					scan.close();
					scan = null;
				}
				Scan1DService.this.stopSelf();
			}
		}
	};

	// listner
	private BroadcastReceiver powerModeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// SCREEN ON ACTION
			if (action != null && action.equals(Intent.ACTION_SCREEN_ON)) {
				LogUtils.e(TAG, "powerModeReceiver, screent on +++ ");
				// new Thread(initTask).start() ;
				try {
					scan = new ScanThread(mHandler);
					scan.start();
				} catch (SecurityException | IOException e) {
					e.printStackTrace();
				}
			}
			// SCREEN OFF ACTION
			if (action != null && action.equals(Intent.ACTION_SCREEN_OFF)) {
				LogUtils.e("powerModeReceiver", "screent off +++");
				if (scan != null) {
					scan.close();
					scan = null;
				}
			}

		}
	};

    /**
     * 去除不可见字符
     */
    public static String filter(String content) {
        if (content != null && content.length() > 0) {
            char[] contentCharArr = content.toCharArray();
            char[] contentCharArrTem = new char[contentCharArr.length];
            int j = 0;
            for (char c : contentCharArr) {
                if (c >= 0x20 && c != 0x7F) {
                    contentCharArrTem[j] = c;
                    j++;
                }
            }
            return new String(contentCharArrTem, 0, j);
        }
        return "";
    }
}
