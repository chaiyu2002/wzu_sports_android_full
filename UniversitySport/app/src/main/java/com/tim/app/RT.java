package com.tim.app;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Environment;

import com.application.library.log.DLOG;
import com.application.library.util.ChannelUtil;
import com.lzy.okhttputils.OkHttpUtils;
import com.lzy.okhttputils.model.HttpHeaders;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.tim.app.receiver.LSReceiver;
import com.tim.app.server.net.NetworkHost;

import java.io.File;

/**
 * The Class RT
 */
public class RT {

    public static final String TAG = "RT";

    public static boolean DEBUG = true;//日志log开关 true开  false 关

    private static RT self = null;

    public static Application application = null;

    public static NetworkHost HOST = null;

    static {
        if (BuildConfig.DEBUG) {
            HOST = NetworkHost.DEBUG;
        } else {
            HOST = NetworkHost.PUBLISH;
            DEBUG = false;
        }
    }

    /**
     * The m local external path.
     */
    public static String mLocalExternalPath = Environment.getExternalStorageDirectory().getAbsolutePath();

    // 应用的根目录
    /**
     * The Constant ROOT.
     */
    public static String ROOT = "UniversitySports";

    // 缺省根目录
    /**
     * The default root path.
     */
    public static String defaultRootPath = mLocalExternalPath.concat("/").concat(ROOT);

    static {
        try {
            File file = new File(defaultRootPath.endsWith("/") ? defaultRootPath : defaultRootPath.concat("/"));
            boolean ismakedir = false;
            if (!file.exists()) {
                String tmp = mLocalExternalPath.concat("/").concat(ROOT);
                File t = new File(tmp);
                if (t.exists()) {
                    if (!t.renameTo(file)) {
                        // 有小写，换成大写
                        defaultRootPath = tmp;
                    }
                } else {
                    ismakedir = file.mkdirs();
                    if (!ismakedir) {
                        if (RT.isMount()) {
                            // 目录被强制占用
                            defaultRootPath = mLocalExternalPath.concat("/").concat(ROOT);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static String defaultCache = defaultRootPath.concat("/cache/");
    public static String defaultImage = defaultRootPath.concat("/images/");
    public static String defaultLog = defaultRootPath.concat("/logs/");

    /**
     * Instantiates a new dmrt.
     */
    private RT() {

    }

    public static String dealUrl = null;


    /**
     * Ins.
     *
     * @return the dmrt
     */
    public static synchronized RT ins() {
        if (self == null) {
            self = new RT();
        }
        return self;
    }

    // 是否初始化完
    /**
     * The m is init.
     */
    public static boolean mIsInit = false;

    /**
     * 初始化 Inits the.
     */
    public void init() {
        // 构造对象
        synchronized (this) {
            if (!mIsInit) {
                try {
                    DLOG.init(DEBUG);
                    mkdirs();
                    initOkHttp();
                    initBitmap();
//                    UserManager.instance().loadUserInfo(null);
                    // UpdateInfo.loadUpdate();// 载入升级信息
                    ins().regReceivers();
//                    typeFace = Typeface.createFromAsset(application.getAssets(), "fonts/FZLTZHUNHK.TTF");
                    startServices();
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (UnsatisfiedLinkError e) {
                    e.printStackTrace();
                } catch (NoClassDefFoundError e) {
                    e.printStackTrace();
                }
                mIsInit = true;
            }
        }
        if (DEBUG) {
//            CheckErrorThread cethread = new CheckErrorThread();
//            cethread.start();
//            CrashHandler.getInstance().init(application);
        }
    }

    private void initOkHttp() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("content-type", "application/json;charset=UTF-8 ");
//        headers.put("commonHeaderKey1", "commonHeaderValue1");    //所有的 header 都 不支持 中文
//        headers.put("commonHeaderKey2", "commonHeaderValue2");
//        HttpParams params = new HttpParams();
//        params.put("commonParamsKey1", "commonParamsValue1");     //所有的 params 都 支持 中文
//        params.put("commonParamsKey2", "这里支持中文参数");

        //必须调用初始化
        OkHttpUtils.init(application);
        //以下都不是必须的，根据需要自行选择
        OkHttpUtils.getInstance()//
//                .debug(AppConstant.HTTP_TAG)                                              //是否打开调试
                .setConnectTimeout(5000)               //全局的连接超时时间
                .setReadTimeOut(30000)                  //全局的读取超时时间
                .setWriteTimeOut(30000)               //全局的写入超时时间
                //.setCookieStore(new MemoryCookieStore())                           //cookie使用内存缓存（app退出后，cookie消失）
                //.setCookieStore(new PersistentCookieStore())
                // cookie持久化存储，如果cookie不过期，则一直有效
                .addCommonHeaders(headers);          //设置全局公共头
//                .addCommonParams(params);           //设置全局公共参数
    }


    private LSReceiver mReceiver;
    private IntentFilter mFilter;

    private void regReceivers() {
        // 注册
        if (mReceiver == null) {
            mFilter = new IntentFilter();
            //网络状态监听
            mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

            //开机广播
            mFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
            //关机
            mFilter.addAction(Intent.ACTION_SHUTDOWN);

            //屏幕关闭
            mFilter.addAction(Intent.ACTION_SCREEN_OFF);
            //屏幕打开
            mFilter.addAction(Intent.ACTION_SCREEN_ON);
            //屏幕解锁
            mFilter.addAction(Intent.ACTION_USER_PRESENT);

            mFilter.setPriority(Integer.MAX_VALUE);
            mReceiver = new LSReceiver();
            RT.application.registerReceiver(mReceiver, mFilter);
        }

    }

    /**
     * Mkdirs.
     */
    public static void mkdirs() {
        try {

            File file = new File(defaultImage);
            if (!file.exists()) {
                file.mkdirs();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            File file = new File(defaultChat);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        try {
//            File file = new File(defaultVideo);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            File file = new File(defaultCache);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File file = new File(defaultLog);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void startServices() {
//        ComponentName mOverlays = new ComponentName(application, ChatService.class);
//        application.getPackageManager().setComponentEnabledSetting(mOverlays,
//                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
//        application.startService(new Intent(application, ChatService.class));
    }

    private void initBitmap() {
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(application)
                .memoryCache(new UsingFreqLimitedMemoryCache(((int) Runtime.getRuntime().maxMemory()) / 8))
                .diskCache(new UnlimitedDiskCache(StorageUtils.getOwnCacheDirectory(application, ROOT + "/images")))
                // .memoryCacheSize(8 * 1024 * 1024)
                .memoryCacheSize(((int) Runtime.getRuntime().maxMemory()) / 8).diskCacheSize(50 * 1024 * 1024)
                .diskCacheFileNameGenerator(new Md5FileNameGenerator()).defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                .threadPoolSize(3).imageDownloader(new BaseImageDownloader(application, 5 * 1000, 30 * 1000))
                .denyCacheImageMultipleSizesInMemory().build();
        ImageLoader.getInstance().init(configuration);

    }


    public static String getString(int res) {
        return RT.application.getString(res);
    }

    public static String getString(int id, Object... formatArgs) {
        return RT.application.getResources().getString(id, formatArgs);
    }

    public static class AppInfo {
        public static String Channel = "";

        static {
            Channel = ChannelUtil.getChannelName(application, "");
        }
    }

    public static boolean isMount() {
        return new File(RT.mLocalExternalPath).canWrite();
    }

    public static int getScreenWidth() {
        return RT.application.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return RT.application.getResources().getDisplayMetrics().heightPixels;
    }

    public static float getDensity() {
        return RT.application.getResources().getDisplayMetrics().density;
    }

}
