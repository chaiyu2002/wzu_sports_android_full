package com.tim.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;

/**
 * @创建者 倪军
 * @创建时间 10/10/2017
 * @描述
 */

public class NetworkUtil {

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        return isConnected;
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("www.sina.com.cn");
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isWifi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        return isWifi;
    }

    // 检查WiFi状态，参考：https://stackoverflow.com/questions/6593858/checking-wi-fi-enabled-or-not-on-android
    public static boolean isWifiOpened(Context context) {
        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            return  true;
        } else {
            return false;
        }

    }

    public static boolean isMobileConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isMobile = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
        return isMobile;
    }
}
