package org.gfg.NotificationService.consumer;

import org.gfg.CommonConstants;
import org.gfg.NotificationService.worker.NotificationWorker;
import org.gfg.model.UserIdentifier;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserConsumer {

    @Autowired
    NotificationWorker notificationWorker;


    @KafkaListener(topics = "USER_CREATION_TOPIC", groupId = "EMAIL_GROUP")
    public void listenNewlyCreatedUser(String data){
        System.out.println("Consumed data from kafka: "+data);
        JSONObject jsonObject = new JSONObject(data);
        String name = jsonObject.optString(CommonConstants.USER_NAME);
        String email = jsonObject.optString(CommonConstants.USER_EMAIL);
        UserIdentifier userIdentifier  = jsonObject.optEnum(UserIdentifier.class,CommonConstants.USER_IDENTIFIER);
        String userIdentifierValue = jsonObject.optString(CommonConstants.USER_IDENTIFIER_VALUE);

        notificationWorker.sendNotification(name,email,userIdentifier,userIdentifierValue);

    }
}
