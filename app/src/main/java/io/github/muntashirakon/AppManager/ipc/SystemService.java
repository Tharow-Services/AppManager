package io.github.muntashirakon.AppManager.ipc;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.SystemCleaner;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

import aosp.android.content.pm.ParceledListSlice;
import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.IAMService;
import io.github.muntashirakon.AppManager.IRemoteProcess;
import io.github.muntashirakon.AppManager.IRemoteShell;
import io.github.muntashirakon.AppManager.compat.ProcessCompat;
import io.github.muntashirakon.AppManager.ipc.ps.Ps;
import io.github.muntashirakon.AppManager.logs.Log;
import io.github.muntashirakon.AppManager.server.common.IRootServiceManager;
import io.github.muntashirakon.AppManager.servermanager.ServerConfig;
import io.github.muntashirakon.AppManager.utils.ThreadUtils;
import io.github.muntashirakon.compat.os.ParcelCompat2;

public class SystemService extends JobService {
    private static final String TAG = SystemService.class.getSimpleName();

    static class IAMServiceImpl extends IAMService.Stub {

        @Override
        public IRemoteProcess newProcess(String[] cmd, String[] env, String dir) throws RemoteException {
            Process process;
            try {
                process = Runtime.getRuntime().exec(cmd, env, dir!=null ? new File(dir) : null);
            } catch (Exception e) {
                throw new RemoteException(e.getMessage());
            }
            return new RemoteProcessImpl(process);
        }

        @Override
        public IRemoteShell getShell(String[] cmd) throws RemoteException {
            return new RemoteShellImpl(cmd);
        }

        @Override
        public ParceledListSlice getRunningProcesses() throws RemoteException {
            Ps ps = new Ps();
            ps.loadProcesses();
            return new ParceledListSlice<>(ps.getProcesses());
        }

        @Override
        public int getUid() throws RemoteException {
            return android.os.Process.myUid();
        }

        @Override
        public void symlink(String file, String link) throws RemoteException {
            try {
                Os.symlink(file, link);
            } catch (ErrnoException e) {
                throw new RemoteException(e.getMessage());
            }
        }

        @Override
        public IBinder getService(String serviceName) throws RemoteException {
            return ServiceManager.getService(serviceName);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == ProxyBinder.PROXY_BINDER_TRANSACTION) {
                data.enforceInterface(IRootServiceManager.class.getName());
                transactRemote(data, reply);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
        /**
         * Call target Binder received through {@link ProxyBinder}.
         *
         * @author Rikka
         */
        private void transactRemote(@NonNull Parcel data, @Nullable Parcel reply) throws RemoteException {
            IBinder targetBinder = data.readStrongBinder();
            int targetCode = data.readInt();
            int targetFlags = data.readInt();

            Parcel newData = ParcelCompat2.obtain(targetBinder);
            try {
                newData.appendFrom(data, data.dataPosition(), data.dataAvail());
                long id = Binder.clearCallingIdentity();
                targetBinder.transact(targetCode, newData, reply, targetFlags);
                Binder.restoreCallingIdentity(id);
            } catch (RemoteException e) {
                throw e;
            } catch (Throwable th) {
                throw (RemoteException) new RemoteException(th.getMessage()).initCause(th);
            } finally {
                newData.recycle();
            }
        }
    }

    @Nullable
    public IBinder onBindHidden(Intent intent) {
        Log.d(TAG, "AMService: onBind");
        return new IAMServiceImpl();
    }

    private static class SystemProcessExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent intent = new Intent();
        ServiceConnectionWrapper wrapper = new ServiceConnectionWrapper(BuildConfig.APPLICATION_ID, AMService.class.getName());
        try {
            Shell.Task task = RootService.bindOrTask(intent, new SystemProcessExecutor(), wrapper);
            Process runtime = Runtime.getRuntime().exec("sh", null, null);
            if (task==null) {
                return false;
            }
            task.run(runtime.getOutputStream(), runtime.getInputStream(), runtime.getErrorStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }
}
