package notification.listener.service;

import static notification.listener.service.NotificationUtils.isPermissionGranted;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.app.ActivityManager;
import android.provider.Telephony;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import notification.listener.service.models.Action;
import notification.listener.service.models.ActionCache;
import android.annotation.SuppressLint;
import android.os.Build;


public class NotificationListenerServicePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener, EventChannel.StreamHandler {

    private static final String CHANNEL_TAG = "x-slayer/notifications_channel";
    private static final String EVENT_TAG = "x-slayer/notifications_event";

    private MethodChannel channel;
    private EventChannel eventChannel;
    private NotificationReceiver notificationReceiver;
    // private SmsReceiver smsReceiver;
    private Context context;
    private Activity mActivity;
    private String smsDefaultPackageName;
    private String smsDefaultAppName;

    private Result pendingResult;
    final int REQUEST_CODE_FOR_NOTIFICATIONS = 1199;
    private boolean resultSubmitted = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_TAG);
        channel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENT_TAG);
        eventChannel.setStreamHandler(this);

        try {
            smsDefaultPackageName = Telephony.Sms.getDefaultSmsPackage(context);
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(smsDefaultPackageName, 0);
            smsDefaultAppName = pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } finally {
            // smsDefaultPackageName 가 null 이면 공백 입력
            if(smsDefaultPackageName == null) {
                smsDefaultPackageName = "";
            }
            // smsDefaultAppName 이 null 이면 공백 입력
            if(smsDefaultAppName == null) {
                smsDefaultAppName = "";
            }
        }
          
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        pendingResult = result;
        if (call.method.equals("isPermissionGranted")) {
            result.success(isPermissionGranted(context));
        } else if (call.method.equals("requestPermission")) {
            try {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                // api 29 이하일때
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    mActivity.startActivityForResult(intent, REQUEST_CODE_FOR_NOTIFICATIONS);
                }
                else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }  
            }
            catch (Exception e) {
                Log.e("NotificationListenerServicePlugin", "Error: " + e);
            }
            // Log.d("NotificationListenerServicePlugin", "requestPermission");
        } else if (call.method.equals("sendReply")) {
            final String message = call.argument("message");
            final int notificationId = call.argument("notificationId");

            final Action action = ActionCache.cachedNotifications.get(notificationId);
            if (action == null) {
                result.error("Notification", "Can't find this cached notification", null);
            }
            try {
                action.sendReply(context, message);
                result.success(true);
            } catch (PendingIntent.CanceledException e) {
                result.success(false);
                e.printStackTrace();
            }
        } else if (call.method.equals("isRunningNotificationListener")) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                // 서비스명 로그
                // Log.d("service.service.getClassName()", service.service.getClassName());
                if ("notification.listener.service.NotificationListener".equals(service.service.getClassName())) {
                    result.success(true);
                    return;
                }
            }
            result.success(false);
        } else if (call.method.equals("getDefaultSmsPackageName")) {
            result.success(smsDefaultPackageName);
        } else if (call.method.equals("getDefaultSmsAppName")) {
            result.success(smsDefaultAppName);
        } else if(call.method.equals("restartService")) {
            Intent listenerIntent = new Intent(context, NotificationReceiver.class);
            // Intent smsIntent = new Intent(context, SmsReceiver.class);
            context.startService(listenerIntent);
            // context.startService(smsIntent);
            result.success(true);   
        }        
        else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        // Log.d("NotificationListenerServicePlugin", "onAttachedToActivity");
        this.mActivity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        // Log.d("NotificationListenerServicePlugin", "onDetachedFromActivity");
        this.mActivity = null;
    }
    @SuppressLint("WrongConstant")
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        IntentFilter intentFilter = new IntentFilter();
        // IntentFilter smsIntentFilter = new IntentFilter();
        intentFilter.addAction(NotificationConstants.INTENT);
        // smsIntentFilter.addAction(NotificationConstants.SMS_INTENT);
        notificationReceiver = new NotificationReceiver(events);

        // smsReceiver = new SmsReceiver(events, smsDefaultPackageName, smsDefaultAppName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.registerReceiver(notificationReceiver, intentFilter, Context.RECEIVER_EXPORTED);
            // context.registerReceiver(smsReceiver, smsIntentFilter, Context.RECEIVER_EXPORTED);
        }else{
            context.registerReceiver(notificationReceiver, intentFilter);
            // context.registerReceiver(smsReceiver, smsIntentFilter);
        }
        Intent listenerIntent = new Intent(context, NotificationReceiver.class);
        // Intent smsIntent = new Intent(context, SmsReceiver.class);
        context.startService(listenerIntent);
        // context.startService(smsIntent);
        Log.i("NotificationPlugin", "Started the notifications tracking service.");
    }

    @Override
    public void onCancel(Object arguments) {
        context.unregisterReceiver(notificationReceiver);
        // context.unregisterReceiver(smsReceiver);
        notificationReceiver = null;
        // smsReceiver = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        // try 처리해서 에러 발생 무시하기
        try {
            if (requestCode == REQUEST_CODE_FOR_NOTIFICATIONS) {
                if (!resultSubmitted) {
                    if (resultCode == Activity.RESULT_OK) {
                        pendingResult.success("Success");
                        resultSubmitted = true;
                    } else {
                        pendingResult.error("ERROR", "Failed", null);
                        resultSubmitted = true;
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
        
    }
}
