package org.gfg.UserService.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(url = "http://localhost:8092", name = "NOTIFICATIONSERVICE")
public interface NotificationFeign {

    @PostMapping("/notification/generate/otp/{email}")
    String sendOTP(@PathVariable("email") String email);
}
