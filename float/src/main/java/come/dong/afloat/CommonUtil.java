package come.dong.afloat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Method;

/**
 * @author pd
 * time     2019/4/7 11:28
 * 一些无法分类的工具
 */
public class CommonUtil {
    private static final String TAG = "CommonUtil";

    /**
     * 设置屏幕的背景透明度
     *
     * @param bgAlpha 屏幕透明度0.0-1.0 1表示完全不透明
     */
    public static void setBackgroundAlpha(float bgAlpha, Activity mContext) {
        WindowManager.LayoutParams lp = ((Activity) mContext).getWindow()
                .getAttributes();
        lp.alpha = bgAlpha;
        ((Activity) mContext).getWindow().setAttributes(lp);
    }

    /**
     * 通过反射，获取包含虚拟键的整体屏幕高度
     * 该高度包含了状态栏的高度
     *
     * @return
     */
    public static int getHasVirtualKeyHeight(Context context) {
        int dpi = 0;
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        @SuppressWarnings("rawtypes")
        Class c;
        try {
            c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            dpi = dm.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dpi;
    }

    /**
     * 获取状态栏高度
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    /**
     * 获取屏幕宽高
     * 这里的高度是实际应用显示的高度，排除了底部按键以及状态栏高度
     *
     * @param context 用来获取Resource的上下文环境
     * @return 数组，[宽度，高度]，单位px
     */
    public static int[] getScreenWH(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
//        float density = dm.density;//密度
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        return new int[]{width, height};
    }

    /**
     * 获取包含底部虚拟按键以及排除了状态栏的高度的高度
     *
     * @param context 上下文
     * @return 数组，[宽度，高度]，单位px
     */
    public static int[] getRealScreenWH(Context context) {
        int[] screenWH = getScreenWH(context);
        screenWH[1] = getHasVirtualKeyHeight(context) - getStatusBarHeight(context);
        return screenWH;
    }

    /**
     * 显示/隐藏软键盘
     */
    private void toggleInput(Context context) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 强制隐藏软键盘
     *
     * @param view 用于获取WindowToken
     */
    private void hideInput(Context context, View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 检测辅助功能是否开启
     *
     * @param mContext    上下文环境
     * @param helpService 辅助功能服务类
     * @return boolean
     */
    public static boolean isAccessibilitySettingsOn(Context mContext, Class helpService) {
        int accessibilityEnabled = 0;
        // HelpService为对应的服务
        final String service = mContext.getPackageName() + "/" + helpService.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }

    /**
     * 判断是否有悬浮窗权限
     *需要在Ams.xml中声明SYSTEM_ALERT_WINDOW
     * @param context 上下文
     * @return true/false
     */
    public static boolean canDrawOverLays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            return true;
        }
    }

    /**
     * 跳转去授权悬浮窗界面
     *
     * @param context 上下文用来跳转活动
     */
    public static void getOverLayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }
    }
}
