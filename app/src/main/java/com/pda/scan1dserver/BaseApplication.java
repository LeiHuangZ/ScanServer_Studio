package com.pda.scan1dserver;

import android.app.Application;

/**
 * description : DESCRIPTION
 * update : 2019/7/3 15:29,LeiHuang,DESCRIPTION
 *
 * @author : LeiHuang
 * @version : VERSION
 * @date : 2019/7/3 15:29
 */
public class BaseApplication extends Application {

    public static boolean mIsOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();
        BaseApplication.mIsOpen = new ScanConfig(this).isOpen();
    }
}
