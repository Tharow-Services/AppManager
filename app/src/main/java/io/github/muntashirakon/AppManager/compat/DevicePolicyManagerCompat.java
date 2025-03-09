package io.github.muntashirakon.AppManager.compat;

import android.app.admin.DevicePolicyManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import io.github.muntashirakon.AppManager.utils.ContextUtils;

@RequiresApi(Build.VERSION_CODES.M)
public class DevicePolicyManagerCompat {
    public static DevicePolicyManager getDevicePolicyManager() {
        return ContextUtils.getContext().getSystemService(DevicePolicyManager.class);
    }

}
