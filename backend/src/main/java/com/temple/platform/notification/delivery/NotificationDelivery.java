package com.temple.platform.notification.delivery;

import com.temple.platform.notification.domain.Notification;

public interface NotificationDelivery {

    void deliver(Notification notification);
}
