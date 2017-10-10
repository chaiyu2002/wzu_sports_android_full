package com.tim.app.ui.activity.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.application.library.net.JsonResponseCallback;
import com.application.library.util.PackageUtil;
import com.tim.app.R;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.ui.activity.AboutActivity;
import com.tim.app.ui.activity.ToolbarActivity;
import com.tim.app.util.DownloadAppUtils;

import org.json.JSONObject;

public class SettingActivity extends ToolbarActivity {

    private static final String TAG = "SettingActivity";
    private TextView tvVersionName;
    private RelativeLayout rlAboutUS;
    private RelativeLayout rlCheckUpdate;

    /**
     * 启动设置界面的统一接口
     *
     * @param context
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, SettingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    public void initView() {
        rlAboutUS = (RelativeLayout) findViewById(R.id.rlAboutUS);
        rlCheckUpdate = (RelativeLayout) findViewById(R.id.rlCheckUpdate);
        tvVersionName = (TextView) findViewById(R.id.tvVersionName);

        rlAboutUS.setOnClickListener(this);
        rlCheckUpdate.setOnClickListener(this);
    }

    @Override
    public void initData() {
        setTitle(getString(R.string.app_system_setting));
        tvVersionName.setText("当前版本：" + PackageUtil.getVersionName(this));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.rlAboutUS) {
            AboutActivity.start(this);
        } else if (v.getId() == R.id.rlCheckUpdate) {
            ServerInterface.instance().queryAppVersion(new JsonResponseCallback() {
                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    if (errCode == 0) {
                        try {
                            JSONObject latestVersion = json.getJSONObject("data").getJSONObject("latestVerison");
                            final String versionName = latestVersion.getString("versionName");
                            final int versionCode = latestVersion.getInt("versionCode");
                            final String changeLog = latestVersion.getString("changeLog");
                            final String apkUrl = latestVersion.getString("downloadUrl");
                            final boolean isForced = latestVersion.getBoolean("isForced");

                            PackageManager manager = (SettingActivity.this).getPackageManager();
                            PackageInfo info = manager.getPackageInfo(SettingActivity.this.getPackageName(), 0);

                            if (versionCode > info.versionCode) {

                                final AlertDialog.Builder builder =
                                        new AlertDialog.Builder(SettingActivity.this);
                                AlertDialog dialog;
                                builder.setTitle("版本升级");
                                builder.setPositiveButton("确认",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                DownloadAppUtils.downloadForAutoInstall(SettingActivity.this, apkUrl, "下载新版本");
                                                //
                                                // if (NetUtil.isWifi(SettingActivity.this)) {
                                                //     DownloadAppUtils.downloadForAutoInstall(SettingActivity.this, apkUrl, "下载新版本");
                                                // } else if (NetUtil.isMobile(SettingActivity.this)) {
                                                //
                                                // }
                                            }
                                        });
                                builder.setMessage(changeLog.replace("\\n", " \n"));

                                if (isForced) {
                                    builder.setCancelable(false);
                                    //对话框不变化
                                } else {
                                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                }

                                dialog = builder.create();
                                dialog.show();
                            } else {
                                Toast.makeText(SettingActivity.this, getString(R.string.prompt_no_update), Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        } catch (org.json.JSONException e) {
                            e.printStackTrace();
                            return false;
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        // TODO 网络出现问题？该接口出现问题？
                        return false;
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
