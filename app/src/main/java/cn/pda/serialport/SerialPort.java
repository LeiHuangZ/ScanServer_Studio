/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.pda.serialport;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author LeiHuang
 */
public class SerialPort {

    /**
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private volatile boolean mTrigOn = false;

    public SerialPort(){}

    public SerialPort(int port, int baudRate) throws SecurityException, IOException {
        mFd = open(port, baudRate);
        if (mFd == null) {
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    public void power5vOn() {
        zigbeepoweron();
    }

    public void power5vOff() {
        zigbeepoweroff();
    }

    public void power3v3On() {
        power3v3on();
    }

    public void power3v3Off() {
        power3v3off();
    }

    public void powerRfidOn() {
        rfidPoweron();
    }

    public void powerRfidOff() {
        rfidPoweroff();
    }

    public void powerPsamOn() {
        psampoweron();
    }

    public void powerPsamOff() {
        psampoweroff();
    }

    public void powerScannerOn() {
        scanerpoweron();
//        scannerTrigOff();
    }

    public void powerScannerOff() {
        scanerpoweroff();
    }

    public void scannerTrigOn() {
        scanertrigeron();
        mTrigOn = true;
    }

    public void scannerTrigOff() {
        scanertrigeroff();
        mTrigOn = false;
    }

//    public boolean scannerTrigState() {
//        return mTrigOn;
//    }

    public void closePort(int port) {
        close(port);
    }

    private native static FileDescriptor open(int port, int baudrate);

    private native static FileDescriptor open(int port, int baudrate, int portparity);

    private native void close(int port);

    private native void zigbeepoweron();

    private native void zigbeepoweroff();

    private native void scanerpoweron();

    private native void scanerpoweroff();

    private native void psampoweron();

    private native void psampoweroff();

    private native void scanertrigeron();

    private native void scanertrigeroff();

    private native void power3v3on();

    private native void power3v3off();

    private native void rfidPoweron();

    private native void rfidPoweroff();

    private native void usbOTGpowerOn();

    private native void usbOTGpowerOff();

    private native void irdapoweron();

    private native void irdapoweroff();

    private native void test(byte[] bytes);

    static {
        System.loadLibrary("devapi");
        System.loadLibrary("irdaSerialPort");
    }

}
