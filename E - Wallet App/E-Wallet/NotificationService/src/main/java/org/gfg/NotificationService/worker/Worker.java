package org.gfg.NotificationService.worker;

import org.springframework.stereotype.Component;

@Component
public interface Worker {

     void sendNotification(String email);
}
