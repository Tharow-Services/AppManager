package io.github.muntashirakon.AppManager.apk.installer;

import static io.github.muntashirakon.AppManager.utils.UIUtils.displayLongToast;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandleHidden;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import io.github.muntashirakon.AppManager.BaseActivity;
import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.compat.ActivityManagerCompat;
import io.github.muntashirakon.AppManager.compat.PackageManagerCompat;
import io.github.muntashirakon.AppManager.self.SelfPermissions;
import io.github.muntashirakon.AppManager.utils.ThreadUtils;
import io.github.muntashirakon.AppManager.utils.UIUtils;
import io.github.muntashirakon.dialog.ScrollableDialogBuilder;

public class PackageUninstallerActivity extends BaseActivity {
    public static final String TAG = PackageUninstallerActivity.class.getSimpleName();
    private String mPackageName;
    private ApplicationInfo mApplicationInfo;
    private BaseActivity mActivity;
    private String mAppLabel;
    private int mUserId;
    private boolean isUninstallFromAllUsers;


    @RequiresPermission(android.Manifest.permission.REQUEST_DELETE_PACKAGES)
    @Override
    protected void onAuthenticated(@Nullable Bundle savedInstanceState) {
        this.mPackageName = getIntent().getDataString().replaceFirst("package:", "");
        this.isUninstallFromAllUsers = getIntent().getBooleanExtra(PackageInstallerCompat.EXTRA_UNINSTALL_ALL_USERS, false);
        this.mActivity = this;
        this.mUserId = UserHandleHidden.myUserId();
        try {
            this.mApplicationInfo = PackageManagerCompat.getApplicationInfo(mPackageName, ApplicationInfo.FLAG_INSTALLED, mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.mAppLabel = mApplicationInfo.name;
        uninstallDialog();
    }

    private void uninstallDialog() {
        if (mUserId != UserHandleHidden.myUserId() && !SelfPermissions.checkSelfOrRemotePermission(Manifest.permission.DELETE_PACKAGES)) {
            // Could be for work profile
            try {
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                uninstallIntent.setData(Uri.parse("package:" + mPackageName));
                ActivityManagerCompat.startActivity(uninstallIntent, mUserId);
                // TODO: 19/8/24 Watch for uninstallation
            } catch (Throwable th) {
                UIUtils.displayLongToast("Error: " + th.getLocalizedMessage());
            }
            return;
        }
        final boolean isSystemApp = (mApplicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        ScrollableDialogBuilder builder = new ScrollableDialogBuilder(mActivity,
                isSystemApp ? R.string.uninstall_system_app_message : R.string.uninstall_app_message)
                .setTitle(mAppLabel)
                // FIXME: 16/6/23 Does it even work without INSTALL_PACKAGES?
                .setCheckboxLabel(R.string.keep_data_and_app_signing_signatures)
                .setPositiveButton(R.string.uninstall, (dialog, which, keepData) -> ThreadUtils.postOnBackgroundThread(() -> {
                    PackageInstallerCompat installer = PackageInstallerCompat.getNewInstance();
                    installer.setAppLabel(mAppLabel);
                    boolean uninstalled = installer.uninstall(mPackageName, mUserId, keepData);
                    ThreadUtils.postOnMainThread(() -> {
                        if (uninstalled) {
                            displayLongToast(R.string.uninstalled_successfully, mAppLabel);
                            mActivity.finish();
                        } else {
                            displayLongToast(R.string.failed_to_uninstall, mAppLabel);
                        }
                    });
                }))
                .setNegativeButton(R.string.cancel, (dialog, which, keepData) -> {
                    if (dialog != null) dialog.cancel();
                });
        if ((mApplicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            builder.setNeutralButton(R.string.uninstall_updates, (dialog, which, keepData) ->
                    ThreadUtils.postOnBackgroundThread(() -> {
                        PackageInstallerCompat installer = PackageInstallerCompat.getNewInstance();
                        installer.setAppLabel(mAppLabel);
                        boolean isSuccessful = installer.uninstall(mPackageName, UserHandleHidden.USER_ALL, keepData);
                        if (isSuccessful) {
                            ThreadUtils.postOnMainThread(() -> displayLongToast(R.string.update_uninstalled_successfully, mAppLabel));
                        } else {
                            ThreadUtils.postOnMainThread(() -> displayLongToast(R.string.failed_to_uninstall_updates, mAppLabel));
                        }
                    }));
        }
        builder.show();
    }
}
