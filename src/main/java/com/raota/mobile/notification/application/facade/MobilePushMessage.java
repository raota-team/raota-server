package com.raota.mobile.notification.application.facade;

import java.util.Map;

public record MobilePushMessage(String title, String body, Map<String, String> data) {}
