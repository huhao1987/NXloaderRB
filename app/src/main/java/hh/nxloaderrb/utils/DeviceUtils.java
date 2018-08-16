package hh.nxloaderrb.utils;

import android.os.Build;
import android.util.Log;


import org.json.JSONObject;

import java.lang.reflect.Field;

import hh.nxloaderrb.BuildConfig;

/**
 * Created by laole918 on 2016/4/23.
 */
public class DeviceUtils {

    public static final String TAG = DeviceUtils.class.getSimpleName();

    public static String getDeviceInfo() {
        JSONObject jsonObject = new JSONObject();
        // 使用反射来收集设备信息.在Build类中包含各种设备信息,
        // 例如: 系统版本号,设备生产商 等帮助调试程序的有用信息
        // 返回 Field 对象的一个数组，这些对象反映此 Class 对象所表示的类或接口所声明的所有字段
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                // setAccessible(boolean flag)
                // 将此对象的 accessible 标志设置为指示的布尔值。
                // 通过设置Accessible属性为true,才能对私有变量进行访问，不然会得到一个IllegalAccessException的异常
                field.setAccessible(true);
                jsonObject.put(field.getName(), field.get(null));
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, field.getName() + " : " + field.get(null));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while collect crash info", e);
            }
        }
        return jsonObject.toString();
    }
}
