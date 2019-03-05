package com.tim.app.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.application.library.log.DLOG;
import com.lzy.okhttputils.OkHttpUtils;
import com.tim.app.R;
import com.tim.app.constant.AppStatusConstant;
import com.tim.app.constant.AppStatusManager;
import com.tim.app.server.entry.User;
import com.tim.app.server.logic.UserManager;
import com.tim.app.util.ViewGT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static com.tim.app.constant.AppConstant.student;
import static com.tim.app.constant.AppConstant.user;

public class SplashActivity extends BaseActivity {

    private static final String TAG = "SplashActivity";

    private static final int MESSAGE_WHAT_GOTO_MAINACTIVITY = 0X01;

    private static int DELAY_TIME = 500;

    private Button btLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppStatusManager.getInstance().setAppStatus(AppStatusConstant.STATUS_NORMAL); //进入应用初始化设置成未登录状态
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
        //        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    public void initView() {
        btLogin = (Button) findViewById(R.id.btLogin);
        btLogin.setOnClickListener(this);
    }

    @Override
    public void initData() {

        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
            UserManager.instance().cleanCache();
        }
        user = getUserFromCache();
        if (user != null) {
            showLoadingDialog();

            student = user.getStudent();
            MainActivity.start(this);
            finish();
        }
    }


    private User getUserFromCache() {
        User user = null;
        SharedPreferences sp = getSharedPreferences(User.USER_SHARED_PREFERENCE, MODE_PRIVATE);
        String temp = sp.getString(User.USER, null);
        if (temp == null) {
            return null;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(temp.getBytes(), Base64.DEFAULT));
        try {
            ObjectInputStream ois = new ObjectInputStream(bais);
            user = (User) ois.readObject();

            DLOG.d(TAG, "user: " + user);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return user;
    }

    public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x01;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                UserManager.instance().cleanCache();
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btLogin:
                ViewGT.gotoLoginActivity(this);
                break;
        }

    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WHAT_GOTO_MAINACTIVITY:
                    if (UserManager.instance().isLogin()) {
                        Intent main_intent = new Intent(SplashActivity.this, MainActivity.class);
                        main_intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                        startActivity(main_intent);
                        finish();
                    }
                    break;
            }
        }
    };

    private void handleIntent() {
        mHandler.sendEmptyMessageDelayed(MESSAGE_WHAT_GOTO_MAINACTIVITY, DELAY_TIME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntent();
        hideLoadingDialog();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        OkHttpUtils.getInstance().cancelTag(TAG);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        hideLoadingDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
