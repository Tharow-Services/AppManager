package io.github.muntashirakon.AppManager.dpc;

import android.app.admin.DeviceAdminService;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.O)
public class DpcService extends DeviceAdminService {
    public DpcService() {
        super();
    }

    @Override
    public void revokeSelfPermissionOnKill(@NonNull String permName) {
        super.revokeSelfPermissionOnKill(permName);
    }
}
