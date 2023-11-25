package win.lioil.bluetooth;

import android.app.Application;
import android.os.Handler;
import android.widget.Toast;

public class APP extends Application {
    private static final Handler sHandler = new Handler();
    private static Toast sToast; // 单例Toast,避免重复创建，显示时间过长
    private static APP app;

    public static APP getInstance() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    /**
     * ShowToast
     *
     * @param text the content to show
     */
    public void toast(String text, int duration) {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = Toast.makeText(this, text, duration);
        sToast.show();
    }

    public static void runUi(Runnable runnable) {
        sHandler.post(runnable);
    }
}
