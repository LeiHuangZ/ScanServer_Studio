package cn.pda.scan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.pda.scan1dserver.LogUtils;
import com.pda.scan1dserver.MainActivity;
import com.pda.scan1dserver.ScanConfig;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import cn.pda.serialport.SerialPort;

/**
 * @author LeiHuang
 */
public class ScanThread extends Thread {

    private final String TAG = "Huang," + ScanThread.class.getSimpleName();

//    private ThreadFactory threadFactory = Executors.defaultThreadFactory();
//    private ExecutorService mExecutorService;

    private Timer mTimer = null;
    private TimerTask mTimerTask = null;

    private SerialPort mSerialPort;
    public static int port = 0;
    private InputStream is;
    private OutputStream os;

    private Handler mHandler;

    private boolean mCancelFlag = true;

    private volatile boolean mRunFlag;

    public static int SCAN = 1001;
    public static int SWITCH_INPUT = 1002;

    private int mCount = 0;

    /**
     * if throw exception, serialport initialize fail.
     */
    public ScanThread(Handler handler) throws SecurityException, IOException {
        mHandler = handler;
        int baudRate = 9600;
//        try {
//            Thread.sleep(450);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        new SerialPort().scannerTrigOn();
//        try {
//            Thread.sleep(450);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        new SerialPort().powerScannerOn();
        try {
            Thread.sleep(1300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mSerialPort = new SerialPort(port, baudRate);
        is = mSerialPort.getInputStream();
        os = mSerialPort.getOutputStream();
        byte[] buffer = new byte[128];
        int read = is.read(buffer);
        LogUtils.e(TAG, "ScanThread read: " + read);
        mSerialPort.scannerTrigOff();
        this.start();
    }

    @Override
    public void run() {
        try {
            mRunFlag = true;
            EventBus.getDefault().post(MainActivity.FLAG_OPEN_SUCCESS);
            LogUtils.e(TAG, "ScanThread run");
            byte[] buffer = new byte[4096];
            int available;
            int size;
            while (!isInterrupted() && mRunFlag) {
                LogUtils.e(TAG, "run: mIsOpen = " + MainActivity.mIsOpen);
                if (!MainActivity.mIsOpen) {
                    return;
                }
                available = is.available();
                if (available > 0) {
                    size = is.read(buffer);
                    if (size > 0) {
                        stopScan();
                        LogUtils.e(TAG, "mCancelScanTask switch input");
                        mCount = 0;
                        mCancelFlag = false;
                        stopTimer();
                        sendMessage(buffer, size, SCAN);
                    }
                }
                LogUtils.e(TAG, "run: ");
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.run();
    }

    private void sendMessage(byte[] dataTemp, int dataLen, int mode) {
        String dataStr = "";
        byte[] data = new byte[dataLen];
        if (dataTemp == null) {
            mHandler.sendEmptyMessage(SWITCH_INPUT);
            return;
        }
        System.arraycopy(dataTemp, 0, data, 0, dataLen);
        if (dataLen > 0) {
            try {
                dataStr = new String(data, 0, dataLen, StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString("data", dataStr);
        bundle.putByteArray("dataBytes", data);
        Message msg = new Message();
        msg.what = mode;
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public void scan() {
        try {
//            if (mSerialPort.scannerTrigState()) {
//                mSerialPort.scannerTrigOff();
//                Thread.sleep(30);
//                return;
//            }
//
//            os.write(Tools.HexString2Bytes("00"));
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            os.write(Tools.HexString2Bytes("00"));
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            os.write(Tools.HexString2Bytes("00"));
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            os.write(Tools.HexString2Bytes("04A30400FF55"));
//            mSerialPort.scannerTrigOff();
            mSerialPort.scannerTrigOn();
            LogUtils.e(TAG, "scan: ");
            if (mTimer == null) {
                mTimer = new Timer();
            }
            mCount = 0;
            if (mTimerTask == null) {
                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        LogUtils.i(TAG, "mCancelScanTask count: ");
//                        do {
                            LogUtils.i(TAG, "mCancelScanTask sleep(1000)... count = " + mCount);
//                        } while (mCancelFlag);
                        if (mCount == 6) {
                            stopScan();
                            LogUtils.e(TAG, "mCancelScanTask switch input");
                            sendMessage(null, 0, SWITCH_INPUT);
                            mCount = 0;
                            mCancelFlag = false;
                            stopTimer();
                        }
                        mCount++;
                    }
                };
            }

            if (mTimer != null && mTimerTask != null) {
                mTimer.schedule(mTimerTask, 0, 500);
            }
//            mExecutorService = new ThreadPoolExecutor(1, 200, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());
            mCancelFlag = true;
//            mExecutorService.submit(mCancelScanTask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        LogUtils.e(TAG, "stopTimer: ");
    }
//    private Runnable mCancelScanTask = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                int loopCount = 0;
//                do {
//                    if (!mCancelFlag) {
//                        return;
//                    }
////                    LogUtils.e(TAG, "run: ");
//                    Thread.sleep(1);
//                    loopCount++;
//                } while (loopCount < 2600);
////                if (mSerialPort.scannerTrigState()) {
//                stopScan();
//                LogUtils.e(TAG, "mCancelScanTask switch input");
//                sendMessage(null, 0, SWITCH_INPUT);
////                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                sendMessage(null, 0, SWITCH_INPUT);
//            }
//        }
//    };

    public void stopScan() {
//        if (mSerialPort.scannerTrigState()) {
        mSerialPort.scannerTrigOff();
//        }
//        if (mExecutorService != null) {
        mCancelFlag = false;
//        stopTimer();
//            mExecutorService.shutdownNow();
//            mExecutorService = null;
        LogUtils.e(TAG, "stopScan: ");
//        }
    }

    public void close() {
        stopScan();
//        if (mExecutorService != null) {
        mCancelFlag = false;
        stopTimer();
        sendMessage(null, 0, SWITCH_INPUT);
//            mExecutorService.shutdownNow();
//            mExecutorService = null;
        LogUtils.e(TAG, "close stopScan: ");
//        }
        mRunFlag = false;
        if (mSerialPort != null) {
            mSerialPort.powerScannerOff();
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSerialPort.closePort(port);
            try {
                new SerialPort(11, 115200);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
