import 'dart:async';
import 'dart:developer';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:notification_listener_service/notification_event.dart';

const MethodChannel methodeChannel =
    MethodChannel('x-slayer/notifications_channel');
const EventChannel _eventChannel = EventChannel('x-slayer/notifications_event');
Stream<ServiceNotificationEvent>? _stream;

class NotificationListenerService {
  NotificationListenerService._();

  /// stream the incoming notifications events
  static Stream<ServiceNotificationEvent> get notificationsStream {
    if (Platform.isAndroid) {
      _stream ??=
          _eventChannel.receiveBroadcastStream().map<ServiceNotificationEvent>(
                (event) => ServiceNotificationEvent.fromMap(event),
              );
      return _stream!;
    }
    throw Exception("Notifications API exclusively available on Android!");
  }

  // notification receiver 작동 확인
  static Future<bool> isRunningNotificationListener() async {
    try {
      return await methodeChannel.invokeMethod('isRunningNotificationListener');
    } on PlatformException catch (error) {
      log("$error");
      return Future.value(false);
    }
  }

  // 서비스 중지 되었을때 재시작
  static Future<bool> restartNotificationListener() async {
    try {
      return await methodeChannel.invokeMethod('restartService');
    } on PlatformException catch (error) {
      log("$error");
      return Future.value(false);
    }
  }

  /// request notification permission
  /// it will open the notification settings page and return `true` once the permission granted.
  static Future<bool> requestPermission() async {
    try {
      return await methodeChannel.invokeMethod('requestPermission');
    } on PlatformException catch (error) {
      log("$error");
      return Future.value(false);
    }
  }

  /// check if notification permission is enebaled
  static Future<bool> isPermissionGranted() async {
    try {
      return await methodeChannel.invokeMethod('isPermissionGranted');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }

  // getDefaultSmsPackageName
  static Future<String> getDefaultSmsPackageName() async {
    try {
      return await methodeChannel.invokeMethod('getDefaultSmsPackageName');
    } on PlatformException catch (error) {
      log("$error");
      return "";
    }
  }

  // getDefaultSmsAppName
  static Future<String> getDefaultSmsAppName() async {
    try {
      return await methodeChannel.invokeMethod('getDefaultSmsAppName');
    } on PlatformException catch (error) {
      log("$error");
      return "";
    }
  }
}
