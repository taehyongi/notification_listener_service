package notification.listener.service;

import static notification.listener.service.NotificationConstants.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.RequiresApi;

import io.flutter.plugin.common.EventChannel.EventSink;

import java.util.HashMap;

public class NotificationReceiver extends BroadcastReceiver {

    private EventSink eventSink;

    public NotificationReceiver(EventSink eventSink) {
        this.eventSink = eventSink;
        Log.d("NotificationReceiver 1", eventSink.toString());
    }

    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver 2", "onReceive");
        String packageName = intent.getStringExtra(PACKAGE_NAME);
        String appName = intent.getStringExtra(APP_NAME);
        String title = intent.getStringExtra(NOTIFICATION_TITLE);
        String content = intent.getStringExtra(NOTIFICATION_CONTENT);
        byte[] notificationIcon = intent.getByteArrayExtra(NOTIFICATIONS_ICON);
        byte[] notificationExtrasPicture = intent.getByteArrayExtra(EXTRAS_PICTURE);
        byte[] largeIcon = intent.getByteArrayExtra(NOTIFICATIONS_LARGE_ICON);
        boolean haveExtraPicture = intent.getBooleanExtra(HAVE_EXTRA_PICTURE, false);
        boolean hasRemoved = intent.getBooleanExtra(IS_REMOVED, false);
        boolean canReply = intent.getBooleanExtra(CAN_REPLY, false);
        int id = intent.getIntExtra(ID, -1);

        Log.d("NotificationReceiver 3", "id: " + String.valueOf(id));


        HashMap<String, Object> data = new HashMap<>();
        data.put("type", "push");
        data.put("id", id);
        data.put("packageName", packageName);
        data.put("appName", appName);
        data.put("title", title);
        data.put("content", content);
        data.put("notificationIcon", notificationIcon);
        data.put("notificationExtrasPicture", notificationExtrasPicture);
        data.put("haveExtraPicture", haveExtraPicture);
        data.put("largeIcon", largeIcon);
        data.put("hasRemoved", hasRemoved);
        data.put("canReply", canReply);

        eventSink.success(data);
    }
}
