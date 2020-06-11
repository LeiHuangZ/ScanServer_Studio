package com.pda.scan1dserver;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * description : DESCRIPTION
 * update : 2019/8/15 11:04,LeiHuang,DESCRIPTION
 *
 * @author : LeiHuang
 * @version : VERSION
 * @date : 2019/8/15 11:04
 */
public class UsbTils {
    private static final String TAG = "Huang," + UsbTils.class.getSimpleName();

    public static boolean isUsbOpen() {
        boolean isHigh = false;
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/devices/virtual/misc/mtgpio/pin");
            InputStream inputStream = process.getInputStream();
            OutputStream outputStream = process.getOutputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            String s = null;
            while ((s = dataInputStream.readLine()) != null) {
                //Log.e("Huang, MainMenu", "readLine = " + s);
                if (s.startsWith(" 46")) {
                    s = s.substring(s.indexOf(" 46"), s.indexOf(" 46") + 11);
                    isHigh = s.charAt(6) == '1' && s.charAt(7) == '1';
                    Log.e("Huang, MainMenu", "substring = " + s);
                } else if (s.startsWith("125")) {
                    s = s.substring(s.indexOf("125"), s.indexOf("125") + 11);
                    Log.e("Huang, MainMenu", "substring = " + s);
                    isHigh = s.charAt(6) == '1' && s.charAt(7) == '1';
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("Huang, MainMenu", "isHigh = " + isHigh);
        return isHigh;
    }

    public static boolean getGpioState(int gpio) {
        boolean isHigh = false;
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/devices/virtual/misc/mtgpio/pin");
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String gpioStr = getGpioStr(gpio);
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                s = s.replace("-", "");
                if (s.startsWith(gpioStr)) {
                    s = s.substring(s.indexOf(gpioStr), s.indexOf(gpioStr) + 11);
                    isHigh = s.charAt(6) == '1' && s.charAt(7) == '1';
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isHigh;
    }

    /**
     * 返回GPIO的标志值，大于等于100的，直接返回其String值，其他的返回一个或者两个空格加GPIO的String值
     *
     * @param gpio 待处理gpio口的int值
     * @return gpio的String标志值，读取状态的标志依据
     */
    private static String getGpioStr(int gpio) {
        int oneb = 9;
        int twob = 99;
        String s;
        if (gpio <= oneb) {
            s = "  " + gpio;
        } else if (gpio <= twob) {
            s = " " + gpio;
        } else {
            s = String.valueOf(gpio);
        }
        return s;
    }
}
