package com.tim.app.ui.activity;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.AppOpsManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.application.library.log.DLOG;
import com.application.library.net.JsonResponseCallback;
import com.application.library.net.ResponseCallback;
import com.application.library.util.NetUtils;
import com.lzy.okhttputils.OkHttpUtils;
import com.tim.app.R;
import com.tim.app.constant.AppConstant;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.server.entry.HistoryRunningSportEntry;
import com.tim.app.server.entry.SportEntry;
import com.tim.app.server.logic.UserManager;
import com.tim.app.sport.SensorService;
import com.tim.app.ui.dialog.LocationDialog;
import com.tim.app.ui.dialog.ProgressDialog;
import com.tim.app.ui.view.SlideUnlockView;
import com.tim.app.ui.view.webview.WebViewActivity;
import com.tim.app.util.BrightnessUtil;
import com.tim.app.util.MathUtil;
import com.tim.app.util.SystemUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.tim.app.constant.AppConstant.SPEED_SCALE;
import static com.tim.app.constant.AppConstant.student;

/**
 * 跑步运动详情页
 */
public class SportDetailActivity extends BaseActivity implements AMap.OnMyLocationChangeListener, AMap.OnMapTouchListener {

    private static final String TAG = "SportDetailActivity";
    private Context context = this;

    //重要实体
    private SportEntry sportEntry;
    private HistoryRunningSportEntry historySportEntry;

    //    private FileOutputStream fos;
    //    private int counter = 0;

    //第三方
    private MapView mapView;
    private AMap aMap;
    private LatLng lastLatLng = null;
    private UiSettings uiSettings;
    private float zoomLevel = 17;//地图缩放级别，范围3-19,越大越精细
    private Location firstLocation;
    private int firstLocationType;

    //全局变量
    private int interval = 0;
    private int speedLimitation = 10;//米
    private int currentDistance = 0;
    private long elapseTime = 0;
    //    private long previousTime = 0;
    private int currentSteps = 0;  // 最终的步数
    private int stepCounter = 0;
    private int stepCountCal = 0;
    private int lastSteps = 0;
    private long startTime;//开始时间
    private long stopTime;//运动结束时间
    private int initSteps = 0;//初始化的步数
    private int initCalcSteps = 0;//初始化的步数
    private double distancePerStep = 0; //步幅
    private double stepPerSecond = 0; //步幅
    private int acquisitionTimes = 1;//两点相似次数
    private LocationManager locationManager;

    private TextView tvSportName;
    private TextView tvParticipantNum;
    private TextView tvCurrentDistance;
    private TextView tvAverSpeedLabel;
    private TextView tvElapseTime;
    private TextView tvAverSpeed;
    private TextView tvTargetDistance;
    private TextView tvTargetTime;
    private TextView tvTargetSpeedLabel;
    private TextView tvTargetSpeed;
    private TextView tvResult;//运动结果
    private LinearLayout llResult;//运动结果父容器
    private ImageView ivLocation;
    private ImageView ivHelp;
    private ImageView ivFinished;
    private TextView tvStepTitle;
    //    private TextView tvCurrentStep;//暂时注释，记得全部放开注释
    private LinearLayout llTargetContainer;

    private TextView tvRemainPower;

    private RelativeLayout rlRoot;
    private RelativeLayout rlBottom;
    private Button btStart;
    private LinearLayout llBottom;
    private Button btContinue;
    private Button btStop;
    private SlideUnlockView slideUnlockView;
    private LocationDialog locationDialog;
    private ProgressDialog progressDialog;

    private LinearLayout llCurrentInfo;
    private RelativeLayout rlCurConsumeEnergy;
    private TextView tvCurConsumeEnergy;
    private TextView tvPause;
    private LinearLayout llLacationHint;

    static final int STATE_NORMAL = 0;//初始状态
    static final int STATE_STARTED = 1;//已开始
    static final int STATE_PAUSE = 2;//暂停
    static final int STATE_END = 3;//结束
    static final int STATE_NETWORK_ERROR = 4;//网络原因结束
    private int state = STATE_NORMAL;
    // 定位提示最少显示 3 秒钟
    private static final int WARM_UP_TIME = 3000;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (locationDialog != null && locationDialog.isShowing()) {
                locationDialog.dismissCurrentDialog();
            }
        }
    };

//    private int screenOffTimeout; //屏幕超时时间
//    private int screenKeepLightTime;
//    private int brightness;
//    private boolean autoAdjustBrightness;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Runnable elapseTimeRunnable;
    private ScheduledFuture<?> timerHandler = null;
    private long timerInterval = 1000;

    private View rlAnimView;
    private View ivShowSportInfo;
    private View ivHideSportInfo;

    private Animation showAnimation;
    private Animation hideAnimation;

    private long targetFinishedTime;
    private int sportRecordId;

    private int pauseStateSteps = 0;
    private int pauseStateCalcSteps = 0;

    private LocationService.MyBinder myBinder = null;

    public static final String NETWORK_ERROR_MSG = "网络请求失败，请检查网络状态或稍后再试";

    public static final String COMMIT_FALIED_MSG = "网络错误，数据提交失败，请随后查看历史记录";

    public static final String COMMIT_AGAIN_MSG = "请检查网络设置，并到开旷地点重试";

    public int commitTimes = 0;

    public static final String OPEN_GPS_MSG = "需要打开GPS才能开始运动...";


    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DLOG.d(TAG, "onServiceConnected name: " + name + ", service: " + service);
            myBinder = (LocationService.MyBinder) service;
            myBinder.startLocationInService(interval);
        }
    };
    private AlertDialog openLocationServiceDialog;

    public static void start(Context context, SportEntry sportEntry) {
        Intent intent = new Intent(context, SportDetailActivity.class);
        intent.putExtra("sportEntry", sportEntry);
        context.startActivity(intent);
    }


    private void startTimer() {
        timerHandler = scheduler.scheduleAtFixedRate(elapseTimeRunnable, 0, timerInterval, TimeUnit.MILLISECONDS);

    }

    private void stopTimer() {
        if (timerHandler != null) {
            timerHandler.cancel(true);
            scheduler.shutdown();
            timerHandler = null;
        }
    }

    private void initGPS() {
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS模块是否开启，如果没有则开启
        if (!locationManager
                .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            buildLocationServiceDialog();
        } else {
            locationDialog.show();
        }
    }

    private void buildLocationServiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重要提示");
        builder.setMessage("请打开定位服务，并授予定位权限~");
        builder.setPositiveButton("去设置",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 0);
                    }
                });

        openLocationServiceDialog = builder.create();
        openLocationServiceDialog.setCancelable(false);
        openLocationServiceDialog.show();
        openLocationServiceDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    openLocationServiceDialog.dismiss();
                    finish();
                }
                return false;
            }
        });
        openLocationServiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent, 0);
                // dialog.dismiss();
            }
        });

        TextView message = (TextView) openLocationServiceDialog.findViewById(android.R.id.message);
        Button positiveButton = (Button) openLocationServiceDialog.findViewById(android.R.id.button1);
        positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        if (requestCode == 0) {
            if (locationManager
                    .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                openLocationServiceDialog.dismiss();
                locationDialog.show();
            } else {
                // Toast.makeText(this, OPEN_GPS_MSG, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getAppDetailSettingIntent(Context context) {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        startActivity(localIntent);
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        initLocationDislog();

        initGPS();

        checkLocationPermissonNew();    //检查定位权限

        Float level = getBatteryLevel();
        tvRemainPower = (TextView) locationDialog.findViewById(R.id.tvRemainPower);
        tvRemainPower.setText(getResources().getString(R.string.remainPower, String.valueOf(level.intValue())));

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        sportEntry = (SportEntry) getIntent().getSerializableExtra("sportEntry");
        interval = sportEntry.getAcquisitionInterval() * 1000;

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写，创建地图
        initMap();

        SensorService.setThresholdValue(sportEntry.getStepThreshold());
        SensorService.setSportDetailActivity(this);
        startService(new Intent(this, SensorService.class));
        // EventManager.ins().registListener(EventTag.ON_STEP_CHANGE, eventListener);//三个参数的构造函数
        // EventManager.ins().registListener(EventTag.ON_ACCELERATION_CHANGE, eventListener);//三个参数的构造函数

        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("type", "跑步");
        startService(intent);
        //startService(new Intent(this, LocationService.class));

        //        DisplayMetrics displayMetrics = new DisplayMetrics();
        //        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //        hide_anim height = displayMetrics.heightPixels;
        //        hide_anim width = displayMetrics.widthPixels;

        aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                //                String text = "缩放比例发生变化，当前地图的缩放级别为: " + cameraPosition.zoom;
                //                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {

            }
        });

        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                //                String text = "当前地图的缩放级别为: " + aMap.getCameraPosition().zoom;
                //                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                //                text = "调整屏幕缩放比例：" + zoomLevel;
                //                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        registerReceiver(lowBatteryReceiver, filter);
    }

    private void initLocationDislog() {
        locationDialog = new LocationDialog(this);
        locationDialog.setCancelable(false);
        locationDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    locationDialog.dismissCurrentDialog();
                    finish();
                }
                return false;
            }
        });
    }

    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        aMap.setOnMyLocationChangeListener(this);
        uiSettings = aMap.getUiSettings();
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        setupLocationStyle();
        aMap.getUiSettings().setMyLocationButtonEnabled(false);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位`蓝点并不进行定位，默认是false。
        aMap.setOnMapTouchListener(this);
    }

    @Override
    public void onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                turnUpScreen();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
    }

    private void turnUpScreen() {

        // DLOG.d(TAG, "BrightnessUtil.getScreenBrightness(getWindow())" + BrightnessUtil.getScreenBrightness(getWindow()));
        boolean needToAdjustBrightness = Float.compare(BrightnessUtil.getScreenBrightness(getWindow()), 0.1f) == 0;

//        if (needToAdjustBrightness) {
//            if (BrightnessUtil.isAutoAdjustBrightness(this)) {
//                BrightnessUtil.setScreenBrightness(this, brightness);
//            } else {
//                BrightnessUtil.setScreenBrightness(this, brightness);
//            }
//        }
//
//        screenKeepLightTime = 0;
    }

    private void setupLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。
        myLocationStyle.interval(interval);
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
                fromResource(R.drawable.navi_map_gps_locked));
        aMap.setMyLocationStyle(myLocationStyle);
    }

    @Override
    public void onMyLocationChange(Location location) {
        DLOG.d(TAG, "onMyLocationChange location: " + location);
        DLOG.openInternalFile(this);

        String toastText = "";
        int errorCode = -1;
        String errorInfo = "";
        int locationType = -1;
        LatLng newLatLng;
        Boolean isNormal = true;

        Bundle bundle = location.getExtras();
        if (bundle != null) {
            errorCode = bundle.getInt(MyLocationStyle.ERROR_CODE);
            errorInfo = bundle.getString(MyLocationStyle.ERROR_INFO);
            // 定位类型，可能为GPS WIFI等，具体可以参考官网的定位SDK介绍
            locationType = bundle.getInt(MyLocationStyle.LOCATION_TYPE);
        }

        //屏幕到了锁屏的时间，调暗亮度
        WindowManager.LayoutParams params = getWindow().getAttributes();
        //screenKeepLightTime += interval / 1000;
        // DLOG.d(TAG, "params.screenBrightness: " + params.screenBrightness);
        // DLOG.d(TAG, "screenKeepLightTime:" + screenKeepLightTime);
        // DLOG.d(TAG, "screenOffTimeout:" + screenOffTimeout);
//        if (screenOffTimeout <= screenKeepLightTime && Float.compare(params.screenBrightness, 0.1f) != 0) {
//            params.screenBrightness = (float) 0.1;
//            getWindow().setAttributes(params);
//            DLOG.d(TAG, "onMyLocationChange turn down light");
//        }

        DLOG.d(TAG, "locationType:" + locationType);
        if (location != null) {
            //定位成功
            if (errorCode != 0) {
                String errText = "正在定位中，GPS信号弱";
                Toast.makeText(this, errText, Toast.LENGTH_SHORT).show();
                return;
            } else {
                newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                DLOG.d(TAG, "newLatLng: " + newLatLng);
                DLOG.d(TAG, "定位成功");
                // locationDialog.dismissCurrentDialog();

                // 判断第一次，第一次会提示
                if (lastLatLng == null) {
                    String errText = "定位成功";
                    firstLocation = location;
                    firstLocationType = locationType;
                    llLacationHint.setVisibility(View.GONE);
                    Toast.makeText(this, errText, Toast.LENGTH_SHORT).show();
                    handler.postDelayed(runnable, WARM_UP_TIME);
                    //locationDialog.dismissCurrentDialog();

                    // 待删除
                    //aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                    //toastText = "调整屏幕缩放比例：" + zoomLevel;
                    //Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

                    CameraUpdate cu = CameraUpdateFactory.newCameraPosition(new CameraPosition(newLatLng, zoomLevel, 0, 0));
                    aMap.moveCamera(cu);

                    btStart.setVisibility(View.VISIBLE);
                }
            }

            if (SensorService.stepCounterEnabled) {
                currentSteps = stepCounter;
            } else {
                currentSteps = stepCountCal;
            }

            if (state == STATE_STARTED) {
                String msg = location.toString();
                //   DLOG.writeToInternalFile(msg);

                float batteryLevel = getBatteryLevel();
                DLOG.d(TAG, "lastLatLng: " + lastLatLng);
                float distanceInterval = AMapUtils.calculateLineDistance(newLatLng, lastLatLng);

                // 如果采样间隔之间，没有步数的变化，stepsInterval就是零！ 会报 Infinity or NaN: Infinity 错误的！
                // int stepsInterval = stepCounter - lastSteps;
                int stepsInterval = currentSteps - lastSteps;
                BigDecimal bdDividend;
                BigDecimal bdDevisor;
                if (stepsInterval == 0) {
                    distancePerStep = 0;
                    stepPerSecond = 0;
                } else {
                    bdDividend = new BigDecimal(distanceInterval);
                    bdDevisor = new BigDecimal(stepsInterval);
                    distancePerStep = bdDividend.divide(bdDevisor, 2, RoundingMode.HALF_UP).doubleValue();

                    bdDividend = new BigDecimal(stepsInterval);
                    bdDevisor = new BigDecimal(sportEntry.getAcquisitionInterval());
                    stepPerSecond = bdDividend.divide(bdDevisor, 2, RoundingMode.HALF_UP).doubleValue();
                }

                // toastText = "绘制曲线，上一次坐标： " + lastLatLng + "， 新坐标：" + newLatLng
                //         + "， 本次移动距离： " + distanceInterval + "， 当前步数： " + stepCounter +
                //         "， 当前电量: " + batteryLevel + "%" + "locationType: " + locationType;
                // Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

                if (!lastLatLng.equals(newLatLng)) {
                    // 前后定位点位置不同
                    if (acquisitionTimes == 1) {
                        if (distanceInterval / sportEntry.getAcquisitionInterval() > speedLimitation) {
                            toastText = "异常移动，每秒位移：" + distanceInterval / sportEntry.getAcquisitionInterval();
                            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
                            isNormal = false;
                            drawLine(lastLatLng, newLatLng, isNormal);
                            // currentDistance += distanceInterval;
                        } else {
                            isNormal = true;
                            drawLine(lastLatLng, newLatLng, isNormal);
                            currentDistance += distanceInterval;

                            if (currentDistance > sportEntry.getQualifiedDistance() && targetFinishedTime == 0) {
                                targetFinishedTime = elapseTime;
                            }

                            tvCurrentDistance.setText(String.valueOf(currentDistance) + " ");
                            bdDividend = new BigDecimal(currentDistance);
                            bdDevisor = new BigDecimal(elapseTime);
                            BigDecimal bdResult = bdDividend.divide(bdDevisor, SPEED_SCALE, BigDecimal.ROUND_HALF_UP);
                            // 解决速度过大
                            if (bdResult.compareTo(new BigDecimal(10)) < 0) {
                                tvAverSpeed.setText(bdResult.toString() + " ");
                            }
                        }
                    } else {
                        // 累加采样间隔时间
                        if (distanceInterval / (acquisitionTimes * sportEntry.getAcquisitionInterval()) > speedLimitation) {
                            toastText = "异常移动，每秒位移：" + distanceInterval / sportEntry.getAcquisitionInterval();
                            Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
                            isNormal = false;
                            drawLine(lastLatLng, newLatLng, isNormal);
                            // currentDistance += distanceInterval;
                        } else {
                            isNormal = true;
                            drawLine(lastLatLng, newLatLng, isNormal);
                            currentDistance += distanceInterval;

                            if (currentDistance > sportEntry.getQualifiedDistance() && targetFinishedTime == 0) {
                                targetFinishedTime = elapseTime;
                            }

                            tvCurrentDistance.setText(String.valueOf(currentDistance) + " ");
                            bdDividend = new BigDecimal(currentDistance);
                            bdDevisor = new BigDecimal(elapseTime);
                            BigDecimal bdResult = bdDividend.divide(bdDevisor, SPEED_SCALE, BigDecimal.ROUND_HALF_UP);
                            // 解决速度过大
                            if (bdResult.compareTo(new BigDecimal(10)) < 0) {
                                tvAverSpeed.setText(bdResult.toString() + " ");
                            }
                        }
                        acquisitionTimes = 1;
                    }
                } else {
                    // 两次坐标点相同，记录次数（初始值为1）
                    DLOG.d(TAG, "两个点一样： newLatLng：" + newLatLng + "，lastLatLng：" + lastLatLng);
                    acquisitionTimes++;
                }

                // 提交到服务器
                ServerInterface.instance().runningActivityData(TAG, sportRecordId, currentSteps, stepCountCal, currentDistance,
                        location.getLongitude(), location.getLatitude(), String.valueOf(distancePerStep), String.valueOf(stepPerSecond),
                        locationType, isNormal, new ResponseCallback() {
                            @Override
                            public boolean onResponse(Object result, int status, String errmsg, int id, boolean fromcache) {
                                if (status == 0) {
                                    DLOG.d(TAG, "runningActivityData succeed");
                                    return true;
                                } else {
                                    String msg = "runningActivityData failed, errmsg: " + errmsg + "\r\n";
                                    msg += "net type: " + NetUtils.getNetWorkType(SportDetailActivity.this) + "\r\n";
                                    msg += "net connectivity is: " + NetUtils.isConnection(SportDetailActivity.this) + "\r\n";
                                    DLOG.writeToInternalFile(msg);
                                    return false;
                                }
                            }
                        });
            }

            // lastSteps = stepCounter;
            lastSteps = currentSteps;
            lastLatLng = newLatLng;
            DLOG.d(TAG, toastText);
        } else {
            String errText = "定位失败：" + errorInfo;
            DLOG.e(TAG, errText);
            Toast.makeText(this, errText, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    @Override
    public void initData() {

//        autoAdjustBrightness = BrightnessUtil.isAutoAdjustBrightness(context);
//        // DLOG.d(TAG, "autoAdjustBrightness:" + autoAdjustBrightness);
//        if (autoAdjustBrightness) {
//            brightness = BrightnessUtil.getScreenBrightness(this);
//            BrightnessUtil.stopAutoAdjustBrightness(context);
//            // DLOG.d(TAG, "brightness:" + brightness);
//        } else {
//            brightness = BrightnessUtil.getScreenBrightness(this);
//            // DLOG.d(TAG, "brightness:" + brightness);
//        }

        float batteryLevel = getBatteryLevel();
        BigDecimal bd = new BigDecimal(Float.toString(batteryLevel));
        Toast.makeText(this, "当前电量： " + bd.toBigInteger() + "%， 请及时充电，保持电量充足", Toast.LENGTH_LONG).show();
//        screenOffTimeout = android.provider.Settings.System.getInt(getContentResolver(),
//                Settings.System.SCREEN_OFF_TIMEOUT, 0) / 1000;

        if (!TextUtils.isEmpty(sportEntry.getName())) {
            tvSportName.setText(sportEntry.getName());
        }

        //        if (sportEntry.getParticipantNum() > 0) {
        tvParticipantNum.setText(getString(R.string.joinPrompt, String.valueOf(sportEntry.getParticipantNum())));
        //        }else{
        //            tvParticipantNum.setText(getString(R.string.joinPrompt, String.valueOf(sportEntry.getParticipantNum())));
        //        }

        if (sportEntry.getQualifiedDistance() > 0) {
            tvTargetDistance.setText(getString(R.string.digitalPlaceholder, String.valueOf(sportEntry.getQualifiedDistance())));
        }

        //        if (sportEntry.getTargetTime() > 0) {
        //            tvTargetTime.setText(String.valueOf(sportEntry.getTargetTime()));
        //        }

        //        tvTargetSpeedLabel.setText(getString(R.string.targetTitleSpeed));
        tvTargetSpeed.setText(getString(R.string.digitalPlaceholder, sportEntry.getTargetSpeed()) + " ");

        tvCurrentDistance.setText(getString(R.string.digitalPlaceholder, String.valueOf(currentDistance)) + " ");
        //        tvElapseTime.setText(String.valueOf(elapseTime / 60));
        //        tvCurrentStep.setText("0 步");
        tvAverSpeed.setText("0.00 ");
        initSteps = 0;
        stepCounter = 0;
        pauseStateSteps = 0;
        pauseStateCalcSteps = 0;
        currentDistance = 0;
        elapseTime = 0;

        //外部存储设备权限请求
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {// PackageManager.PERMISSION_DENIED
            UserManager.instance().cleanCache();
        }
        
        elapseTimeRunnable = new Runnable() {
            public void run() {
                // If you need update UI, simply do this:
                runOnUiThread(new Runnable() {
                    public void run() {
                        // update your UI component here.
                        elapseTime += timerInterval / 1000;
                        DLOG.d(TAG, "elapseTime: " + elapseTime);
                        String time = com.tim.app.util.TimeUtil.formatMillisTime(elapseTime * 1000);
                        tvElapseTime.setText(time);
                    }
                });
            }
        };

        // 设置滑动解锁-解锁的监听
        slideUnlockView.setOnUnLockListener(new SlideUnlockView.OnUnLockListener() {
            @Override
            public void setUnLocked(boolean unLock) {
                // 如果是true，证明解锁
                if (unLock) {
                    turnUpScreen();
                    // 重置一下滑动解锁的控件
                    slideUnlockView.reset();
                    // 让滑动解锁控件消失
                    slideUnlockView.setVisibility(View.GONE);
                    tvPause.setVisibility(View.GONE);

                    if (state == STATE_STARTED) {
                        state = STATE_END;
                        stopTimer();
                        //做保护
                        if (elapseTime != 0) {
                            BigDecimal bd = MathUtil.bigDecimalDivide(Double.toString(currentDistance),
                                    Double.toString(elapseTime), SPEED_SCALE);

                            tvAverSpeed.setText(bd.toString() + " ");
                        } else {
                            tvAverSpeed.setText("0.00 ");
                        }

                        tvParticipantNum.setVisibility(View.GONE);

                        progressDialog.show();
                        runningActivitiesEnd(targetFinishedTime);

                        myBinder.stopLocationInService();
                        aMap.setOnMyLocationChangeListener(null);
                        aMap.setMyLocationEnabled(false);
                    }
                }
            }
        });
    }

    /**
     * 组装地图截图和其他View截图，需要注意的是目前提供的方法限定为MapView与其他View在同一个ViewGroup下
     *
     * @param bitmap        地图截图回调返回的结果
     * @param viewContainer MapView和其他要截图的View所在的父容器ViewGroup
     * @param mapView       MapView控件
     * @param views         其他想要在截图中显示的控件
     */
    public static Bitmap getMapAndViewScreenShot(Bitmap bitmap, ViewGroup viewContainer, MapView mapView, View... views) {
        int width = viewContainer.getWidth();
        int height = viewContainer.getHeight();
        final Bitmap screenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenBitmap);
        canvas.drawBitmap(bitmap, mapView.getLeft(), mapView.getTop(), null);
        for (View view : views) {
            view.setDrawingCacheEnabled(true);
            canvas.drawBitmap(view.getDrawingCache(), view.getLeft(), view.getTop(), null);
        }

        return screenBitmap;
    }


    /**
     * 绘制两个坐标点之间的线段,从以前位置到现在位置
     */
    private void drawLine(LatLng oldData, LatLng newData, boolean isNormal) {
        // 绘制曲线
        if (isNormal) {
            // if (!isDrawYellow) {
                aMap.addPolyline((new PolylineOptions())
                        .add(oldData, newData)
                        .geodesic(true).color(Color.GREEN));
            // } else {
            //     isDrawYellow = false;
            //     aMap.addPolyline((new PolylineOptions())
            //             .add(oldData, newData)
            //             .geodesic(true).color(Color.YELLOW));
            // }
        } else {
            aMap.addPolyline((new PolylineOptions())
                    .add(oldData, newData)
                    .geodesic(true).color(Color.RED));
        }

    }

    public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x01;
    public static final int REQUEST_PERMISSION_WRITE_FINE_LOCATION = 0x02;
    //    public static final int WRITE_COARSE_LOCATION_REQUEST_CODE = 0x03;


    /**
     * @param requestCode
     * @param permissions
     * @param grantResults 记录的是授权的结果，如果申请了两个授权，那么数组的length就是2
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DLOG.d(TAG, "requestCode:" + requestCode);
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UserManager.instance().cleanCache();
                }
                break;
            case REQUEST_PERMISSION_WRITE_FINE_LOCATION:
                //                String permission = Manifest.permission.ACCESS_FINE_LOCATION;
                //                String op = AppOpsManagerCompat.permissionToOp(permission);
                //                int result = AppOpsManagerCompat.noteProxyOp(context, op, context.getPackageName());
                //                if (result == AppOpsManagerCompat.MODE_IGNORED
                //                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //                    DLOG.d("onRequestPermissionsResult", "onRequestPermissionsResult");
                //                    Toast.makeText(this,
                //                            getString(R.string.manual_open_permission_hint),
                //                            Toast.LENGTH_SHORT).show();
                //                }
                DLOG.d(TAG, "grantResults.length:" + grantResults.length);
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(SportDetailActivity.this, "您已授权~", Toast.LENGTH_SHORT).show();

                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(SportDetailActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Toast.makeText(SportDetailActivity.this, "必须要授权才能进行运动哦~", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showPermissionDialog();
                    }
                }
                break;
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
                        Toast.makeText(SportDetailActivity.this, "请授予应用定位权限~", Toast.LENGTH_SHORT).show();
                        Intent intent1 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                        intent1.setData(uri);
                        startActivity(intent1);
                        finish();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //引导用户手动授权，权限请求失败
                        Toast.makeText(SportDetailActivity.this, "必须要授权才能进行运动哦~", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).setCancelable(false).show();
    }

    public void checkLocationPermissonNew() {
        if (ContextCompat.checkSelfPermission(SportDetailActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(SportDetailActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_WRITE_FINE_LOCATION);
            DLOG.d(TAG, "没有授予");
        } else {
            DLOG.d(TAG, "授予了");
            // 小米手机5.0，无法正确获得是否授权
            if (SystemUtil.getRomType().equals(SystemUtil.SYS_MIUI)) {
                String permission = Manifest.permission.ACCESS_FINE_LOCATION;
                String op = AppOpsManagerCompat.permissionToOp(permission);
                int result = AppOpsManagerCompat.noteProxyOp(context, op, context.getPackageName());
                DLOG.d(TAG, "result:" + result);
                if (result == AppOpsManagerCompat.MODE_IGNORED) {
                    DLOG.d(TAG, "没有定位权限");
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (!testAppops()) {
                            handler.postDelayed(runnable, WARM_UP_TIME);
                            showPermissionDialog();
                        }
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_PERMISSION_WRITE_FINE_LOCATION);
                    }
                } else {
                    // 有权限或者默认。
                    DLOG.d(TAG, "ACCESS_FINE_LOCATION was GRANTED!");
                }
            }
        }
    }

    /**
     * checkOp：小米手机2 5.0，授权返回0，拒绝返回1
     *
     * @return
     */
    public boolean testAppops() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int checkOp = appOpsManager.checkOp(AppOpsManager.OPSTR_FINE_LOCATION, Binder.getCallingUid(), getPackageName());
            DLOG.d(TAG, "checkOp:" + checkOp);
            if (checkOp == AppOpsManager.MODE_IGNORED) {
                DLOG.d(TAG, "权限被拒绝了");
                return false;
            }
        }
        return true;
    }

    public boolean checkLocationPermission() {

        DLOG.d(TAG + "SystemRom: ", SystemUtil.getRomType());
        // 小米手机5.0权限
        if (SystemUtil.getRomType().equals(SystemUtil.SYS_MIUI)) {
            String permission = Manifest.permission.ACCESS_FINE_LOCATION;
            String op = AppOpsManagerCompat.permissionToOp(permission);
            int result = AppOpsManagerCompat.noteProxyOp(context, op, context.getPackageName());
            if (result == AppOpsManagerCompat.MODE_IGNORED
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                DLOG.d(TAG, "没有定位权限");
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this,
                            getString(R.string.manual_open_permission_hint),
                            Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_PERMISSION_WRITE_FINE_LOCATION);
                }
                return false;
            } else {
                // 有权限或者默认。
                DLOG.d(TAG, "ACCESS_FINE_LOCATION was GRANTED!");
                return true;
            }
        }
        // 判断定位服务开关
        if (!locationManager
                .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            buildLocationServiceDialog();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onClick(View v) {
        turnUpScreen();
        switch (v.getId()) {
            // case ibBack:
            //     finish();
            //     break;
            case R.id.btStart:

                DLOG.d(TAG, "ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION):" + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION));
                DLOG.d(TAG, "ActivityCompat.shouldShowRequestPermissionRationale(this," + ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION));

                // 先检查定位权限
                // if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                //         != PackageManager.PERMISSION_GRANTED) {
                //     if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                //             Manifest.permission.ACCESS_FINE_LOCATION)) {
                //         // Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
                //
                //     } else {
                //         ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                //                 REQUEST_PERMISSION_WRITE_FINE_LOCATION);
                //
                //     }
                // }

                // 没有定位权限，不能开始运动。
                if (!checkLocationPermission()) {
                    return;
                }

                if (state == STATE_NORMAL) {
                    DLOG.d(TAG, "sportEntry.getId():" + sportEntry.getId());
                    startTime = System.currentTimeMillis();
                    stepCountCal = 0;
                    stepCounter = 0;
                    ServerInterface.instance().runningActivitiesStart(TAG, sportEntry.getId(), student.getId(), startTime, new JsonResponseCallback() {
                        @Override
                        public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                            if (errCode == 0) {
                                try {
                                    sportRecordId = json.getInt("id");
                                    DLOG.d(TAG, "sportRecordId:" + sportRecordId);

                                    if (SensorService.stepCounterEnabled) {
                                        currentSteps = stepCounter;
                                    } else {
                                        currentSteps = stepCountCal;
                                    }

                                    //第一次向服务器提交数据,默认第一次是正常的数据
                                    ServerInterface.instance().runningActivityData(TAG, sportRecordId, currentSteps, stepCountCal, currentDistance,
                                            firstLocation.getLongitude(), firstLocation.getLatitude(), String.valueOf(distancePerStep), String.valueOf(stepPerSecond),
                                            firstLocationType, true, new ResponseCallback() {
                                                @Override
                                                public boolean onResponse(Object result, int status, String errmsg, int id, boolean fromcache) {
                                                    if (status == 0) {
                                                        DLOG.d(TAG, "第一次上传 runningActivityData 成功!");
                                                        state = STATE_STARTED;

                                                        llCurrentInfo.setVisibility(View.VISIBLE);
                                                        rlCurConsumeEnergy.setVisibility(View.GONE);
                                                        // btStart.setVisibility(View.GONE);
                                                        // rlBottom.setVisibility(View.GONE);
                                                        slideUnlockView.setVisibility(View.VISIBLE);
                                                        tvPause.setVisibility(View.VISIBLE);

                                                        progressDialog.dismissCurrentDialog();

                                                        initData();
                                                        startTimer();
                                                        Intent bindIntent = new Intent(SportDetailActivity.this, LocationService.class);
                                                        bindService(bindIntent, connection, BIND_AUTO_CREATE);
                                                        return true;
                                                    } else {
                                                        // String msg = "runningActivityData failed, errmsg: " + errmsg + "\r\n";
                                                        // msg += "net type: " + NetUtils.getNetWorkType(SportDetailActivity.this) + "\r\n";
                                                        // msg += "net connectivity is: " + NetUtils.isConnection(SportDetailActivity.this) + "\r\n";
                                                        // DLOG.writeToInternalFile(msg);
                                                        Toast.makeText(SportDetailActivity.this, NETWORK_ERROR_MSG, Toast.LENGTH_SHORT).show();
                                                        return false;
                                                    }
                                                }
                                            });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    btStart.setVisibility(View.VISIBLE);
                                    progressDialog.dismissCurrentDialog();
                                    Toast.makeText(SportDetailActivity.this, NETWORK_ERROR_MSG, Toast.LENGTH_SHORT).show();
                                    DLOG.d(TAG, "errMsg: " + errMsg);
                                }
                                return true;
                            } else {
                                btStart.setVisibility(View.VISIBLE);
                                progressDialog.dismissCurrentDialog();
                                Toast.makeText(SportDetailActivity.this, NETWORK_ERROR_MSG, Toast.LENGTH_SHORT).show();
                                DLOG.d(TAG, "errMsg: " + errMsg);
                                return false;
                            }
                        }
                    });

                    // 点击开始按钮后立即隐藏开始按钮
                    btStart.setVisibility(View.GONE);
                    progressDialog.show();

                } else if (state == STATE_END) {//运动结束时，查看锻炼结果
                    finish();
                    SportResultActivity.start(this, historySportEntry);
                } else if (state == STATE_NETWORK_ERROR) {
                    finish();
                    onBackPressed();
                }
                break;
            case R.id.ivLocation:
                //修改地图的中心点位置
                // CameraPosition cp = aMap.getCameraPosition();
                // CameraPosition cpNew = CameraPosition.fromLatLngZoom(lastLatLng, cp.zoom);
                // CameraUpdate cu = CameraUpdateFactory.newCameraPosition(cpNew);
                // aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                // aMap.moveCamera(cu);
                //点击定位图标 实现定位到当前位置
                CameraUpdate cu = CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLatLng, zoomLevel, 0, 0));
                aMap.moveCamera(cu);
                break;
            case R.id.ivShowSportInfo:
                DLOG.d(TAG, "ivShowSportInfo");
                // 指南针的位置要变化，UiSettings 中寻找方法
                if (null == showAnimation) {
                    showAnimation = AnimationUtils.loadAnimation(this, R.anim.show_anim);
                }
                ivShowSportInfo.setSelected(false);
                showAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        rlAnimView.setVisibility(View.VISIBLE);
                        ivShowSportInfo.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                rlAnimView.startAnimation(showAnimation);
                break;
            case R.id.ivHideSportInfo:
                DLOG.d(TAG, "ivHideSportInfo");
                if (null == hideAnimation) {
                    hideAnimation = AnimationUtils.loadAnimation(this, R.anim.hide_anim);
                }
                hideAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        rlAnimView.setVisibility(View.GONE);
                        ivShowSportInfo.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                rlAnimView.startAnimation(hideAnimation);
                break;
        }

    }

    /**
     * 提交运动数据
     */
    private void runningActivitiesEnd(final long targetFinishedTime) {
        //必须先初始化。
        //        SQLite.init(context, RunningSportsCallback.getInstance());
        DLOG.d(TAG, "runningActivitiesEnd");

        if (SensorService.stepCounterEnabled) {
            currentSteps = stepCounter;
        } else {
            currentSteps = stepCountCal;
        }

        //提交本次运动数据，更新UI
        ServerInterface.instance().runningActivitiesEnd(
                TAG, sportRecordId, currentDistance, currentSteps, elapseTime, targetFinishedTime, new JsonResponseCallback() {
                    @Override
                    public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                        DLOG.d(TAG, "errCode:" + errCode);
                        if (errCode == 0) {
                            try {
                                historySportEntry = new HistoryRunningSportEntry();

                                historySportEntry.setId(json.getInt("id"));
                                historySportEntry.setSportId(json.getInt("runningSportId"));
                                historySportEntry.setStudentId(json.getInt("studentId"));
                                historySportEntry.setDistance(json.getInt("distance"));
                                historySportEntry.setStepCount(json.getInt("stepCount"));
                                historySportEntry.setCostTime(json.getInt("costTime"));
                                historySportEntry.setSpeed(json.getInt("speed"));
                                historySportEntry.setStepPerSecond(json.getDouble("stepPerSecond"));
                                historySportEntry.setDistancePerStep(json.getInt("distancePerStep"));
                                historySportEntry.setTargetFinishedTime(json.getLong("targetFinishedTime"));
                                historySportEntry.setStartTime(json.getLong("startTime"));
                                historySportEntry.setKcalConsumed(json.getInt("kcalConsumed"));
                                historySportEntry.setQualified(json.getBoolean("qualified"));
                                historySportEntry.setValid(json.getBoolean("isValid"));
                                historySportEntry.setVerified(json.getBoolean("isVerified"));
                                historySportEntry.setQualifiedDistance(json.getInt("qualifiedDistance"));
                                historySportEntry.setQualifiedCostTime(json.getInt("qualifiedCostTime"));
                                historySportEntry.setMinCostTime(json.getLong("minCostTime"));
                                //                                historySportEntry.setCreatedAt(json.getLong("createdAt"));
                                //                                historySportEntry.setUpdatedAt(json.getLong("updatedAt"));
                                historySportEntry.setEndedAt(json.getLong("endedAt"));
                                //                                historySportEntry.setEndedBy(json.getBoolean("endedBy"));
                                historySportEntry.setType(AppConstant.RUNNING_TYPE);
                                progressDialog.dismissCurrentDialog();
                            } catch (org.json.JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(SportDetailActivity.this, COMMIT_FALIED_MSG, Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            rlBottom.setVisibility(View.VISIBLE);
                            llBottom.setVisibility(View.GONE);
                            btStart.setVisibility(View.VISIBLE);
                            btStart.setText("查看锻炼结果");

                            //非正常结束
                            if (historySportEntry.getEndedAt() == 0) {
                                tvResult.setText("未结束");
                                tvResult.setTextColor(Color.parseColor("#FF9800"));
                                ivHelp.setVisibility(View.VISIBLE);
                            } else {
                                //是否达标
                                if (historySportEntry.isQualified()) {
                                    //是否审核
                                    if (historySportEntry.isVerified()) {
                                        //是否有效
                                        if (historySportEntry.isValid()) {
                                            tvResult.setText("达标");
                                            tvResult.setTextColor(Color.parseColor("#4CAF50"));
                                            ivFinished.setVisibility(View.VISIBLE);
                                        } else {
                                            tvResult.setText("审核未通过");
                                            tvResult.setTextColor(Color.RED);
                                            ivHelp.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        tvResult.setText("达标待审核");
                                        tvResult.setTextColor(Color.parseColor("#4CAF50"));
                                        ivHelp.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    tvResult.setText("未达标");
                                    tvResult.setTextColor(Color.parseColor("#FF9800"));
                                    ivHelp.setVisibility(View.VISIBLE);
                                }
                            }

                            tvResult.setVisibility(View.VISIBLE);
                            llResult.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    WebViewActivity.loadUrl(SportDetailActivity.this, "http://www.guangyangyundong.com:86/#/help", "帮助中心");
                                    overridePendingTransition(R.anim.right_in, R.anim.left_out);
                                }
                            });

                            rlCurConsumeEnergy.setVisibility(View.VISIBLE);
                            tvCurConsumeEnergy.setText(getString(R.string.curConsumeEnergyTemp, String.valueOf(historySportEntry.getKcalConsumed())));
                            return true;

                        } else {
                            // 判断网络提交异常次数 <= 3次，提示用户再次提交
                            if (commitTimes++ < 3) {
                                showRetryDialog(context, COMMIT_AGAIN_MSG);
                                progressDialog.dismissCurrentDialog();
                                return false;
                            } else {
                                state = STATE_NETWORK_ERROR;
                                btStart.setVisibility(View.VISIBLE);
                                btStart.setText("返回首页");
                                progressDialog.dismissCurrentDialog();
                                Toast.makeText(context, COMMIT_FALIED_MSG, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        }
                    }
                });
    }

    public void showRetryDialog(Context context, String message) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_message, null);
        TextView confirm;    //确定按钮
        final TextView content;    //内容
        confirm = (TextView) view.findViewById(R.id.dialog_btn_confirm);
        confirm.setText("重试");
        content = (TextView) view.findViewById(R.id.dialog_txt_content);
        content.setText(message);
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runningActivitiesEnd(targetFinishedTime);
                progressDialog.show();
                dialog.dismiss();
            }
        });
        dialog.show();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int displayWidth = dm.widthPixels;
        int displayHeight = dm.heightPixels;
        android.view.WindowManager.LayoutParams p = dialog.getWindow().getAttributes();  //获取对话框当前的参数值
        p.width = (int) (displayWidth * 0.55);    //宽度设置为屏幕的0.5
        p.height = (int) (displayHeight * 0.28);    //宽度设置为屏幕的0.5
        dialog.setCanceledOnTouchOutside(false);// 设置点击屏幕Dialog不消失
        dialog.getWindow().setAttributes(p);     //设置生效
    }

    @Override
    public void onBackPressed() {
        if (state == STATE_STARTED || state == STATE_PAUSE) {
            Toast.makeText(this, "请先停止运动后，再点击返回键", Toast.LENGTH_LONG).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sport_detail;
    }

    @Override
    public void initView() {
        llLacationHint = (LinearLayout) findViewById(R.id.llLacationHint);
        //        ibBack = (ImageButton) findViewById(R.id.ibBack);
        //        ibBack.setOnClickListener(this);
        tvSportName = (TextView) findViewById(R.id.tvSportName);
        tvParticipantNum = (TextView) findViewById(R.id.tvParticipantNum);
        tvCurrentDistance = (TextView) findViewById(R.id.tv_current_distance);
        tvAverSpeedLabel = (TextView) findViewById(R.id.tvAverSpeedLabel);
        tvAverSpeed = (TextView) findViewById(R.id.tv_average_speed);
        tvTargetDistance = (TextView) findViewById(R.id.tvTargetDistance);
        //        tvTargetTime = (TextView) findViewById(R.id.tvTargetTime);
        tvElapseTime = (TextView) findViewById(R.id.tvElapsedTime);
        tvTargetSpeedLabel = (TextView) findViewById(R.id.tvTargetTitle);
        tvTargetSpeed = (TextView) findViewById(R.id.tvTargetValue);
        tvPause = (TextView) findViewById(R.id.tvPause);
        ivLocation = (ImageView) findViewById(R.id.ivLocation);
        ivHelp = (ImageView) findViewById(R.id.ivHelp);
        ivFinished = (ImageView) findViewById(R.id.ivFinished);
        slideUnlockView = (SlideUnlockView) findViewById(R.id.slideUnlockView);
        rlBottom = (RelativeLayout) findViewById(R.id.rlBottom);
        rlRoot = (RelativeLayout) findViewById(R.id.rlRoot);

        btStart = (Button) findViewById(R.id.btStart);
        btStart.setOnClickListener(this);
        //        btStart.setVisibility(View.VISIBLE);

        llBottom = (LinearLayout) findViewById(R.id.llBottom);
        btContinue = (Button) findViewById(R.id.btContinue);
        btStop = (Button) findViewById(R.id.btStop);
        tvResult = (TextView) findViewById(R.id.tvResult);
        llResult = (LinearLayout) findViewById(R.id.llResult);
        //        tvStepTitle = (TextView) findViewById(R.id.tvStepTitle);
        //        tvCurrentStep = (TextView) findViewById(R.id.tvCurrentStep);
        llTargetContainer = (LinearLayout) findViewById(R.id.llTargetContainer);

        llCurrentInfo = (LinearLayout) findViewById(R.id.llCurrentInfo);
        rlCurConsumeEnergy = (RelativeLayout) findViewById(R.id.rlCurConsumeEnergy);
        tvCurConsumeEnergy = (TextView) findViewById(R.id.tvCurConsumeEnergy);

        rlAnimView = findViewById(R.id.rlAnimView);
        ivShowSportInfo = findViewById(R.id.ivShowSportInfo);
        ivHideSportInfo = findViewById(R.id.ivHideSportInfo);


        btContinue.setOnClickListener(this);
        btStop.setOnClickListener(this);
        ivLocation.setOnClickListener(this);
        ivShowSportInfo.setOnClickListener(this);
        ivHideSportInfo.setOnClickListener(this);
    }

    public void onStepChange(int steps) {
        DLOG.d(TAG, "系统自带计步器：" + steps);
        if (state == STATE_STARTED) {
            // DLOG.d(TAG, "state: " + state);
            if (initSteps == 0) {
                initSteps = steps;
            } else {
                stepCounter = steps - initSteps - pauseStateSteps;
                // tvCurrentStep.setText(String.valueOf(stepCounter) + "步");
            }
        } else {
            if (initSteps != 0) {
                pauseStateSteps = steps - initSteps - stepCounter;
            }
        }
    }

    public void onStepCalcChange(int calcSteps) {
        DLOG.d(TAG, "自定义计步器：" + calcSteps);
        if (state == STATE_STARTED) {
            // DLOG.d(TAG, "state: " + state);
            if (initCalcSteps == 0) {
                initCalcSteps = calcSteps;
            } else {
                stepCountCal = calcSteps - initCalcSteps - pauseStateCalcSteps;
                // tvCurrentStep.setText(String.valueOf(stepCounter) + "步");
            }
        } else {
            if (initCalcSteps != 0) {
                pauseStateCalcSteps = calcSteps - initCalcSteps - stepCountCal;
            }
        }
    }

    /*
    EventListener eventListener = new EventListener() {
        @Override
        public void handleMessage(int what, int arg1, int arg2, Object dataobj) {
            switch (what) {
                case EventTag.ON_STEP_CHANGE:
                    int steps = (int) dataobj;
                    DLOG.d(TAG, "step counter: " + steps);
                    if (state == STATE_STARTED) {
                        // DLOG.d(TAG, "state: " + state);
                        if (initSteps == 0) {
                            initSteps = steps;
                        } else {
                            stepCounter = steps - initSteps - pauseStateSteps;
                            // tvCurrentStep.setText(String.valueOf(stepCounter) + "步");
                        }
                    } else {
                        if (initSteps != 0) {
                            pauseStateSteps = steps - initSteps - stepCounter;
                        }
                    }
                    break;
                case EventTag.ON_ACCELERATION_CHANGE:
                    int calcSteps = (int) dataobj;
                    DLOG.d(TAG, "calcSteps: " + calcSteps);
                    if (state == STATE_STARTED) {
                        // DLOG.d(TAG, "state: " + state);
                        if (initCalcSteps == 0) {
                            initCalcSteps = calcSteps;
                        } else {
                            stepCountCal = calcSteps - initCalcSteps - pauseStateCalcSteps;
                            // tvCurrentStep.setText(String.valueOf(stepCounter) + "步");
                        }
                    } else {
                        if (initCalcSteps != 0) {
                            pauseStateCalcSteps = calcSteps - initCalcSteps - stepCountCal;
                        }
                    }
                    // if (state == STATE_STARTED) {
                    //     if (initSteps == 0) {
                    //         initSteps = stepCountCal;
                    //     } else {
                    //         stepCounter = stepCountCal - initSteps - pauseStateSteps;
                    //         // tvCurrentStep.setText(String.valueOf(stepCounter) + "步");
                    //     }
                    // } else {
                    //     if (initSteps != 0) {
                    //         pauseStateSteps = stepCountCal - initSteps - stepCounter;
                    //     }
                    // }
                    break;

            }
        }
    };
    */

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (lastLatLng != null) {
            CameraUpdate cu = CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLatLng, zoomLevel, 0, 0));
            aMap.moveCamera(cu);
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }

//        if (autoAdjustBrightness) {
//            BrightnessUtil.startAutoAdjustBrightness(this);
//        }

        if (locationDialog != null && locationDialog.isShowing()) {
            locationDialog.dismissCurrentDialog();
        }
        //页面销毁移除未完成的网络请求
        OkHttpUtils.getInstance().cancelTag(TAG);
        // EventManager.ins().removeListener(EventTag.ON_STEP_CHANGE, eventListener);

        if (myBinder != null) {
            unbindService(connection);
        }

        Intent stepLocationIntent = new Intent(this, LocationService.class);
        stopService(stepLocationIntent);
        Intent stepSensorIntent = new Intent(this, SensorService.class);
        stopService(stepSensorIntent);

        unregisterReceiver(lowBatteryReceiver);

        stopTimer();

        DLOG.closeInternalFile();
        //        if (fos != null) {
        //            try {
        //                fos.close();
        //            } catch (IOException e) {
        //                e.printStackTrace();
        //            }
        //        }
    }

    private BroadcastReceiver lowBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
                if (state == STATE_STARTED) {
                    String msg = "电量不足，请尽快完成运动";
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }

                float batteryLevel = getBatteryLevel();
                Toast.makeText(SportDetailActivity.this, "当前电量： " + batteryLevel + "%", Toast.LENGTH_LONG).show();
            }
        }
    };

}
