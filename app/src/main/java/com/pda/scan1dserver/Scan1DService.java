package com.pda.scan1dserver;

import android.app.Instrumentation;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.pda.scan.ScanThread;

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

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            prefixStr = scanConfig.getPrefix();
            surfixStr = scanConfig.getSurfix();
            if (msg.what == ScanThread.SCAN) {
                byte[] dataBytes = msg.getData().getByteArray("dataBytes");
                if (dataBytes == null) {
                    return;
                }
                String data = "";
                String encoding = scanConfig.getEncoding();
                try {
                    data = new String(dataBytes, 0, dataBytes.length, encoding);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                LogUtils.e(TAG, "handleMessage: receive data: " + data);
                data = filter(data);
                // broadcast result
                broadScanResult(dataBytes, -1);
                // input prefix
                if (prefixStr.contains("\t0A0D")) {
                    sendToInput("\t", true);
                } else if ("0A0D".equals(prefixStr)) {
                    sendToInput("", true);
                } else {
                    sendToInput(prefixStr, false);
                }
                // input barcode
                sendToInput(data, false);
                // input prefix
                if (surfixStr.contains("\t0A0D")) {
                    sendToInput("\t", true);
                } else if ("".equals(surfixStr) || surfixStr.length() == 0) {
                    // sendToInput("", fa) ;
                } else if ("0A0D".startsWith(surfixStr)) {
                    sendToInput("", true);
                } else {
                    sendToInput(surfixStr, false);
                }
                if (scanConfig.isVoice()) {
                    mSoundPoolMgr.play(1);
                }
                isRuning = false;
                LogUtils.i(TAG, "handleMessage, receive data complete >>>>>> ");
            } else if (msg.what == ScanThread.SWITCH_INPUT) {
                LogUtils.i(TAG, "handleMessage, SWITCH_INPUT >>>>>> ");
                isRuning = false;
            }
        }
    };
    private SoundPoolMgr mSoundPoolMgr;

    private String TAG = "Scan1DService";

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        LogUtils.e(TAG, "onCreate");

        MainActivity.mIsOpen = new ScanConfig(this).isOpen();

        scanConfig = new ScanConfig(this);
        mSoundPoolMgr = SoundPoolMgr.getInstance(this);
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("android.rfid.KILL_SERVER");
//        registerReceiver(killReceiver, filter);
        // listner screen on/off
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(powerModeReceiver, screenFilter);
        /*
         * KeyReceiver register
         */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.rfid.FUN_KEY");
        registerReceiver(mKeyReceiver, intentFilter);
        // 在API11之后构建Notification的方式
        Notification.Builder builder = new Notification.Builder
                (this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class);

        builder.setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        // 参数一：唯一的通知标识；参数二：通知消息。
        startForeground(110, notification);// 开始前台服务
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(powerModeReceiver);
        unregisterReceiver(mKeyReceiver);
        if (scan != null) {
            scan.close();
            scan.interrupt();
            scan = null;
        }
        LogUtils.e(TAG, "onDestroy");
//        unregisterReceiver(killReceiver);
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
        super.onDestroy();
    }

    private boolean isRuning = false;

    private boolean mIsKeyPressed = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.e(TAG, "+++ onstart command++++");
        if (intent == null || intent.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK || intent.getFlags() == 10086) {
            if (scan == null) {
                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                scan = new ScanThread(mHandler);
                            } catch (IOException e) {
                                e.printStackTrace();
                                EventBus.getDefault().post(MainActivity.FLAG_OPEN_FAIL);
                            }
//                            scan.start();
                            LogUtils.e(TAG, "onStartCommand: init finished");
                        }
                    }).start();
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
        LogUtils.i(TAG, "onStartCommand >>>>>> mIsKeyPressed = " + mIsKeyPressed);
        if (keyDown && !isRuning && !mIsKeyPressed) {
            mIsKeyPressed = true;
            if (scan == null) {
                try {
                    scan = new ScanThread(mHandler);
//                    scan.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            isRuning = true;
            scan.scan();
        } else if (!keyDown && mIsKeyPressed) {
            mIsKeyPressed = false;
        }
        return Service.START_STICKY;
    }

    private void softInput(final String dataStr) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                String dataStr1 = dataStr;
                String endChar0 = "\r";
                String endChar1 = "\n";
                if (dataStr1.contains(endChar0)) {
                    dataStr1 = dataStr1.replace("\r", "");
                }
                if (dataStr1.contains(endChar1)) {
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

//    private BroadcastReceiver killReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            LogUtils.e(TAG, "onReceive, action: " + intent.getAction());
//            if (intent.getBooleanExtra("kill", false)) {
//                unregisterReceiver(mKeyReceiver);
//                LogUtils.e("killReceiver", "kll +++");
//                if (scan != null) {
//                    scan.close();
//                    scan = null;
//                }
//                Scan1DService.this.stopSelf();
//            }
//        }
//    };

    private BroadcastReceiver powerModeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isfirstboot = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("isfirstboot", false);
            LogUtils.e(TAG, "onReceive: isfirstboot = " + isfirstboot);
            String action = intent.getAction();
            // SCREEN ON ACTION
            if (action != null && action.equals(Intent.ACTION_SCREEN_ON)) {
                if (isfirstboot) {
                    return;
                }
                LogUtils.e(TAG, "powerModeReceiver, screent on +++ ");
                // new Thread(initTask).start() ;
                try {
                    if (scan == null) {
                        scan = new ScanThread(mHandler);
//                        scan.start();
                    }
                } catch (SecurityException | IOException e) {
                    e.printStackTrace();
                }
//                Scan1DService.this.onStartCommand(null, -1, 0);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.rfid.FUN_KEY");
                registerReceiver(mKeyReceiver, intentFilter);
                LogUtils.e(TAG, "onReceive: register");
            }
            // SCREEN OFF ACTION
            if (action != null && action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (isfirstboot) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("isfirstboot", false).apply();
                }
                unregisterReceiver(mKeyReceiver);
                LogUtils.e("powerModeReceiver", "screent off +++");
                if (scan != null) {
                    scan.close();
                    scan = null;
                }
            }

        }
    };

    private KeyReceiver mKeyReceiver = new KeyReceiver();

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

    /**
     * 将扫描结果以广播的形式发送，其他APP可以通过注册Action为"com.rfid.SCAN"的广播，接收扫描到的数据
     */
    private void broadScanResult(byte[] data, int codeId) {
        Intent intent = new Intent();
        intent.putExtra("data", data);
        try {
            String result;
            String encoding = scanConfig.getEncoding();
            result = new String(data, 0, data.length, encoding);
            result = filter(result);
            intent.putExtra("scannerdata", result);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        intent.putExtra("code_id", codeId);
        intent.setAction("com.rfid.SCAN");
        sendBroadcast(intent);
    }
}
