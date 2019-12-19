package com.pda.scan1dserver;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import cn.pda.scan.ScanThread;

public class MainActivity extends Activity {

    private LinearLayout mMainContainer;
    private ScanConfig scanConfig;
    private Switch openSwitch;
    private Spinner spinnerPrefix;
    private Spinner spinnerSurfix;
    private Spinner spinnerEnconding;
    private CheckBox checkVoice;
    private CheckBox checkF1;
    private CheckBox checkF2;
    private CheckBox checkF3;
    private CheckBox checkF4;

    private CheckBox checkF5;
    private CheckBox checkF6;
    private CheckBox checkF7;
    private TextView tvPrefix;
    private TextView tvSurfix;

    private String tabStr;
    private String spaceStr;
    private String enterStr;
    private String noneStr;
    private String otherStr;
    private String tabAndEnterStr;
    private String[] fixArray;
    private String[] encodingArray;
    private String mUtf8Str = "UTF-8";
    private String mGbkStr = "GBK";
    private String mShiftJisStr = "Shift_JIS";
    //	private String[] fixArray2;
    private boolean mCreatePurSelect = true;
    private boolean mCreateSurSelect = true;
    private boolean mCreateEncodSelect = true;

    private Dialog dialogLoading;
    private Dialog dialogOther;

    /**
     * 开启标签
     */
    public static final String FLAG_OPEN_SUCCESS = "open_success";
    public static final String FLAG_OPEN_FAIL = "open_fail";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(getAppVersionName(this));
        scanConfig = new ScanConfig(this);
        initView();
    }

    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            // ---get the package info---
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return context.getString(R.string.app_name) + "(" + ScanThread.port + ")-v" + versionName;
    }

    private void initView() {
        mMainContainer = (LinearLayout) findViewById(R.id.main_container);
        tabStr = getResources().getString(R.string.tab);
        spaceStr = getResources().getString(R.string.space);
        enterStr = getResources().getString(R.string.enter);
        noneStr = getResources().getString(R.string.none);
        otherStr = getResources().getString(R.string.other);
        tabAndEnterStr = getResources().getString(R.string.tabAndEnter);
        fixArray = new String[]{tabStr, spaceStr, enterStr, noneStr, otherStr, tabAndEnterStr};
        encodingArray = new String[]{mUtf8Str, mGbkStr, mShiftJisStr};
//		fixArray2 = new String[] { "\t", " ", "0A0D", "", "\t0A0D"};
        tvPrefix = (TextView) findViewById(R.id.text_prefix);
        tvSurfix = (TextView) findViewById(R.id.text_surfix);

        openSwitch = (Switch) findViewById(R.id.switch_scan);
        spinnerPrefix = (Spinner) findViewById(R.id.spinner_prefix);
        spinnerSurfix = (Spinner) findViewById(R.id.spinner_surfix);
        spinnerEnconding = (Spinner) findViewById(R.id.spinner_encoding);
        checkVoice = (CheckBox) findViewById(R.id.checkBox_voice);
        checkF1 = (CheckBox) findViewById(R.id.checkBox_f1);
        checkF2 = (CheckBox) findViewById(R.id.checkBox_f2);
        checkF3 = (CheckBox) findViewById(R.id.checkBox_f3);
        checkF4 = (CheckBox) findViewById(R.id.checkBox_f4);
        checkF5 = (CheckBox) findViewById(R.id.checkBox_f5);
        checkF6 = (CheckBox) findViewById(R.id.checkBox_f6);
        checkF7 = (CheckBox) findViewById(R.id.checkBox_f7);

//		String surfix = scanConfig.getSurfix();
//		String prefix = scanConfig.getPrefix();
        int surfixIndex = scanConfig.getSurfixIndex();
        int prefixIndex = scanConfig.getPrefixIndex();
        int encodingIndex = scanConfig.getEncodingIndex();
//		for (int i = 0; i < fixArray2.length; i++) {
//			if (surfix.equals(fixArray2[i])) {
//				surfixIndex = i;
//			}
//			if (prefix.equals(fixArray2[i])) {
//				prefixIndex = i;
//			}
//		}
//		if (surfixIndex == 3 && !surfix.equals("")) {
//			surfixIndex = 4;
//		}
//		if (prefixIndex == 3 && !prefix.equals("")) {
//			prefixIndex = 4;
//		}
        spinnerPrefix.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, fixArray));
        spinnerPrefix.setSelection(prefixIndex);
        if (prefixIndex == 4) {
            tvPrefix.setText(scanConfig.getPrefix());
        }
        spinnerSurfix.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, fixArray));
        spinnerSurfix.setSelection(surfixIndex);
        if (surfixIndex == 4) {
            tvSurfix.setText(scanConfig.getSurfix());
        }
        spinnerEnconding.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, encodingArray));
        spinnerEnconding.setSelection(encodingIndex);
        // set key
        checkF1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setF1(isChecked);
            }
        });

        checkF2.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setF2(isChecked);
            }
        });
        checkF3.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setF3(isChecked);
            }
        });
        checkF4.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setF4(isChecked);
            }
        });

        checkF5.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setF5(isChecked);
            }
        });
        checkF6.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setF6(isChecked);
            }
        });
        checkF7.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setF7(isChecked);
            }
        });
        checkVoice.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scanConfig.setVoice(isChecked);
            }
        });

        // open dev
        openSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // open scan
                if (isChecked) {
                    boolean open = scanConfig.isOpen();
                    if (open) {
                        return;
                    }
                    createLoaddingDialog();
                    Intent toService = new Intent(MainActivity.this, Scan1DService.class);
                    toService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startService(toService);
                } else {
                    Intent toKill = new Intent();
                    toKill.setAction("android.rfid.KILL_SERVER");
                    toKill.putExtra("kill", true);
                    sendBroadcast(toKill);
                }
                scanConfig.setOpen(isChecked);

            }
        });

        // prefix
        spinnerPrefix.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mCreatePurSelect) {
                    mCreatePurSelect = false;
                    return;
                }
                // try {
                // Field field = AdapterView.class.getDeclaredField("mOldSelectedPosition");
                // field.setAccessible(true); //设置mOldSelectedPosition可访问
                // field.setInt(spinnerPrefix, AdapterView.INVALID_POSITION);
                // //设置mOldSelectedPosition的值
                // } catch (Exception e) {
                // e.printStackTrace();
                // }
                if (fixArray[position].equals(tabStr)) {
                    scanConfig.setPrefix("\t");
                    scanConfig.setPrefixIndex(position);
                    tvPrefix.setText("");
                } else if (fixArray[position].equals(spaceStr)) {
                    scanConfig.setPrefix(" ");
                    tvPrefix.setText("");
                    scanConfig.setPrefixIndex(position);
                } else if (fixArray[position].equals(enterStr)) {
                    scanConfig.setPrefix("0A0D");
                    tvPrefix.setText("");
                    scanConfig.setPrefixIndex(position);
                } else if (fixArray[position].equals(noneStr)) {
                    scanConfig.setPrefix("");
                    tvPrefix.setText("");
                    scanConfig.setPrefixIndex(position);
                } else if (fixArray[position].equals(tabAndEnterStr)) {
                    scanConfig.setPrefix("\t0A0D");
                    tvPrefix.setText("");
                    scanConfig.setPrefixIndex(position);
                } else if (fixArray[position].equals(otherStr)) {
                    // dialog input prefix char
                    createOtherDialog(position, true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                LogUtils.e("nothing selected", "");

            }
        });

        // surfix
        spinnerSurfix.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mCreateSurSelect) {
                    mCreateSurSelect = false;
                    return;
                }
                if (fixArray[position].equals(tabStr)) {
                    scanConfig.setSurfix("\t");
                    scanConfig.setSurfixIndex(position);
                    tvSurfix.setText("");
                } else if (fixArray[position].equals(spaceStr)) {
                    scanConfig.setSurfix(" ");
                    scanConfig.setSurfixIndex(position);
                    tvSurfix.setText("");
                } else if (fixArray[position].equals(enterStr)) {
                    scanConfig.setSurfix("0A0D");
                    scanConfig.setSurfixIndex(position);
                    tvSurfix.setText("");
                } else if (fixArray[position].equals(noneStr)) {
                    scanConfig.setSurfix("");
                    scanConfig.setSurfixIndex(position);
                    tvSurfix.setText("");
                } else if (fixArray[position].equals(tabAndEnterStr)) {
                    scanConfig.setSurfix("\t0A0D");
                    scanConfig.setSurfixIndex(position);
                    tvSurfix.setText("");
                } else if (fixArray[position].equals(otherStr)) {
                    // dialog input surfix char
                    createOtherDialog(position, false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinnerEnconding.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mCreateEncodSelect) {
                    mCreateEncodSelect = false;
                    return;
                }
                switch (position) {
                    case 0:
                        scanConfig.setEncoding("UTF-8");
                        break;
                    case 1:
                        scanConfig.setEncoding("GBK");
                        break;
                    case 2:
                        scanConfig.setEncoding("Shift_JIS");
                        break;
                    default:
                        break;
                }
                scanConfig.setEncodingIndex(position);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        openSwitch.setChecked(scanConfig.isOpen());
        checkF1.setChecked(scanConfig.isF1());
        checkF2.setChecked(scanConfig.isF2());
        checkF3.setChecked(scanConfig.isF3());
        checkF4.setChecked(scanConfig.isF4());
        checkF5.setChecked(scanConfig.isF5());
        checkF6.setChecked(scanConfig.isF6());
        checkF7.setChecked(scanConfig.isF7());
        checkVoice.setChecked(scanConfig.isVoice());

    }

    @Override
    protected void onStart() {
        // 注册 EventBus
        // 判断 Eventbus 是否注册
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        // 注销操作
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final String result) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (FLAG_OPEN_SUCCESS.equals(result)) {
            showToast(getString(R.string.init_success));
        } else if (FLAG_OPEN_FAIL.equals(result)) {
            showToast(getString(R.string.init_success));
        }
        dialogLoading.cancel();
    }

    // show toast
    private Toast mToast;

    private void showToast(String content) {
        if (mToast == null) {
            mToast = Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(content);
        }
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    // create loading dialog
    private void createLoaddingDialog() {
        Builder builder = new Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, mMainContainer, false);
        builder.setView(view);
        dialogLoading = builder.create();
        dialogLoading.setCancelable(false);
        dialogLoading.show();
    }

    private EditText editUserChar;

    private void createOtherDialog(final int position, final boolean isPrefix) {
        // Input the customized Char
        Builder builder = new Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_others, mMainContainer, false);
        editUserChar = (EditText) view.findViewById(R.id.editText_others);
        builder.setView(view);
        builder.setTitle(getResources().getString(R.string.user_char));
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                int surfixIndex = scanConfig.getSurfixIndex();
                int prefixIndex = scanConfig.getPrefixIndex();
                spinnerSurfix.setSelection(surfixIndex);
                spinnerPrefix.setSelection(prefixIndex);
                dialogOther.cancel();
            }
        });
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userChar = editUserChar.getText().toString();
                if (isPrefix) {
                    tvPrefix.setText(userChar);
                    scanConfig.setPrefix(userChar);
                    scanConfig.setPrefixIndex(position);
                } else {
                    tvSurfix.setText(userChar);
                    scanConfig.setSurfix(userChar);
                    scanConfig.setSurfixIndex(position);
                }
                dialogOther.cancel();
            }
        });
        builder.setCancelable(false);

        dialogOther = builder.create();
        // dialogOther.setCancelable(false) ;
        dialogOther.show();
    }
}
