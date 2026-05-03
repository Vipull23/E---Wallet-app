package org.gfg.NotificationService.otp;

import org.gfg.NotificationService.worker.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
public class OTPController {

    @Autowired
   // @Qualifier("otpWorker")
    Worker worker;

    @PostMapping("/generate/otp/{email}")
    public String generateOTP(@PathVariable("email") String email){

        if (email== null || email.length()==0){
            return "NOT_SENT";
        }

        worker.sendNotification(email);
        return "OK";
    }




}
