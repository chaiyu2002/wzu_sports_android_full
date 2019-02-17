package com.tim.app.ui.activity;

import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.application.library.net.JsonResponseCallback;
import com.application.library.util.SmoothSwitchScreenUtil;
import com.tim.app.R;
import com.tim.app.server.api.ServerInterface;

import org.json.JSONObject;

import static com.tim.app.constant.AppConstant.student;

/**
 * 体测数据
 */
public class BodyCheckDataActivity extends BaseActivity {

    private static final String TAG = "BodyCheckDataActivity";

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ImageButton ibBack;
    private TextView tvHeight;
    private TextView tvWeight;
    private TextView tvVitalCapacity;
    private TextView tvBMI;
    // private int studentId = 2;

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_body_check_data;
    }

    @Override
    public void initView() {
        ibBack = (ImageButton) findViewById(R.id.ibBack);
        tvHeight = (TextView) findViewById(R.id.tvHeight);
        tvWeight = (TextView) findViewById(R.id.tvWeight);
        tvVitalCapacity = (TextView) findViewById(R.id.tvVitalCapacity);
        tvBMI = (TextView) findViewById(R.id.tvBMI);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.dl_body_test_data_drawer);
        mNavigationView = (NavigationView) findViewById(R.id.nv_body_test_data_navigation);


        //从服务器去的学生信息，计算显示的学期数
        Menu menu = mNavigationView.getMenu();
        for (int i = 1; i <= 8; i++) {
            if (i % 2 == 0) {
                menu.add(Menu.NONE, i, i, "2016年~2017年 第二学期");
            } else {
                menu.add(Menu.NONE, i, i, "2016年~2017年 第一学期");
            }
        }
        if (mNavigationView != null) {
            mNavigationView.setNavigationItemSelectedListener(
                    new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(MenuItem menuItem) {
                            menuItem.setChecked(true);
                            mDrawerLayout.closeDrawers();
                            //                            switch (menuItem.getItemId()) {
                            //                                case R.id.nav_term_one:
                            //                                    ToastUtil.showToast("第一学期");
                            //                                    break;
                            //                                case R.id.nav_term_two:
                            //                                    ToastUtil.showToast("第二学期");
                            //                                    break;
                            //                                case R.id.nav_term_three:
                            //                                    ToastUtil.showToast("第三学期");
                            //                                    break;
                            //                                case R.id.nav_term_four:
                            //                                    ToastUtil.showToast("第四学期");
                            //                                    break;
                            //                                case R.id.nav_term_five:
                            //                                    ToastUtil.showToast("第五学期");
                            //                                    break;
                            //                                case R.id.nav_term_six:
                            //                                    ToastUtil.showToast("第六学期");
                            //                                    break;
                            //                                case R.id.nav_term_seven:
                            //                                    ToastUtil.showToast("第七学期");
                            //                                    break;
                            //                                case R.id.nav_term_eight:
                            //                                    ToastUtil.showToast("第八学期");
                            //                                    break;
                            //                            }
                            return true;
                        }
                    });
            mNavigationView.setItemIconTintList(null);
        }


        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        });


        ibBack.setOnClickListener(this);
    }


    @Override
    public void initData() {
        ServerInterface.instance().queryFitnessCheckData(student.getId(), new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                if (errCode == 0) {
                    try {
                        tvHeight.setText(json.optJSONObject("data").optJSONObject("student")
                                .optJSONArray("fitnessCheckDatas").getJSONObject(0).getString("height") + "m");
                        tvWeight.setText(json.optJSONObject("data").optJSONObject("student")
                                .optJSONArray("fitnessCheckDatas").getJSONObject(0).getString("weight") + "kg");
                        tvVitalCapacity.setText(json.optJSONObject("data").optJSONObject("student")
                                .optJSONArray("fitnessCheckDatas").getJSONObject(0).getString("lungCapacity") + "cm");
                        tvBMI.setText(json.optJSONObject("data").optJSONObject("student")
                                .optJSONArray("fitnessCheckDatas").getJSONObject(0).getString("bmi"));
                        return true;
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "queryCurTermData onJsonResponse e: ");
                        return false;
                    }
                } else {
                    return false;
                }
            }

        });
        //        tvHeight.setText("169cm");
        //        tvWeight.setText("69KG");
        //        tvVitalCapacity.setText("2000cc");
        //        tvBMI.setText("23");
    }

    @Override
    protected void onResume() {
        super.onResume();
        SmoothSwitchScreenUtil.smoothSwitchScreen(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibBack:
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


}
