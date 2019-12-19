package cn.pda.scan;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.pda.scan1dserver.LogUtils;
import com.pda.scan1dserver.MainActivity;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.pda.serialport.SerialPort;

/**
 * @author LeiHuang
 */
public class ScanThread extends Thread {

    private final String TAG = "Huang," + ScanThread.class.getSimpleName();

    private ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private ExecutorService mExecutorService;

    private SerialPort mSerialPort;
    public static int port = 0;
    private InputStream is;
    private OutputStream os;

    private Handler mHandler;

    private volatile boolean mRunFlag;

    public static int SCAN = 1001;
    public static int SWITCH_INPUT = 1002;

    /**
     * if throw exception, serialport initialize fail.
     */
    public ScanThread(Handler handler) throws SecurityException, IOException {
        mHandler = handler;
        int baudRate = 9600;
        mSerialPort = new SerialPort(port, baudRate);
        is = mSerialPort.getInputStream();
        os = mSerialPort.getOutputStream();
        mSerialPort.powerScannerOn();
    }

    @Override
    public void run() {
        try {
            mRunFlag = true;
            int size;
            byte[] buffer = new byte[4096];
            int available;
            EventBus.getDefault().post(MainActivity.FLAG_OPEN_SUCCESS);
            while (!isInterrupted() && mRunFlag) {
                available = is.available();
                if (available > 0) {
                    size = is.read(buffer);
                    if (size > 0) {
                        stopScan();
                        sendMessage(buffer, size, SCAN);
                    }
                }
                Thread.sleep(30);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        super.run();
    }

    private void sendMessage(byte[] data, int dataLen, int mode) {
        String dataStr = "";
        if (data != null && dataLen > 0) {
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
            if (mSerialPort.scannerTrigState()) {
                mSerialPort.scannerTrigOff();
                Thread.sleep(30);
                return;
            }
            mSerialPort.scannerTrigOn();
            mExecutorService = new ThreadPoolExecutor(1, 200, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());
            mExecutorService.submit(mCancelScanTask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable mCancelScanTask = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(3000);
                if (mSerialPort.scannerTrigState()) {
                    stopScan();
                    sendMessage(null, 0, SWITCH_INPUT);
                    LogUtils.e(TAG, "mCancelScanTask switch input");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void stopScan() {
        if (mSerialPort.scannerTrigState()) {
            mSerialPort.scannerTrigOff();
        }
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
            mExecutorService = null;
        }
    }

    public void close() {
        stopScan();
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
        }
    }

}
