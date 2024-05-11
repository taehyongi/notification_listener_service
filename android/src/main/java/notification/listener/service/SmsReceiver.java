package notification.listener.service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Build;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.annotation.RequiresApi;
import io.flutter.plugin.common.EventChannel.EventSink;

import java.util.HashMap;

public class SmsReceiver extends BroadcastReceiver {

    private EventSink eventSink;

    // sms 기본 패키지 이름
    private String SMS_PACKAGE_NAME;
    
    // sms 기본 앱 이름
    private String SMS_APP_NAME;

    public SmsReceiver() {
        // 기본 생성자

       
    }

    public SmsReceiver(EventSink eventSink, String smsDefaultPackageName, String smsDefaultAppName) {
        this.eventSink = eventSink;
        this.SMS_PACKAGE_NAME = smsDefaultPackageName;
        this.SMS_APP_NAME = smsDefaultAppName;
        Log.d("SmsReceiver eventSink", eventSink.toString());
    }

    private SmsMessage[] parseMessage(Bundle bundle) {
        Log.d("SmsReceiver", "parseMessage");
        Log.d("SmsReceiver", bundle.toString());
        Object[] objs = (Object[]) bundle.get("pdus");
        Log.d("SmsReceiver", "2");
        Log.d("SmsReceiver objs", objs.toString());
        SmsMessage[] messages = new SmsMessage[objs.length];
        Log.d("SmsReceiver", "3");

        for(int i=0; i<objs.length; i++){
            messages[i] = SmsMessage.createFromPdu((byte[])objs[i]);
        }

        Log.d("SmsReceiver", "4");

        return messages;
    }
      
    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SmsReceiver", "onReceive");

        try {
            Bundle bundle = intent.getExtras();
            SmsMessage[] messages = parseMessage(bundle);

            Log.d("SmsReceiver", "aaa");

            // 값이 있으면 첫번째 가져오기
            if (messages == null || messages.length < 1) {
                return;
            }

            Log.d("SmsReceiver", "vvv");
    
        
            HashMap<String, Object> data = new HashMap<>();
            // Add your desired data to the HashMap
            data.put("type", "sms"); 
            data.put("address", messages[0].getOriginatingAddress());
            data.put("content", messages[0].getMessageBody());
            data.put("packageName", this.SMS_PACKAGE_NAME);
            data.put("appName", this.SMS_APP_NAME); 
            // data.put("notificationIcon", notificationIcon);
            // data.put("notificationExtrasPicture", notificationExtrasPicture);
            // data.put("haveExtraPicture", haveExtraPicture);
            // data.put("largeIcon", largeIcon);
            // data.put("hasRemoved", hasRemoved);
            // data.put("canReply", canReply);

            Log.d("SmsReceiver", data.toString());

            eventSink.success(data);
        } catch (Exception e) {
            Log.e("SmsReceiver", "Error receiving broadcast", e);
        }
        
    }
}
