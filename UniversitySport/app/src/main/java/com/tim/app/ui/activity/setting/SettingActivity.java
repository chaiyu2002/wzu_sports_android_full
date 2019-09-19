package com.tim.app.ui.activity.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.application.library.log.DLOG;
import com.application.library.net.JsonResponseCallback;
import com.application.library.util.PackageUtil;
import com.tim.app.R;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.server.entry.User;
import com.tim.app.ui.activity.AboutActivity;
import com.tim.app.ui.activity.ToolbarActivity;
import com.tim.app.util.DownloadAppUtils;
import com.tim.app.util.NetworkUtil;
import com.tim.app.util.ToastUtil;

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
            if (!NetworkUtil.isNetworkConnected(SettingActivity.this)) {
                Toast.makeText(this, "请检查网络~", Toast.LENGTH_SHORT).show();
                return;
            }
            ServerInterface.instance().queryAppVersion(new JsonResponseCallback() {
                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    if (errCode == 0) {
                        try {
                            JSONObject latestVersion = json.getJSONObject("data").getJSONObject("latestVerison");
                            final String versionName = latestVersion.getString("versionName");
                            final int versionCode = latestVersion.getInt("versionCode");
                            final String changeLog = latestVersion.getString("changeLog");
                            final String downloadUrl = latestVersion.getString("downloadUrl");
                            final boolean isForced = latestVersion.getBoolean("isForced");

                            PackageManager manager = (SettingActivity.this).getPackageManager();
                            PackageInfo info = manager.getPackageInfo(SettingActivity.this.getPackageName(), 0);

                            SharedPreferences sp = getSharedPreferences(User.USER_UPDATE_PREFERENCE, Context.MODE_PRIVATE);
                            int ignoreVersion = sp.getInt(User.IGNORE_VERSION, 0);
                            DLOG.d(TAG, "ignoreVersion:" + ignoreVersion);
                            if (ignoreVersion == versionCode) {
                                ToastUtil.toast(SettingActivity.this, "已忽略本次更新");
                                return false;
                            }

                            if (versionCode > info.versionCode) {

                                final AlertDialog.Builder builder =
                                        new AlertDialog.Builder(SettingActivity.this);

                                builder.setTitle("版本升级");
                                builder.setMessage(changeLog.replace("\\n", " \n"));
                                builder.setPositiveButton("升级",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                DownloadAppUtils.downloadForAutoInstall(SettingActivity.this, downloadUrl, "下载新版本");
                                            }
                                        });

                                if (isForced) {//强制升级
                                    builder.setCancelable(false);
                                    //对话框不变化
                                } else {
                                    builder.setNegativeButton("暂不升级", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    builder.setNeutralButton("忽略本次", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            SharedPreferences.Editor sp = getSharedPreferences(User.USER_UPDATE_PREFERENCE, Context.MODE_PRIVATE).edit();
                                            sp.putInt(User.IGNORE_VERSION, versionCode);
                                            sp.apply();
                                        }
                                    });
                                }

                                AlertDialog dialog = builder.create();
                                dialog.show();

                                if (isForced) {
                                    // 重写“确定”（AlertDialog.BUTTON_POSITIVE），截取监听
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            DownloadAppUtils.downloadForAutoInstall(SettingActivity.this, downloadUrl, "下载新版本");
                                            Toast.makeText(SettingActivity.this, "开始下载新版本", Toast.LENGTH_SHORT).show();
                                            // 这里可以控制是否让对话框消失
                                            // dialog.dismiss();
                                        }
                                    });
                                }
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
