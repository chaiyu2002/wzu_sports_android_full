package com.tim.app.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.application.library.log.DLOG;
import com.application.library.net.JsonResponseCallback;
import com.application.library.runtime.ActivityManager;
import com.application.library.util.SmoothSwitchScreenUtil;
import com.application.library.widget.EmptyLayout;
import com.application.library.widget.recycle.BaseRecyclerAdapter;
import com.application.library.widget.recycle.HorizontalDividerItemDecoration;
import com.application.library.widget.recycle.WrapRecyclerView;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tim.app.R;
import com.tim.app.RT;
import com.tim.app.constant.AppConstant;
import com.tim.app.constant.AppStatusConstant;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.server.entry.SportEntry;
import com.tim.app.server.entry.User;
import com.tim.app.ui.activity.setting.SettingActivity;
import com.tim.app.ui.adapter.SportAdapter;
import com.tim.app.ui.cell.GlideApp;
import com.tim.app.ui.view.HomepageHeadView;
import com.tim.app.ui.view.webview.WebViewActivity;
import com.tim.app.util.DownloadAppUtils;
import com.tim.app.util.MathUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.tim.app.constant.AppConstant.SPEED_SCALE;
import static com.tim.app.constant.AppConstant.student;
import static com.tim.app.constant.AppConstant.user;

/**
 * 首页
 */
public class MainActivity extends BaseActivity implements BaseRecyclerAdapter.OnItemClickListener, View.OnClickListener {

    private static final String TAG = "MainActivity";
    public static final int SPORT_BACKGROUND_WIDTH = 1080;
    public static final int SPORT_BACKGROUND_HEIGHT = 465;
    public static final String APP_ID = "wx24a2c72c638267d7";
    public static final String APP_USER_NAME = "gh_2a8433042436";

    //    public static User user;
    //    public static Student student;
    private static boolean mIsEnable = true;

    private MainActivity context;

    private long last_back_time = 0;
    private DrawerLayout mDrawerLayout;

    private FrameLayout flMenu;
    private FrameLayout frMenu;
    //    private ImageView ibNotify;
    //    private TextView tvLogout;
    private TextView tvUserName;
    private ImageView ivAvatar;

    private LinearLayout llContainer;
    private WrapRecyclerView wrvSportType;

    private SportAdapter adapter;
    //    private BadNetworkAdapter badNetworkAdapter;
    private List<SportEntry> sportEntryDataList;
    //    private List<BadNetWork> networkDataList;

    private EmptyLayout emptyLayout;

    private NavigationView navigationView;

    private HomepageHeadView homepageHeadView;
    //    private BadNetworkView badNetworkView;

    /*
    * 微信
    * */
    //    private IWXAPI api;
    //
    //    private void regToWx() {
    //        api = WXAPIFactory.createWXAPI(this, AppConstant.APP_ID);
    //        api.registerApp(AppConstant.APP_ID);
    //    }

    /**
     * 发送数据到微信
     * <p>
     * //
     */
    //    private void sendToWx(String text) {
    //        WXTextObject textObj = new WXTextObject();
    //        textObj.text = text;
    //
    //        WXMediaMessage msg = new WXMediaMessage();
    //        msg.mediaObject = textObj;
    //        msg.description = text;
    //
    //        SendMessageToWX.Req req = new SendMessageToWX.Req();
    //        req.transaction = String.valueOf(System.currentTimeMillis());
    //
    //        api.sendReq(req);
    //    }
    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    //TODO https://juejin.im/entry/582180a3bf22ec0068e2285d
    @Override
    protected void restartApp() {
        //        Toast.makeText(getApplicationContext(), "应用被回收重启走流程", Toast.LENGTH_LONG).show();
        //        Log.d(TAG, "应用被回收重启走流程");
        startActivity(new Intent(this, SplashActivity.class));
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int action = intent.getIntExtra(AppStatusConstant.KEY_HOME_ACTION, AppStatusConstant.ACTION_BACK_TO_HOME);
        switch (action) {
            case AppStatusConstant.ACTION_RESTART_APP:
                restartApp();
                break;
            case AppStatusConstant.ACTION_KICK_OUT:
                break;
            case AppStatusConstant.ACTION_BACK_TO_HOME:
                break;
        }
    }

    private boolean isOpen;

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
        SmoothSwitchScreenUtil.smoothSwitchScreen(this);
        context = this;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }


    @Override
    protected void init(Bundle savedInstanceState) {
        queryAppVersion();
    }

    @Override
    public void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.dl_main_drawer);
        flMenu = (FrameLayout) findViewById(R.id.flTitleMenu);
        flMenu.setOnClickListener(this);
        flMenu.setVisibility(View.VISIBLE);
        frMenu = (FrameLayout) findViewById(R.id.frTitleMenu);
        frMenu.setOnClickListener(this);
        //        ibNotify = (ImageView) findViewById(ibNotify);
        //        tvLogout = (TextView) findViewById(tvLogout);
        //        badNetworkView = (BadNetworkView) findViewById(R.id.bnvContainer);
        //        ibMenu.setOnClickListener(this);
        //        ibNotify.setOnClickListener(this);
        //        tvLogout.setOnClickListener(this);

        navigationView =
                (NavigationView) findViewById(R.id.nv_main_navigation);

        llContainer = (LinearLayout) findViewById(R.id.llContainer);
        wrvSportType = (WrapRecyclerView) findViewById(R.id.wrvSportType);

        emptyLayout = new EmptyLayout(this, llContainer);
        emptyLayout.showLoading();
        emptyLayout.setEmptyButtonShow(true);
        emptyLayout.setErrorButtonShow(true);
        emptyLayout.setEmptyDrawable(R.drawable.ic_empty_sport_data);
        emptyLayout.setErrorDrawable(R.drawable.ic_empty_sport_data);
        emptyLayout.setEmptyText("当前没有数据");
        emptyLayout.setEmptyButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emptyLayout.showLoading();
                queryHomePagedata();
            }
        });

        if (navigationView != null) {
            //动态加载headerView
            View headerView = navigationView.inflateHeaderView(R.layout.navigation_header);
            tvUserName = (TextView) headerView.findViewById(R.id.tvUserName);
            ivAvatar = (ImageView) headerView.findViewById(R.id.ivAvatar);
            navigationView.setNavigationItemSelectedListener(
                    new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(MenuItem menuItem) {
                            menuItem.setChecked(true);
                            //                            mDrawerLayout.closeDrawers();
                            switch (menuItem.getItemId()) {
                                case R.id.nav_rank:
                                    Intent intentRank = new Intent(MainActivity.this, SchoolRankingActivity.class);
                                    startActivity(intentRank);
                                    break;
                                case R.id.nav_survey://历史数据概况
                                    Intent intent = new Intent(MainActivity.this, HistorySportActivity.class);
                                    startActivity(intent);
                                    break;
                                //// TODO: 2017/8/17  
                                //                                case R.id.nav_fitness_test://体测数据
                                //                                    Intent intentBodyTestData = new Intent(MainActivity.this, BodyCheckDataActivity.class);
                                //                                    startActivity(intentBodyTestData);
                                //                                    break;
                                //                                case R.id.nav_sports_achievement://体育成绩
                                //                                    Intent intentScore = new Intent(MainActivity.this, SportsScoreActivity.class);
                                //                                    startActivity(intentScore);
                                //                                    break;
                                //                                case R.id.nav_approval://审批
                                //                                    break;
                                case R.id.nav_help://帮助
                                    WebViewActivity.loadUrl(MainActivity.this, "http://www.guangyangyundong.com:86/#/help", "帮助中心");
                                    break;
                                case R.id.nav_set://设置
                                     Intent intentSetting = new Intent(MainActivity.this, SettingActivity.class);
                                     startActivity(intentSetting);
                                    break;
                            }
                            return true;
                        }
                    });
            navigationView.setItemIconTintList(null);
        }
        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                isOpen = false;
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                isOpen = true;
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        wrvSportType.setLayoutManager(layoutManager);
        wrvSportType.addItemDecoration(new HorizontalDividerItemDecoration.Builder(
                MainActivity.this).color(getResources().getColor(R.color.transparent)).size((int) (RT.getDensity() * 2)).build());

        homepageHeadView = (HomepageHeadView) LayoutInflater.from(this).inflate(R.layout.homepage_head_view, null);
        wrvSportType.addHeaderView(homepageHeadView);
        //        badNetworkView = (BadNetworkView) LayoutInflater.from(this).inflate(R.layout.bad_network_view, null);
        /**
         * 添加底部留白
         */
        View footerView = new View(this);
        footerView.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, (int) RT.getDensity() * 50));
        footerView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        wrvSportType.addFootView(footerView);

        sportEntryDataList = new ArrayList<>();
        adapter = new SportAdapter(this, sportEntryDataList);
        wrvSportType.setAdapter(adapter);

        //        networkDataList = new ArrayList<>();
        //        networkDataList.add(new BadNetWork());
        //        badNetworkAdapter = new BadNetworkAdapter(this, networkDataList);
        //        wrvSportType.invalidate();
    }


    /**
     * 首页底部运动方式Item点击事件
     *
     * @param view
     * @param position within the adapter's data set.  在适配器中数据集中的位置。
     * @param id
     */
    @Override
    public void onItemClick(View view, int position, long id) {
        SportEntry sportEntry = sportEntryDataList.get(position);

        // this block is not work!
        //        if (BadNetworkAdapter.BAD_NETWORK.equals(view.getTag())) {
        //            Log.d(TAG, "onItemClick: bad network!");
        //            queryRunningSport();
        //        }

        DLOG.d(TAG, "position:" + position);
        DLOG.d(TAG, "sportEntry:" + sportEntry);
        SharedPreferences sharedPreferences = getSharedPreferences(User.USER_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        Boolean isFirstLaunch = sharedPreferences.getBoolean(AppConstant.IS_FIRST_LAUNCH, true);
        if (sportEntry.getType() == SportEntry.RUNNING_SPORT) {
            if (isFirstLaunch) {
                showPermissionDialog();
                sharedPreferences.edit().putBoolean(AppConstant.IS_FIRST_LAUNCH,false).apply();
            } else {
                SportDetailActivity.start(this, sportEntry);
            }
        } else {
            if (isFirstLaunch) {
                showPermissionDialog();
                sharedPreferences.edit().putBoolean(AppConstant.IS_FIRST_LAUNCH,false).apply();
            } else {
                SportsAreaListActivity.start(this, sportEntry);
            }
        }
    }

    public void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("重要提示")
                .setMessage("请授予应用定位权限，应用才可以正常工作~")
                .setPositiveButton("去授权", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //引导用户至设置页手动授权
                        Toast.makeText(MainActivity.this, "请授予应用定位权限~", Toast.LENGTH_SHORT).show();
                        Intent intent1 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                        intent1.setData(uri);
                        startActivity(intent1);
                    }
                })
                .setCancelable(false).show();
    }

    /**
     * 查询首页底部运动方式
     */
    public void queryRunningSport() {
        ServerInterface.instance().queryRunningSports(student.getUniversityId(), mIsEnable, student.isMan(), new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                if (errCode == 0) {
                    try {
                        sportEntryDataList.clear();

                        JSONArray runningSportArray = json.getJSONObject("data").getJSONArray("runningSports");
                        for (int i = 0; i < runningSportArray.length(); i++) {
                            JSONObject jsonObject = runningSportArray.getJSONObject(i);
                            SportEntry sportEntry = new SportEntry();

                            if (!jsonObject.optBoolean("isEnabled")) {
                                continue;
                            }

                            int runningSportId = jsonObject.getInt("id");
                            sportEntry.setId(runningSportId);

                            int participantNum = jsonObject.getInt("participantNum");
                            sportEntry.setParticipantNum(participantNum);

                            int interval = jsonObject.getInt("acquisitionInterval");
                            sportEntry.setAcquisitionInterval(interval);

                            int distance = jsonObject.getInt("qualifiedDistance");
                            sportEntry.setQualifiedDistance(distance);

                            int stepThreshold = jsonObject.getInt("stepThreshold");
                            DLOG.d(TAG, "stepThreshold:" + stepThreshold);
                            sportEntry.setStepThreshold(stepThreshold);

                            double time = jsonObject.getDouble("qualifiedCostTime");
                            BigDecimal targetSpeed = MathUtil.bigDecimalDivide(Double.toString(distance),
                                    Double.toString(time), SPEED_SCALE);
                            sportEntry.setTargetSpeed(targetSpeed.toString());

                            sportEntry.setType(SportEntry.RUNNING_SPORT);
                            sportEntry.setName(jsonObject.getString("name"));
                            sportEntry.setBgDrawableId(R.drawable.ic_bg_run);
                            sportEntry.setImgUrl(jsonObject.getString("imgUrl"));

                            BigDecimal targetTime = MathUtil.bigDecimalDivide(Double.toString(time),
                                    Double.toString(60), SPEED_SCALE);
                            sportEntry.setTargetTime(Integer.valueOf(targetTime.toBigInteger().intValue()));

                            sportEntryDataList.add(sportEntry);
                        }
                        // wrvSportType.setAdapter(adapter);
                        adapter.setOnItemClickListener(context);
                        adapter.notifyDataSetChanged();
                        if (sportEntryDataList.size() == 0) {
                            emptyLayout.showEmpty();
                        } else {
                            emptyLayout.showContent();
                        }
                    } catch (JSONException e) {
                        emptyLayout.showError();
                        e.printStackTrace();
                    }
                    //在查询跑步项目之后调用。
                    queryAreaSport();
                    return true;
                } else {
                    //TODO
                    emptyLayout.showEmptyOrError(errCode);
                    DLOG.d(TAG, "获取跑步运动项目失败 错误码：" + errCode);
                    return false;
                }
            }

        });
    }

    /**
     * 查询首页顶部本学期运动记录
     */
    public void queryCurTermData() {
        ServerInterface.instance().queryCurTermData(student.getUniversityId(), student.getId(), new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                if (errCode == 0) {
                    try {
                        JSONObject jsonObject = json.getJSONObject("data").getJSONObject("student");
                        String curTermTargetTimes = String.valueOf(json.getJSONObject("data").getJSONObject("university")
                                .getJSONObject("currentTerm").getJSONObject("termSportsTask")
                                .getString("targetSportsTimes"));

                        String curTermAreaCounts = jsonObject.getString("currentTermAreaActivityCount");
                        String curTermRunningCounts = jsonObject.getString("currentTermRunningActivityCount");

                        //                        String curTermAreaQualifiedCounts = jsonObject.optString("currentTermQualifiedAreaActivityCount");
                        //                        String curTermRunningQualifiedCounts = jsonObject.optString("currentTermQualifiedRunningActivityCount");

                        String curTermAreaCostedTime = jsonObject.getString("areaActivityTimeCosted");
                        String curTermRunningCostedTime = jsonObject.getString("runningActivityTimeCosted");

                        String curTermAreaKcalConsumption = jsonObject.getString("areaActivityKcalConsumption");
                        String curTermRunningKcalConsumption = jsonObject.getString("runningActivityKcalConsumption");


                        String totalCount = String.valueOf(Integer.valueOf(curTermAreaCounts) + Integer.valueOf(curTermRunningCounts));
                        //                        String totalQualifiedCount = String.valueOf(Integer.valueOf(curTermAreaQualifiedCounts) + Integer.valueOf(curTermRunningQualifiedCounts));
                        String totalSignInCount = String.valueOf(jsonObject.getInt("signInCount"));
                        String totalKcalComsuption = String.valueOf(Integer.valueOf(curTermAreaKcalConsumption) + Integer.valueOf(curTermRunningKcalConsumption));
                        double totalCostedTime = Double.valueOf(curTermAreaCostedTime) + Double.valueOf(curTermRunningCostedTime);


                        totalCostedTime = totalCostedTime / 60;
                        BigDecimal bd = new BigDecimal(totalCostedTime);
                        bd = bd.setScale(1, RoundingMode.HALF_UP);
                        homepageHeadView.setData(totalCount, totalKcalComsuption, String.valueOf(bd.intValue()), totalSignInCount, curTermTargetTimes);
                        homepageHeadView.displayNormalLayout();
                        //                        adapter.notifyDataSetChanged();
                        return true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        emptyLayout.showError();
                        return false;
                    }
                } else {
                    emptyLayout.showEmptyOrError(errCode);
                    return false;
                }
            }

        });
    }


    /**
     * 获取区域运动项目
     */
    public void queryAreaSport() {
        //        mAreaSportEntryList = new ArrayList<>();
        ServerInterface.instance().queryAreaSport(student.getUniversityId(), new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                SportEntry areaSportEntry = new SportEntry();
                if (errCode == 0) {
                    try {
                        JSONArray areaSportArray = json.getJSONObject("data").getJSONArray("areaSports");
                        DLOG.d(TAG, "areaSportArray.length():" + areaSportArray.length());
                        for (int i = 0; i < areaSportArray.length(); i++) {
                            JSONObject jsonObject = areaSportArray.getJSONObject(i);

                            if (!jsonObject.getBoolean("isEnabled")) {
                                continue;
                            }

                            areaSportEntry.setId(jsonObject.getInt("id"));
                            areaSportEntry.setName(jsonObject.getString("name"));
                            areaSportEntry.setType(SportEntry.AREA_SPORT);
                            //                        areaSportEntry.setEnable(jsonObject.optBoolean("isEnable"));
                            areaSportEntry.setTargetTime(jsonObject.getInt("qualifiedCostTime"));
                            areaSportEntry.setAcquisitionInterval(jsonObject.getInt("acquisitionInterval"));
                            areaSportEntry.setParticipantNum(jsonObject.getInt("participantNum"));

                            areaSportEntry.setImgUrl(jsonObject.getString("imgUrl"));
                            areaSportEntry.setBgDrawableId(R.drawable.ic_bg_area);

                            sportEntryDataList.add(areaSportEntry);

                            if (sportEntryDataList.size() == 0) {
                                emptyLayout.showEmpty();
                            } else {
                                emptyLayout.showContent();
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        emptyLayout.showError();
                        e.printStackTrace();
                    }
                    return true;
                } else {
                    //TODO
                    emptyLayout.showEmptyOrError(errCode);
                    DLOG.d(TAG, "获取区域运动项目失败 错误码：" + errCode);
                    return false;
                }
            }
        });
    }


    public void queryAppVersion() {
        ServerInterface.instance().queryAppVersion(new JsonResponseCallback() {
            private JSONObject latestVersion;

            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                //                String versionName = "";
                //                int versionCode;
                //                String changeLog = "";
                //                String apkUrl = "";
                //                boolean isForced = false;
                if (errCode == 0) {
                    try {
                        latestVersion = json.getJSONObject("data").getJSONObject("latestVerison");
                        final String versionName = latestVersion.getString("versionName");
                        final int versionCode = latestVersion.getInt("versionCode");
                        final String changeLog = latestVersion.getString("changeLog");
                        final String downloadUrl = latestVersion.getString("downloadUrl");
                        final boolean isForced = latestVersion.getBoolean("isForced");
                        final int platformId = latestVersion.getInt("platformId");

                        PackageManager manager = context.getPackageManager();
                        PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

                        SharedPreferences sp = getSharedPreferences(User.USER_UPDATE_PREFERENCE, Context.MODE_PRIVATE);
                        int ignoreVersion = sp.getInt(User.IGNORE_VERSION, 0);
                        DLOG.d(TAG, "ignoreVersion:" + ignoreVersion);
                        if (ignoreVersion == versionCode) {
                            return false;
                        }

                        DLOG.d(TAG, "服务器版本" + versionCode);
                        DLOG.d(TAG, "客户端版本" + info.versionCode);
                        if (versionCode > info.versionCode) {

                            final AlertDialog.Builder builder =
                                    new AlertDialog.Builder(context);

                            builder.setTitle("版本升级");
                            builder.setMessage(changeLog.replace("\\n", " \n"));
                            builder.setPositiveButton("升级",
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            DownloadAppUtils.downloadForAutoInstall(context, downloadUrl, "下载新版本");
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
                                        DownloadAppUtils.downloadForAutoInstall(context, downloadUrl, "下载新版本");
                                        Toast.makeText(context, "开始下载新版本", Toast.LENGTH_SHORT).show();
                                        // 这里可以控制是否让对话框消失
                                        // dialog.dismiss();
                                    }
                                });
                            }

                        } else {
                            // TODO no update
                            // queryHomePagedata();
                        }

                        return true;
                    } catch (JSONException e) {
                        // TODO if check update gave an error
                        e.printStackTrace();
                        return false;
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    // TODO if network is bad
                    // queryHomePagedata();
                    return false;
                }
            }
        });
    }

    @Override
    public void initData() {
        tvUserName.setText(user.getStudent().getName());

        if (!TextUtils.isEmpty(user.getAvatarUrl())) {
            GlideApp.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar)// while a resource is loading.
                    .error(R.drawable.ic_default_avatar) // if a load fails.
                    .fallback(R.drawable.ic_default_avatar) // If a fallback is not set, null models will cause the error drawable to be displayed. If the error drawable is not set, the placeholder will be displayed.
                    //                .fitCenter()
                    //                .centerCrop()
                    //                .miniThumb(50)
                    .circleCrop()
                    .transition(withCrossFade())
                    .into(ivAvatar);
        } else {
            // ignore it
        }
    }

    private void queryHomePagedata() {
        queryCurTermData();
        queryRunningSport();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SmoothSwitchScreenUtil.smoothSwitchScreen(this);
        queryHomePagedata();
        wrvSportType.smoothScrollToPosition(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.flTitleMenu:
                mDrawerLayout.openDrawer(Gravity.LEFT);
                break;
            case R.id.frTitleMenu:
                IWXAPI api = WXAPIFactory.createWXAPI(MainActivity.this, APP_ID, false);
                WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
                req.userName = APP_USER_NAME;
                api.sendReq(req);
                break;
            // case tvLogout:
            //     startActivity(new Intent(this,LoginActivity.class));
            //     finish();
            //     break;
            // case R.id.llBadNetworkFresh:
            //     queryCurTermData();
            //     Log.d(TAG, "onClick llBadNetworkFresh");
            //     break;
            // case R.id.llBadNetworkContainer:
            //     queryRunningSport();
            //     Log.d(TAG, "onClick llBadNetworkContainer");
            //     break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isOpen) {
            mDrawerLayout.closeDrawers();
            return;
        }
        long current_time = System.currentTimeMillis();
        if (current_time - last_back_time > 2000) {
            Toast.makeText(context, getString(R.string.app_exit), Toast.LENGTH_SHORT).show();
            last_back_time = current_time;
        } else {
            ActivityManager.ins().AppExit(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
