package io.github.muntashirakon.AppManager.dpc;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.utils.ContextUtils;

public class DpcReviewer extends DeviceAdminReceiver {

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        DevicePolicyManager manager = getManager(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.setOrganizationId(context.getString(R.string.fdroid));
        }
        super.onEnabled(context, intent);
    }

    @Nullable
    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {
        return context.getString(R.string.sort);
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        //super.onDisabled(context, intent);

        super.onEnabled(context, intent);

    }

    @Override
    public void onTransferOwnershipComplete(@NonNull Context context, @Nullable PersistableBundle bundle) {
        super.onTransferOwnershipComplete(context, bundle);
    }

    @Override
    public void onComplianceAcknowledgementRequired(@NonNull Context context, @NonNull Intent intent) {
        super.onComplianceAcknowledgementRequired(context, intent);
    }

    @Override
    public void onOperationSafetyStateChanged(@NonNull Context context, int reason, boolean isSafe) {
        super.onOperationSafetyStateChanged(context, reason, isSafe);
    }
    private static ComponentName mWho;

    public static @NonNull ComponentName getAdmin() {
        return getWhoStatic(ContextUtils.getContext());
    }
    public static @NonNull ComponentName getWhoStatic(@NonNull Context context) {
        if (mWho != null) {
            return mWho;
        }
        mWho = new ComponentName(context, DpcReviewer.class);
        return mWho;
    }
}
