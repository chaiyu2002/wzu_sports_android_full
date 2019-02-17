package com.tim.app.ui.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.AppOpsManagerCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
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
import com.tim.app.server.entry.FixLocationOutdoorSportPoint;
import com.tim.app.server.entry.HistoryAreaSportEntry;
import com.tim.app.server.entry.SportEntry;
import com.tim.app.server.logic.UserManager;
import com.tim.app.sport.RunningSportsCallback;
import com.tim.app.sport.SQLite;
import com.tim.app.ui.dialog.LocationDialog;
import com.tim.app.ui.dialog.ProgressDialog;
import com.tim.app.ui.view.SlideUnlockView;
import com.tim.app.ui.view.webview.WebViewActivity;
import com.tim.app.util.BrightnessUtil;
import com.tim.app.util.PermissionUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.tim.app.constant.AppConstant.student;

/**
 * @创建者 倪军
 * @创建时间 2017/7/31
 * @描述 定点运动详情页
 */
public class SportFixedLocationActivity extends BaseActivity implements AMap.OnMyLocationChangeListener, AMap.OnMapTouchListener {

    /*全局变量*/
    private static final String TAG = "FixLocationActivity";
    private Context context = this;

    /*三方控件*/
    private MapView mapView;
    private AMap aMap;
    private UiSettings uiSettings;
    private Location firstLocation;
    private int firstLocationType;
    private LatLng lastLatLng = null;
    private LatLng firstLatLng = null;
    private Circle circle;//当前运动区域
    private List<LatLng> targetLatLngs = new ArrayList<LatLng>();
    private Marker centerMarker;

    /*重要实体*/
    private SportEntry sportEntry;//创建areaActivity的时候要用到
    private FixLocationOutdoorSportPoint fixLocationOutdoorSportPoint;
    private HistoryAreaSportEntry historySportEntry;

    /*基本控件*/
    // private LinearLayout llLacationHint;
    private TextView tvResult;//运动结果
    private LinearLayout llResult;//运动结果父容器
    private TextView tvAreaName; //区域地点名字
    private TextView tvSelectLocation;//选择区域
    private RelativeLayout rlTopFloatingWindow; //浮动窗顶部地点名字块
    private RelativeLayout rlAreaDesc; //运动地点描述块
    private TextView tvAreaDesc; //运动地点描述
    private RelativeLayout rlElapsedTime; //消耗的时间块
    private TextView tvElapsedTime; //耗时
    private RelativeLayout rlBottomFloatingWindow; //浮动窗底部达标时间块
    private TextView tvTargetTime; //达标时间
    private TextView tvParticipantNum; //参与的人数
    private RelativeLayout rlBottom;
    private Button btStart;
    private LinearLayout llBottom;
    private SlideUnlockView slideUnlockView;
    private TextView tvPause;
    private View rlAnimView;
    private ImageView ivLocation;  //页面左下角定位图标
    private ImageView ivHelp;
    private ImageView ivFinished;

    private LocationDialog locationDialog;
    private ProgressDialog progressDialog;

//    /*屏幕亮度*/
//    private int screenOffTimeout; //屏幕超时时间
//    private int screenKeepLightTime;
//    private int brightness;
//    private boolean autoAdjustBrightness;

    /*初始化变量*/
    static final int STATE_NORMAL = 0;//初始状态
    static final int STATE_STARTED = 1;//已开始
    static final int STATE_PAUSE = 2;//暂停
    static final int STATE_END = 3;//结束
    private int state = STATE_NORMAL;
    private int interval = 1000;  //计时
    private int acquisitionInterval = 0;//采样时间间隔
    private int navigationInterval = 3000;//开始之前导航间隔
    private long elapseTime = 0;
    private boolean isFirst = true;
    private long startTime;//开始时间
    private long stopTime;//运动结束时间
    private float zoomLevel = 16;//地图缩放级别，范围3-19,越大越精细
    private int areaSportRecordId; //注意，这里的id指的是服务端数据库中这条记录的ID。

    public static final String NETWORK_ERROR_MSG = "网络请求失败，请检查网络状态或稍后再试";

    public static final String COMMIT_FALIED_MSG = "网络错误，数据提交失败，请随后查看历史记录";

    public static final String OPEN_GPS_MSG = "需要打开GPS才能开始运动...";

    public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x01;
    public static final int REQUEST_PERMISSION_WRITE_FINE_LOCATION = 0x02;

    /*组件*/
    private LocationService.MyBinder myBinder = null;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private Runnable elapseTimeRunnable;
    private ScheduledFuture<?> timerHandler = null;
    private long timerInterval = 1000;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DLOG.d(TAG, "onServiceConnected name: " + name + ", service: " + service);
            myBinder = (LocationService.MyBinder) service;
            myBinder.startLocationInService(acquisitionInterval);
        }
    };

    public static void start(Context context, FixLocationOutdoorSportPoint fixLocationOutdoorSportPoint, SportEntry sportEntry) {
        Intent intent = new Intent(context, SportFixedLocationActivity.class);
        intent.putExtra("sportEntry", sportEntry);
        intent.putExtra("fixLocationOutdoorSportPoint", fixLocationOutdoorSportPoint);
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

    /****************  <初始化开始> *****************/
    private void initGPS() {
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS模块是否开启，如果没有则开启
        if (!locationManager
                .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            //            Toast.makeText(this, "定位服务未打开，请打开定位服务",
            //                    Toast.LENGTH_SHORT).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("定位服务未打开，请打开定位服务");
            builder.setPositiveButton("确定",
                    new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            // 转到手机设置界面，用户设置GPS
                            Intent intent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 0); // 设置完成后返回到原来的界面
                        }
                    });
            //            locationDialog.setNeutralButton("取消", new android.content.DialogInterface.OnClickListener() {
            //                @Override
            //                public void onClick(DialogInterface arg0, int arg1) {
            //                    arg0.dismiss();
            //                }
            //            });

            AlertDialog dialog = builder.show();
            TextView message = (TextView) dialog.findViewById(android.R.id.message);
            Button positiveButton = (Button) dialog.findViewById(android.R.id.button1);
            positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        } else {
            locationDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        if (requestCode == 0) {
            if (locationManager
                    .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                // locationDialog.show();
            } else {
                Toast.makeText(this, OPEN_GPS_MSG, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

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

        Float level = getBatteryLevel();
        TextView tvRemainPower = (TextView) locationDialog.findViewById(R.id.tvRemainPower);
        tvRemainPower.setText(getResources().getString(R.string.remainPower, String.valueOf(level.intValue())));

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        initGPS();

        sportEntry = (SportEntry) getIntent().getSerializableExtra("sportEntry");
        fixLocationOutdoorSportPoint = (FixLocationOutdoorSportPoint) getIntent().getSerializableExtra("fixLocationOutdoorSportPoint");
        targetLatLngs.add(new LatLng(fixLocationOutdoorSportPoint.getLatitude(), fixLocationOutdoorSportPoint.getLongitude()));
        // DLOG.d(TAG, "fixLocationOutdoorSportPoint:" + fixLocationOutdoorSportPoint);
        DLOG.d(TAG, "sportEntry:" + sportEntry);
        DLOG.d(TAG, "sportEntry.getAcquisitionInterval():" + sportEntry.getAcquisitionInterval());

        acquisitionInterval = sportEntry.getAcquisitionInterval() * 1000;
        // elapseTime = 0 - sportEntry.getAcquisitionInterval(); // 解决动态改变高德回调的之后立即回调一次的问题。

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写，创建地图
        initMap();
        //        mAMapGeoFence = new AMapGeoFence(this.getApplicationContext(), aMap, handler);

        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("type", "区域");
        startService(intent);

        aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {

            }
        });

        aMap.setOnInfoWindowClickListener(new AMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // AMapUtil.moveToTarget(aMap, zoomLevel, targetLatLngs.get(0), 1500);
                setZoomScale();
            }
        });

        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                setZoomScale();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        registerReceiver(lowBatteryReceiver, filter);
    }

    public void setZoomScale() {
        List<LatLng> latLngs = new ArrayList<>();
        float degree = (float) targetLatLngs.get(0).latitude;
        float minute = (float) ((targetLatLngs.get(0).latitude - (int) targetLatLngs.get(0).latitude) * 60);
        // Log.d(TAG, "(targetLatLngs.get(0).latitude - (int)targetLatLngs.get(0).latitude):" + (targetLatLngs.get(0).latitude - (int) targetLatLngs.get(0).latitude));
        // Log.d(TAG, "minute:" + minute);
        float second = (minute - (int) minute) * 60;
        // Log.d(TAG, "second:" + second);
        float topSecond = (float) (second + fixLocationOutdoorSportPoint.getRadius() / 30.9f);
        float bottomSecond = (float) (second - fixLocationOutdoorSportPoint.getRadius() / 30.9f);
        // Log.d(TAG, "second:" + second);
        float topLatitude = ((int) degree + (topSecond / 60 + (int) minute) / 60);
        float bottomLatitude = ((int) degree + (bottomSecond / 60 + (int) minute) / 60);
        // DLOG.d(TAG, "topLatitude:" + topLatitude);
        // DLOG.d(TAG, "bottomLatitude:" + bottomLatitude);
        LatLng topLatlng = new LatLng(topLatitude, targetLatLngs.get(0).longitude);
        LatLng bottomLatlng = new LatLng(bottomLatitude, targetLatLngs.get(0).longitude);
        latLngs.add(topLatlng);
        latLngs.add(bottomLatlng);

        LatLngBounds bounds = getLatLngBounds(latLngs);//以中心点缩放

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float calcWidthDp = (float) (metrics.densityDpi * 0.2778);
        float calcWidthPx = metrics.widthPixels / metrics.densityDpi * calcWidthDp;

        float calcHeightDp = (float) (metrics.densityDpi * 0.3125);
        float calcHeightPx = metrics.heightPixels / metrics.densityDpi * calcHeightDp;
        aMap.animateCamera(CameraUpdateFactory.newLatLngBoundsRect(bounds, (int) calcWidthPx, (int) calcWidthPx, (int) calcHeightPx, (int) calcHeightPx)); //平滑移动
    }

    @Override
    public void initData() {
        float batteryLevel = getBatteryLevel();
        Toast.makeText(this, "当前电量： " + batteryLevel + "%， 请及时充电，保持电量充足", Toast.LENGTH_LONG).show();
//        screenOffTimeout = Settings.System.getInt(getContentResolver(),
//                Settings.System.SCREEN_OFF_TIMEOUT, 0) / 1000;
//
//        autoAdjustBrightness = BrightnessUtil.isAutoAdjustBrightness(context);
//        if (autoAdjustBrightness) {
//            brightness = BrightnessUtil.getScreenBrightness(this);
//            BrightnessUtil.stopAutoAdjustBrightness(context);
//        } else {
//            brightness = BrightnessUtil.getScreenBrightness(this);
//        }


        if (!TextUtils.isEmpty(fixLocationOutdoorSportPoint.getAreaName())) {
            tvAreaName.setText(fixLocationOutdoorSportPoint.getAreaName());
        }
        if (!TextUtils.isEmpty(fixLocationOutdoorSportPoint.getDescription())) {
            tvAreaDesc.setText(fixLocationOutdoorSportPoint.getDescription());
        }
        tvParticipantNum.setText(getString(R.string.joinPrompt, String.valueOf(sportEntry.getParticipantNum())));

        if (fixLocationOutdoorSportPoint.getQualifiedCostTime() > 0) {
            tvTargetTime.setText(String.format(getResources().getString(R.string.minutePlaceHolder), String.valueOf(fixLocationOutdoorSportPoint.getQualifiedCostTime() / 60)));
        } else {
            tvTargetTime.setText("-");
        }

        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {// PackageManager.PERMISSION_DENIED
            UserManager.instance().cleanCache();
        }

        setupArea();

        elapseTimeRunnable = new Runnable() {
            public void run() {
                // If you need update UI, simply do this:
                runOnUiThread(new Runnable() {
                    public void run() {
                        // update your UI component here.
                        elapseTime += timerInterval / 1000;
                        DLOG.d(TAG, "elapseTime: " + elapseTime);
                        String time = com.tim.app.util.TimeUtil.formatMillisTime(elapseTime * 1000);
                        tvElapsedTime.setText(time);
                    }
                });
            }
        };

        CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
                new CameraPosition(targetLatLngs.get(0), zoomLevel, 0, 0));
        aMap.moveCamera(cu);

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
                    tvPause.setVisibility(View.INVISIBLE);

                    if (state == STATE_STARTED) {
                        state = STATE_END;

                        stopTimer();
                        //结束本次运动
                        areaActivitiesEnd(areaSportRecordId);

                        //tvParticipantNum.setVisibility(View.GONE);
                        rlBottom.setVisibility(View.VISIBLE);
                        llBottom.setVisibility(View.GONE);
                        btStart.setVisibility(View.VISIBLE);
                        btStart.setText("查看锻炼结果");

                        myBinder.stopLocationInService();
                        aMap.setOnMyLocationChangeListener(null);
                    }
                }
            }
        });
    }

    private void setupArea() {
        //设置地图区域范围（画圈儿）
        LatLng latLng = new LatLng(fixLocationOutdoorSportPoint.getLatitude(), fixLocationOutdoorSportPoint.getLongitude());
        circle = aMap.addCircle(new CircleOptions().
                center(latLng).
                radius(fixLocationOutdoorSportPoint.getRadius()).
                fillColor(Color.parseColor("#77B0F566")).
                strokeColor(Color.parseColor("#224C5773")).
                strokeWidth(1));

        //初始化中心点Marker
        centerMarker = aMap.addMarker(new MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.ic_location_stick))
                .position(targetLatLngs.get(0))
                .title("目标区域"));
        centerMarker.showInfoWindow();
        DLOG.d(TAG, "fixLocationOutdoorSportPoint:" + fixLocationOutdoorSportPoint);
    }

    @Override
    public void initView() {
        // llLacationHint = (LinearLayout) findViewById(R.id.llLocationHint);
        tvPause = (TextView) findViewById(R.id.tvPause);
        slideUnlockView = (SlideUnlockView) findViewById(R.id.slideUnlockView);
        rlBottom = (RelativeLayout) findViewById(R.id.rlBottom);
        btStart = (Button) findViewById(R.id.btStart);
        llBottom = (LinearLayout) findViewById(R.id.llBottom);

        rlTopFloatingWindow = (RelativeLayout) findViewById(R.id.rlTopFloatingWindow);
        tvElapsedTime = (TextView) findViewById(R.id.tvElapsedTime);
        tvResult = (TextView) findViewById(R.id.tvResult);
        llResult = (LinearLayout) findViewById(R.id.llResult);
        tvParticipantNum = (TextView) findViewById(R.id.tvParticipantNum);
        ivLocation = (ImageView) findViewById(R.id.ivLocation);
        ivHelp = (ImageView) findViewById(R.id.ivHelp);
        ivFinished = (ImageView) findViewById(R.id.ivFinished);
        tvAreaName = (TextView) findViewById(R.id.tvAreaName);
        tvSelectLocation = (TextView) findViewById(R.id.tvSelectLocation);
        rlAreaDesc = (RelativeLayout) findViewById(R.id.rlAreaDesc);
        tvAreaDesc = (TextView) findViewById(R.id.tvAreaDesc);
        rlElapsedTime = (RelativeLayout) findViewById(R.id.rlElapsedTime);
        rlBottomFloatingWindow = (RelativeLayout) findViewById(R.id.rlBottomFloatingWindow);
        tvTargetTime = (TextView) findViewById(R.id.tvTargetTime);

        rlAnimView = findViewById(R.id.rlAnimView);

        btStart.setOnClickListener(this);
        ivLocation.setOnClickListener(this);
        tvSelectLocation.setOnClickListener(this);
    }


    /****************  <高德地图相关开始> *****************/

    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        aMap.setOnMyLocationChangeListener(this);
        uiSettings = aMap.getUiSettings();
    }

    private void setUpMap() {
        setupLocationStyle();
        aMap.getUiSettings().setMyLocationButtonEnabled(false);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位`蓝点并不进行定位，默认是false。
    }

    private void setupLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。
        myLocationStyle.interval(navigationInterval);
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
                fromResource(R.drawable.navi_map_gps_locked));
        aMap.setMyLocationStyle(myLocationStyle);
    }

    /**
     * 绘制两个坐标点之间的线段,从以前位置到现在位置
     */
    private void drawLine(LatLng oldData, LatLng newData, boolean isNormal) {
        // 绘制曲线
        if (isNormal) {
            aMap.addPolyline((new PolylineOptions())
                    .add(oldData, newData)
                    .geodesic(true).color(Color.GREEN));
        } else {
            aMap.addPolyline((new PolylineOptions())
                    .add(oldData, newData)
                    .geodesic(true).color(Color.RED));
        }
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

        DLOG.d(TAG, "zbc");
        //运动耗时
        if (state == STATE_STARTED) {
            DLOG.d(TAG, "elapseTime:" + elapseTime);
            MyLocationStyle myLocationStyle = aMap.getMyLocationStyle();
            DLOG.d(TAG, "myLocationStyle.getInterval():" + myLocationStyle.getInterval());

            //            elapseTime = (long) ((System.currentTimeMillis() - startTime + 0.5) / 1000);
            //            String time = com.tim.app.util.TimeUtil.formatMillisTime(elapseTime * 1000);
            //            Log.d(TAG, "elapseTime:" + elapseTime);
            //            tvElapsedTime.setText(time);
            //            DLOG.d(TAG, "elapseTime:" + elapseTime);
        }

        Bundle bundle = location.getExtras();
        if (bundle != null) {
            errorCode = bundle.getInt(MyLocationStyle.ERROR_CODE);
            errorInfo = bundle.getString(MyLocationStyle.ERROR_INFO);
            // 定位类型，可能为GPS WIFI等，具体可以参考官网的定位SDK介绍
            locationType = bundle.getInt(MyLocationStyle.LOCATION_TYPE);
            DLOG.d(TAG, "errorCode:" + errorCode + "     errorInfo:" + errorInfo + "     locationType:" + locationType);
        }

        //屏幕到了锁屏的时间，调暗亮度，采样时间太长了导致不容易测试
        WindowManager.LayoutParams params = getWindow().getAttributes();
//        screenKeepLightTime += interval / 1000;
//        if (screenOffTimeout <= screenKeepLightTime && Float.compare(params.screenBrightness, 0.1f) != 0) {
//            params.screenBrightness = (float) 0.1;
//            getWindow().setAttributes(params);
//            DLOG.d(TAG, "onMyLocationChange turn down light");
//        }

        if (location != null) {
            //定位成功
            if (errorCode != 0) {
                String errText = "正在定位中，GPS信号弱";
                Toast.makeText(this, errText, Toast.LENGTH_SHORT).show();
                return;
            } else {
                newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                DLOG.d(TAG, "newLatLng: " + newLatLng);
                // 判断第一次，第一次会提示
                if (lastLatLng == null) {
                    String errText = "定位成功";
                    Toast.makeText(this, errText, Toast.LENGTH_SHORT).show();

                    firstLatLng = newLatLng;
                    firstLocation = location;
                    firstLocationType = locationType;
                    locationDialog.dismissCurrentDialog();

                    // CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
                    //         new CameraPosition(targetLatLngs.get(0), zoomLevel, 0, 0));
                    // aMap.moveCamera(cu);

                    btStart.setVisibility(View.VISIBLE);

                    // Log.d(TAG, "targetLatLng:" + targetLatLngs.toString());
                    // Log.d(TAG, "newLatLng:" + newLatLng);
                }
            }

            if (state == STATE_STARTED) {
                String msg = location.toString();
                // DLOG.writeToInternalFile(msg);
                DLOG.d(TAG, "lastLatLng: " + lastLatLng);

                float batteryLevel = getBatteryLevel();
                if (batteryLevel <= 20) {
                    Toast.makeText(this, "当前电量： " + (int) batteryLevel + "%， 请及时充电，保持电量充足", Toast.LENGTH_LONG).show();
                }

                // float distanceInterval = AMapUtils.calculateLineDistance(newLatLng, lastLatLng);

                // toastText = "绘制曲线，上一次坐标： " + lastLatLng + "， 新坐标：" + newLatLng
                //         + "， 本次移动距离： " + distanceInterval +
                //         "， 当前电量: " + batteryLevel + "%";
                // Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

                boolean isContains = circle.contains(lastLatLng);
                if (!isContains) {
                    isNormal = false;
                    Toast.makeText(context, "你已离开运动区域，请回到运动区域进行锻炼", Toast.LENGTH_LONG).show();
                }

                //// 向服务器提交数据
                ServerInterface.instance().areaActivityData(TAG, areaSportRecordId, location.getLongitude(),
                        location.getLatitude(), locationType, isNormal, new ResponseCallback() {
                            @Override
                            public boolean onResponse(Object result, int status, String errmsg, int id, boolean fromcache) {
                                if (status == 0) {
                                    DLOG.d(TAG, "上传 areaActivityData 成功!");
                                    return true;
                                } else {
                                    String msg = "areaActivityData failed, errmsg: " + errmsg + "\r\n";
                                    msg += "net type: " + NetUtils.getNetWorkType(SportFixedLocationActivity.this) + "\r\n";
                                    msg += "net connectivity is: " + NetUtils.isConnection(SportFixedLocationActivity.this) + "\r\n";
                                    DLOG.writeToInternalFile(msg);
                                    return false;
                                }
                            }
                        });

            }
            lastLatLng = newLatLng;
        } else {
            String errText = "定位失败：" + errorInfo;
            DLOG.e(TAG, errText);
            Toast.makeText(this, errText, Toast.LENGTH_LONG).show();
        }
    }

    //根据中心点和自定义内容获取缩放bounds
    private LatLngBounds getLatLngBounds(LatLng centerpoint, List<LatLng> pointList) {
        // 经纬度坐标矩形区域的生成器
        LatLngBounds.Builder b = LatLngBounds.builder();
        if (centerpoint != null) {
            for (int i = 0; i < pointList.size(); i++) {
                LatLng p = pointList.get(i);
                LatLng p1 = new LatLng((centerpoint.latitude * 2) - p.latitude, (centerpoint.longitude * 2) - p.longitude);
                b.include(p);
                b.include(p1);
            }
        }
        return b.build();
    }

    /**
     * 根据自定义内容获取缩放bounds
     */
    public LatLngBounds getLatLngBounds(List<LatLng> pointList) {
        LatLngBounds.Builder b = LatLngBounds.builder();
        for (int i = 0; i < pointList.size(); i++) {
            LatLng p = pointList.get(i);
            b.include(p);
        }
        return b.build();
    }

    /****************  <Activity基本设置开始> *****************/

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

//        if (autoAdjustBrightness) {
//            BrightnessUtil.startAutoAdjustBrightness(this);
//        }

        //        Log.d(TAG, "onDestroy");

        //页面销毁移除未完成的网络请求
        OkHttpUtils.getInstance().cancelTag(TAG);

        if (myBinder != null) {
            unbindService(connection);
        }

        Intent stopIntent = new Intent(this, LocationService.class);
        stopService(stopIntent);
        unregisterReceiver(lowBatteryReceiver);

        DLOG.closeInternalFile();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    // 为什么这个没有反应
    // @Override
    // public boolean onTouchEvent(MotionEvent event) {
    //     WindowManager.LayoutParams params = getWindow().getAttributes();
    //     params.screenBrightness = (float) 1;
    //     getWindow().setAttributes(params);
    //     DLOG.d(TAG, "onTouchEvent turn up light");
    //     return false;
    // }

    @Override
    public void onClick(View v) {
        turnUpScreen();

        switch (v.getId()) {
            case R.id.btStart:
                // DLOG.d(TAG, "ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION):" + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION));
                // DLOG.d(TAG, "ActivityCompat.shouldShowRequestPermissionRationale(this," + ActivityCompat.shouldShowRequestPermissionRationale(this,
                //         Manifest.permission.ACCESS_FINE_LOCATION));

                //先检查定位权限
                if (!PermissionUtil.checkLocationPermission(this)) {
                    return;
                }

                // 判断是否在运动范围内
                boolean isContains = circle.contains(lastLatLng);

                if (state == STATE_NORMAL) {
                    if (isContains) {
                        // 改变回调间隔
                        MyLocationStyle myLocationStyle = aMap.getMyLocationStyle();
                        myLocationStyle.interval(acquisitionInterval);
                        aMap.setMyLocationStyle(myLocationStyle);
                        CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
                                new CameraPosition(targetLatLngs.get(0), zoomLevel, 0, 0));
                        aMap.moveCamera(cu);
                        allowStart();
                        startTimer();
                        // DLOG.d(TAG, "onClick：elapseTime:" + elapseTime);
                    } else {
                        Toast.makeText(this, "请到指定运动区域进行锻炼", Toast.LENGTH_SHORT).show();
                    }
                    // MyLocationStyle myLocationStyle = aMap.getMyLocationStyle();
                    // myLocationStyle.interval(acquisitionInterval);
                    // aMap.setMyLocationStyle(myLocationStyle);
                    // allowStart();
                } else if (state == STATE_END) {
                    finish();
                    SportResultActivity.start(this, historySportEntry);
                }
                break;

            case R.id.ivLocation:
                //点击定位图标 实现定位到当前位置
                // AMapUtil.moveToTarget(aMap, zoomLevel, lastLatLng, 600);

                DisplayMetrics metrics = getResources().getDisplayMetrics();
                float calcWidthDp = (float) (metrics.densityDpi * 0.2778);
                float calcWidthPx = metrics.widthPixels / metrics.densityDpi * calcWidthDp;

                float calcHeightDp = (float) (metrics.densityDpi * 0.3125);
                float calcHeightPx = metrics.heightPixels / metrics.densityDpi * calcHeightDp;

                targetLatLngs.add(lastLatLng);
                LatLngBounds bounds = getLatLngBounds(targetLatLngs);//以中心点缩放
                aMap.animateCamera(CameraUpdateFactory.newLatLngBoundsRect(bounds, (int) calcWidthPx, (int) calcWidthPx, (int) calcHeightPx, (int) calcHeightPx)); //平滑移动

                break;
            case R.id.tvSelectLocation:
                finish();
        }
    }

    /**
     * 点击事件，运动开始
     */
    public void allowStart() {
        if (state == STATE_NORMAL) {
            btStart.setVisibility(View.GONE); // 点击开始按钮后立即隐藏开始按钮
            progressDialog.show();

            //设置时间
            tvElapsedTime.setText("0 分钟");

            // initData();
            Intent bindIntent = new Intent(this, LocationService.class);
            bindService(bindIntent, connection, BIND_AUTO_CREATE);

            //开始本次运动
            ServerInterface.instance().areaActivities(TAG, sportEntry.getId(), student.getId(), fixLocationOutdoorSportPoint.getId(), new JsonResponseCallback() {
                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    if (errCode == 0) {
                        DLOG.d(TAG, "areaSportsStart 成功");
                        state = STATE_STARTED;
                        startTime = System.currentTimeMillis();
                        JSONObject jsonObject = json.optJSONObject("obj");

                        areaSportRecordId = jsonObject.optInt("id");
                        DLOG.d(TAG, "areaSportRecordId:" + areaSportRecordId);
                        //                        acquisitionInterval = jsonObject.optInt("acquisitionInterval");

                        //第一次向服务器提交数据
                        // DLOG.d(TAG, "firstLocation:" + firstLocation);
                        ServerInterface.instance().areaActivityData(TAG, areaSportRecordId, firstLocation.getLongitude(),
                                firstLocation.getLatitude(), firstLocationType, true, new ResponseCallback() {
                                    @Override
                                    public boolean onResponse(Object result, int status, String errmsg, int id, boolean fromcache) {
                                        if (status == 0) {
                                            DLOG.d(TAG, "第一次上传 areaActivityData 成功!");

                                            slideUnlockView.setVisibility(View.VISIBLE);
                                            tvPause.setVisibility(View.VISIBLE);
                                            rlElapsedTime.setVisibility(View.VISIBLE);
                                            rlAreaDesc.setVisibility(View.GONE);
                                            tvSelectLocation.setVisibility(View.GONE);

                                            progressDialog.dismissCurrentDialog();

                                            return true;
                                        } else {
                                            Toast.makeText(SportFixedLocationActivity.this, NETWORK_ERROR_MSG, Toast.LENGTH_SHORT).show();
                                            String msg = "areaActivityData failed, errmsg: " + errmsg + "\r\n";
                                            msg += "net type: " + NetUtils.getNetWorkType(SportFixedLocationActivity.this) + "\r\n";
                                            msg += "net connectivity is: " + NetUtils.isConnection(SportFixedLocationActivity.this) + "\r\n";
                                            DLOG.writeToInternalFile(msg);
                                            return false;
                                        }
                                    }
                                });
                        return true;
                    } else {
                        Toast.makeText(SportFixedLocationActivity.this, NETWORK_ERROR_MSG, Toast.LENGTH_SHORT).show();
                        DLOG.d(TAG, "errMsg:" + errMsg);
                        return false;
                    }
                }
            });
        }
    }

    /**
     * @param requestCode
     * @param permissions
     * @param grantResults 记录的是授权的结果，如果申请了两个授权，那么数组的length就是2
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UserManager.instance().cleanCache();
                }
                break;
            case REQUEST_PERMISSION_WRITE_FINE_LOCATION:
                String permission = Manifest.permission.ACCESS_FINE_LOCATION;
                String op = AppOpsManagerCompat.permissionToOp(permission);
                int result = AppOpsManagerCompat.noteProxyOp(context, op, context.getPackageName());
                if (result == AppOpsManagerCompat.MODE_IGNORED
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    DLOG.d("onRequestPermissionsResult", "onRequestPermissionsResult");
                    Toast.makeText(this,
                            getString(R.string.manual_open_permission_hint),
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    /****************  <BaseActivity回调开始> *****************/
    @Override
    protected int getLayoutId() {
        return R.layout.activity_area_sport;
    }

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /****************  <自定义方法开始> *****************/
    /**
     * 提交运动数据
     *
     * @param areaSportRecordId
     */
    private void areaActivitiesEnd(final int areaSportRecordId) {
        DLOG.d(TAG, "areaSportRecordId:" + areaSportRecordId);
        //必须先初始化。
        SQLite.init(context, RunningSportsCallback.getInstance());
        //提交本次运动数据，更新UI
        ServerInterface.instance().areaActivitiesEnd(
                TAG, areaSportRecordId, new JsonResponseCallback() {
                    @Override
                    public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                        DLOG.d(TAG, "成功发出 areaActivitiesEnd 请求");
                        if (errCode == 0) {
                            /**
                             * {
                             07-31 22:18:26.017 9640-9640/com.tim.moli D/http: ║     "statusMsg": "记录提交成功",
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║     "obj": {
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║         "id": 130,
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║         "areaSportId": 5,
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║         "studentId": 2,
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║         "costTime": 4,
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║         "startTime": 1501510701000,
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║         "kcalConsumed": 0,
                             07-31 22:18:26.018 9640-9640/com.tim.moli D/http: ║         "qualified": false,
                             07-31 22:18:26.019 9640-9640/com.tim.moli D/http: ║         "qualifiedCostTime": 3600,
                             07-31 22:18:26.019 9640-9640/com.tim.moli D/http: ║         "createdAt": 1501557500000,
                             07-31 22:18:26.019 9640-9640/com.tim.moli D/http: ║         "updatedAt": 1501557500000,
                             07-31 22:18:26.019 9640-9640/com.tim.moli D/http: ║         "endedAt": 1501510705792,
                             07-31 22:18:26.019 9640-9640/com.tim.moli D/http: ║         "endedBy": false
                             07-31 22:18:26.019 9640-9640/com.tim.moli D/http: ║     }
                             07-31 22:18:26.019 9640-9640/com.tim.moli D/http: ║ }
                             */
                            JSONObject jsonObject = json.optJSONObject("obj");
                            historySportEntry = new HistoryAreaSportEntry();

                            DLOG.d(TAG, "fixLocationOutdoorSportPoint:" + fixLocationOutdoorSportPoint);
                            historySportEntry.setLocationPoint(fixLocationOutdoorSportPoint);
                            historySportEntry.setId(jsonObject.optInt("id"));
                            historySportEntry.setSportId(jsonObject.optInt("areaSportId"));
                            historySportEntry.setStudentId(jsonObject.optInt("studentId"));
                            historySportEntry.setCostTime(jsonObject.optInt("costTime"));
                            historySportEntry.setStartTime(jsonObject.optLong("startTime"));
                            historySportEntry.setKcalConsumed(jsonObject.optInt("kcalConsumed"));
                            historySportEntry.setQualified(jsonObject.optBoolean("qualified"));

                            historySportEntry.setVerified(jsonObject.optBoolean("isVerified"));
                            //historySportEntry.setVerified(true);    //先写死，以后用的时候再改

                            historySportEntry.setQualifiedCostTime(jsonObject.optInt("qualifiedCostTime"));
                            historySportEntry.setCreatedAt(jsonObject.optLong("createdAt"));
                            historySportEntry.setUpdatedAt(jsonObject.optLong("updatedAt"));
                            historySportEntry.setEndedAt(jsonObject.optLong("endedAt"));
                            historySportEntry.setEndedBy(jsonObject.optBoolean("endedBy"));
                            historySportEntry.setType(AppConstant.AREA_TYPE);

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
                            DLOG.d(TAG, "historySportEntry.isQualified():" + historySportEntry.isQualified());
                            llResult.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    WebViewActivity.loadUrl(SportFixedLocationActivity.this, "http://www.guangyangyundong.com:86/#/help", "帮助中心");
                                    overridePendingTransition(R.anim.right_in, R.anim.left_out);
                                }
                            });
                            return true;
                        } else {
                            Toast.makeText(SportFixedLocationActivity.this, COMMIT_FALIED_MSG, Toast.LENGTH_SHORT).show();
                            DLOG.d(TAG, COMMIT_FALIED_MSG);
                            // 由于网络原因而使得数据没有正确提交，historySportEntry 是为空的！不应该显示"查看锻炼结果"按钮
                            btStart.setVisibility(View.GONE);
                            return false;
                        }
                    }
                });
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);   //当前电池百分比
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);   //当前电池的最大基数，一般都设置为100
        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50.0f;
        }
        return ((float) level / (float) scale) * 100.0f;
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
        boolean needToAdjustBrightness = Float.compare(BrightnessUtil.getScreenBrightness(getWindow()), 0.1f) == 0;

//        if (needToAdjustBrightness) {
//            if (BrightnessUtil.isAutoAdjustBrightness(this)) {
//                BrightnessUtil.setScreenBrightness(this, brightness);
//            } else {
//                BrightnessUtil.setScreenBrightness(this, brightness);
//            }
//        }
//        screenKeepLightTime = 0;
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
                Toast.makeText(SportFixedLocationActivity.this, "当前电量： " + batteryLevel + "%", Toast.LENGTH_LONG).show();
            }
        }
    };
}
