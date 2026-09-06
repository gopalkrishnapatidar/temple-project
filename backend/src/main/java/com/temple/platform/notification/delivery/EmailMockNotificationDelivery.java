package com.temple.platform.notification.delivery;

import com.temple.platform.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailMockNotificationDelivery implements NotificationDelivery {

    private static final Logger log = LoggerFactory.getLogger(EmailMockNotificationDelivery.class);

    @Override
    public void deliver(Notification notification) {
        log.info(
                "EMAIL_MOCK delivery accountId={} notificationReference={} type={} title={} message={}",
                notification.accountId(),
                notification.notificationReference(),
                notification.type(),
                notification.title(),
                notification.message()
        );
    }
}
