package cn.pda.scan;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.pda.scan1dserver.BaseApplication;
import com.pda.scan1dserver.MainActivity;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import cn.pda.serialport.SerialPort;

/**
 * @author LeiHuang
 */
public class ScanThread extends Thread {

    private final String TAG = ScanThread.class.getSimpleName();

    private ThreadFactory threadFactory = Executors.defaultThreadFactory();
    //    private ExecutorService mExecutorService;
    private ScheduledExecutorService mScheduledExecutorService;

    private SerialPort mSerialPort;
    public static int port = 0;
    private InputStream is;
    private OutputStream os;

    private volatile boolean mRunFlag;

    public static int SCAN = 1001;
    public static int SWITCH_INPUT = 1002;

    private Handler mHandler;

    private int mDecodeTimeout = 3000;

    /**
     * if throw exception, serialport initialize fail.
     */
    public ScanThread(Handler handler) throws SecurityException, IOException, InterruptedException {
        int baudRate = 9600;
//        try {
//            Thread.sleep(450);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        new SerialPort().scannerTrigOn();
//        try {
//            Thread.sleep(450);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        new SerialPort().powerScannerOn();
        mSerialPort = new SerialPort(port, baudRate);
        Thread.sleep(100);
        is = mSerialPort.getInputStream();
        os = mSerialPort.getOutputStream();
        mSerialPort.scannerTrigOff();
        mHandler = handler;
    }

    @Override
    public void run() {
        try {
            mRunFlag = true;
            EventBus.getDefault().post(MainActivity.FLAG_OPEN_SUCCESS);
            Log.e(TAG, "ScanThread run");
            while (!isInterrupted() && mRunFlag) {
                if (!BaseApplication.mIsOpen) {
                    return;
                }
                int size;
                byte[] buffer = new byte[4096];
                int available;
                available = is.available();
                if (available > 0) {
                    size = is.read(buffer);
                    if (size > 0) {
                        stopScan();
                        sendMessage(buffer, size, SCAN);
                    }
                }
                Thread.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
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
            Log.d(TAG, "[scan] TrigOn");
//            mExecutorService = new ThreadPoolExecutor(1, 200, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());
            mScheduledExecutorService = new ScheduledThreadPoolExecutor(3, threadFactory);
            mCancelFlag = true;
//            mExecutorService.submit(mCancelScanTask);
            mScheduledExecutorService.schedule(mCancelScanSchedule, mDecodeTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean mCancelFlag = true;
    private Runnable mCancelScanTask = new Runnable() {
        @Override
        public void run() {
            try {
                int loopCount = 0;
                do {
                    if (!mCancelFlag) {
                        return;
                    }
                    Log.e(TAG, "run: ");
                    Thread.sleep(1);
                    loopCount++;
                } while (loopCount < mDecodeTimeout);
//                if (mSerialPort.scannerTrigState()) {
                stopScan();
                Log.e(TAG, "mCancelScanTask switch input");
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable mCancelScanSchedule = new Runnable() {
        @Override
        public void run() {
            try {
                stopScan();
                Log.e(TAG, "mCancelScanTask switch input");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void stopScan() {
//        if (mSerialPort.scannerTrigState()) {
        mSerialPort.scannerTrigOff();
//        }
//        if (mExecutorService != null) {
//            mCancelFlag = false;
//            mExecutorService.shutdownNow();
//            mExecutorService = null;
//            Log.e(TAG, "stopScan: ");
//        }
        if (mScheduledExecutorService != null) {
            mCancelFlag = false;
            mScheduledExecutorService.shutdownNow();
            mScheduledExecutorService = null;
            Log.e(TAG, "stopScan: ");
        }
        mHandler.sendEmptyMessage(SWITCH_INPUT);
    }

    public void close() {
//        stopScan();
//        if (mExecutorService != null) {
//            mCancelFlag = false;
//            mExecutorService.shutdownNow();
//            mExecutorService = null;
//            Log.e(TAG, "stopScan: ");
//        }
        if (mScheduledExecutorService != null) {
            mCancelFlag = false;
            mScheduledExecutorService.shutdownNow();
            mScheduledExecutorService = null;
            Log.e(TAG, "stopScan: ");
        }
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
//            try {
//                new SerialPort(11, 115200);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

}
