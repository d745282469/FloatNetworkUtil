package come.dong.afloat;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Create by AndroidStudio
 * Author: pd
 * Time: 2020/1/4 09:02
 */
public class FloatNetworkUtilService extends Service {
    private static final String TAG = FloatNetworkUtilService.class.getSimpleName();
    private WindowManager.LayoutParams layoutParams;
    private WindowManager windowManage;
    private View view;
    private TextView wifiName;
    private TextView wifiRssi;
    private TextView wifiType;
    private TextView wifiPsdType;
    private TextView connectTime;
    private int connectTimeCount = 0; //配网耗时
    private Timer connectDeviceTimer = new Timer();

    private int WHAT_REFRESH_RSSI = 100;
    private int WHAT_CONNECT_DEVICE = 101;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == WHAT_REFRESH_RSSI) {
                wifiRssi.setText(NetWorkUtil.getWifiRssi() + "dBm");
            } else if (msg.what == WHAT_CONNECT_DEVICE) {
                connectTimeCount++;
                connectTime.setText(connectTimeCount + "s");
            }
            return false;
        }
    });
    private FloatNetworkUtilBinder binder = new FloatNetworkUtilBinder(this);

    @Override
    public void onCreate() {
        super.onCreate();
        showFloatWindow();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        windowManage.removeView(view);
        super.onDestroy();
    }

    /**
     * 开始计时
     */
    private void startConnectDevice() {
        connectDeviceTimer = new Timer();
        connectTimeCount = 0;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = WHAT_CONNECT_DEVICE;
                handler.sendMessage(msg);
            }
        };
        connectDeviceTimer.schedule(task, 0, 1000);
    }

    private void cancelConnectDevice() {
        connectDeviceTimer.cancel();
    }

    private void connectDeviceTimeout() {
        connectDeviceTimer.cancel();
    }

    private void connectDeviceSuccess() {
        connectDeviceTimer.cancel();
        Log.d(TAG,"配网成功耗时："+connectTimeCount+"秒");
    }

    private void showFloatWindow() {
        windowManage = (WindowManager) getSystemService(WINDOW_SERVICE);

        //要显示的view
        view = LayoutInflater.from(this).inflate(R.layout.float_network_util, null);
        view.setOnTouchListener(new FloatWindowTouchListener());//整体可以拖动

        wifiName = view.findViewById(R.id.float_network_name);
        wifiRssi = view.findViewById(R.id.float_network_rssi);
        wifiType = view.findViewById(R.id.float_network_type);
        wifiPsdType = view.findViewById(R.id.float_network_psd_type);
        connectTime = view.findViewById(R.id.float_network_time);
        getWifiInfo();

        //设置属性
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 300;
        layoutParams.y = 300;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;//允许外部点击

        windowManage.addView(view, layoutParams);

        networkListen();
        autoGetRssi();
    }

    private void getWifiInfo() {
        wifiName.setText(NetWorkUtil.getCurrentWifiSsid());
        wifiRssi.setText(NetWorkUtil.getWifiRssi() + "dBm");
        switch (NetWorkUtil.is24Or5()) {
            case 24:
                wifiType.setText("2.4GHz");
                break;
            case 5:
                wifiType.setText("5GHz");
                break;
            default:
                wifiType.setText("未知");
        }
        wifiPsdType.setText(NetWorkUtil.getCurrentWifiEncryptionType());
    }

    /**
     * 定时更新信号强度
     */
    private void autoGetRssi() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = WHAT_REFRESH_RSSI;
                handler.sendMessage(msg);
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 0, 100);
    }

    private void networkListen() {
        NetWorkUtil.addCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                getWifiInfo();
            }

            @Override
            public void onLost(Network network) {
                getWifiInfo();
            }
        });
    }

    private class FloatWindowTouchListener implements View.OnTouchListener {
        private int x, y;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int moveX = nowX - x;
                    int moveY = nowY - y;
                    x = nowX;
                    y = nowY;

                    //更改属性达到移动的效果
                    layoutParams.x = layoutParams.x + moveX;
                    layoutParams.y = layoutParams.y + moveY;
                    windowManage.updateViewLayout(view, layoutParams);
                    break;
            }
            return false;
        }
    }

    public class FloatNetworkUtilBinder extends Binder {
        private FloatNetworkUtilService service;

        public FloatNetworkUtilBinder(FloatNetworkUtilService service) {
            this.service = service;
        }

        public void startConnectDevice() {
            service.startConnectDevice();
        }

        public void cancelConnectDevice() {
            service.cancelConnectDevice();
        }

        public void connectDeviceTimeout() {
            service.connectDeviceTimeout();
        }

        public void connectDeviceSuccess() {
            service.connectDeviceSuccess();
        }
    }
}
