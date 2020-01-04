package come.dong.afloat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by AndroidStudio
 * Author: pd
 * Time: 2019/11/27 15:58
 * 需要权限
 * ACCESS_WIFI_STATE 用于获取wifi信息
 * ACCESS_NETWORK_STATE 用于获取网络状态信息
 * ACCESS_COARSE_LOCATION 用于获取wifi列表
 * ACCESS_FINE_LOCATION 用于获取wifi列表
 */
public class NetWorkUtil {
    private static final String TAG = NetWorkUtil.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static Activity topActivity = null;

    private static Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (topActivity != activity) {
                topActivity = activity;
            }

            Log.d("dong", "topActivity=" + activity.getLocalClassName());
            if (isNetworkLost) {
                networkLost(null);
            } else {
                networkConnect(null);
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            Log.d("dong", "移除topActivity：" + activity.getLocalClassName());
            if (activity == topActivity) {
                topActivity = null;//移除引用，防止内存泄露
            }
        }
    };

    private static TextView textView = null;
    private static boolean isNetworkLost = false;
    private static WifiManager wifiManager;
    private static ConnectivityManager connectivityManager;
    private static List<ConnectivityManager.NetworkCallback> callbackList = new ArrayList<>();

    public static void init(Application application) {
        //内部调用的是add的方法，因此不怕会覆盖之前的
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        wifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);

        connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    //网络丢失
                    networkLost(network);
                    for (ConnectivityManager.NetworkCallback callback : callbackList){
                        callback.onLost(network);
                    }
                }

                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    networkConnect(network);
                }
            });
        }
    }

    public static void addCallback(ConnectivityManager.NetworkCallback callback){
        callbackList.add(callback);
    }

    public static void removeCallback(ConnectivityManager.NetworkCallback callback){
        callbackList.remove(callback);
    }

    /**
     * 检查当前链接的是否是wifi
     * 需要SDK>23
     *
     * @return true/false
     */
    public static boolean isConnectWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities == null){
                Log.d(TAG,"capabilities 为空");
                return false;
            }
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        Log.d(TAG,"SDK版本号小于23");
        return false;
    }

    /**
     * 获取wifi信号强度，单位dBm
     *
     * @return int
     */
    public static int getWifiRssi() {
        if (!isConnectWifi()){
            Log.d(TAG,"当前链接的不是WIFI");
            return -1;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getRssi();
    }

    /**
     * 获取wifi列表
     *
     * @return list
     */
    public static List<ScanResult> getScanResult() {
        return wifiManager.getScanResults();
    }

    /**
     * 判断当前链接的wifi是2.5GHz还是5GHz
     *
     * @return -1表示都不是
     */
    public static int is24Or5() {
        if (!isConnectWifi()){
            Log.d(TAG,"当前链接的不是WIFI");
            return -1;
        }
        WifiInfo info = wifiManager.getConnectionInfo();
        int f = info.getFrequency();
        if (f > 2400 && f < 2500) {
            return 24;
        } else if (f > 4900 && f < 5900) {
            return 5;
        } else {
            return -1;
        }
    }

    /**
     * 获取wifi的加密方式
     *[WPA-PSK-TKIP+CCMP][WPA2-PSK-TKIP+CCMP][RSN-PSK-TKIP+CCMP][ESS][WPS][WFA-HT][WFA-VHT]
     * @param scanResult 扫描后的wifi结果
     * @return string
     */
    public static String getEncryptionType(ScanResult scanResult) {
        if (!TextUtils.isEmpty(scanResult.SSID)) {
            String capabilities = scanResult.capabilities;
            Log.d(TAG, "[" + scanResult.SSID + "]：" + capabilities);
            if (!TextUtils.isEmpty(capabilities)) {
                if (capabilities.contains("WPA2") || capabilities.contains("wpa2")){
                    capabilities = capabilities.replaceAll("WPA2","");
                    capabilities = capabilities.replaceAll("wpa2","");

                    if (capabilities.contains("wpa")|| capabilities.contains("WPA")){
                        return "wpa/wpa2";
                    }else {
                        return "wpa2";
                    }

                }

                if (!capabilities.contains("WPA2") && !capabilities.contains("wpa2") &&
                        (capabilities.contains("WPA") || capabilities.contains("wpa"))){
                    return "wpa";
                }

               if (capabilities.contains("WEP")
                        || capabilities.contains("wep")) {
                    return "wep";
                } else {
                    return "未知";
                }
            }
        }
        return scanResult.capabilities;
    }

    /**
     * 获取当前链接wifi的加密模式
     * @return wpa/wep
     */
    public static String getCurrentWifiEncryptionType(){
        if (!isConnectWifi()){
            Log.d(TAG,"当前链接的不是WIFI");
            return "未知";
        }
        String type = "未知";
        for (ScanResult item : getScanResult()){
            if (item.SSID.equals(getCurrentWifiSsid())){
                type = getEncryptionType(item);
            }
        }
        return type;
    }

    /**
     * 获取当前链接的WIFI_ssid也就是WIFI的名称
     *
     * @return WIFI 的SSID
     */
    public static String getCurrentWifiSsid() {
        if (!isConnectWifi()){
            Log.d(TAG,"当前链接的不是WIFI");
            return "未知";
        }
        String ssid = "未知 zn";
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            ssid = wifiInfo.getSSID();//获取到的SSID可能带双引号，所以要去掉
            if (ssid.substring(0, 1).equals("\"") && ssid.substring(ssid.length() - 1).equals("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
        }
        return ssid;
    }

    private static void networkLost(@Nullable final Network network) {
        isNetworkLost = true;
        if (topActivity != null) {
            textView = new TextView(topActivity);
            textView.setText("网络丢失");
            textView.setTextSize(50);
            textView.setTextColor(Color.RED);
            textView.setPadding(0, 100, 0, 0);

            topActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewGroup root = (ViewGroup) topActivity.getWindow().getDecorView();
                    root.addView(textView);
                }
            });

        }
    }

    private static void networkConnect(@Nullable final Network network) {
        isNetworkLost = false;
        if (topActivity != null) {
            Log.d("dong", "移除添加的View");
            topActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewGroup root = (ViewGroup) topActivity.getWindow().getDecorView();
                    root.removeView(textView);
                    if (network!=null){
                        for (ConnectivityManager.NetworkCallback callback : callbackList){
                            callback.onAvailable(network);
                        }
                    }

                }
            });
        }
    }

}
