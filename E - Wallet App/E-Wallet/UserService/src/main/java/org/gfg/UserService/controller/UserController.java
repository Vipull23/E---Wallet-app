package org.gfg.UserService.controller;

import jakarta.validation.Valid;
import org.gfg.UserService.model.User;
import org.gfg.UserService.request.OTPVerificationRequest;
import org.gfg.UserService.request.OTPVerificationResponse;
import org.gfg.UserService.request.UserCreationRequest;
import org.gfg.UserService.response.UserCreationAcknowledgement;
import org.gfg.UserService.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-service")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/onboard/user")
    public ResponseEntity<UserCreationAcknowledgement> onboardNewUser(@RequestBody @Valid UserCreationRequest userCreationRequest){
        UserCreationAcknowledgement userCreationAcknowledgement = new UserCreationAcknowledgement();

        if (!userCreationRequest.getEmail().contains("@")){
            userCreationAcknowledgement.setMessage("Please pass valid email");
        }else if (!userCreationRequest.getDob().contains("/")){
            userCreationAcknowledgement.setMessage("Please pass correct DOB");
        }else if (userCreationRequest.getMobileNo().length()>9 && userCreationRequest.getMobileNo().length()<14){
            userCreationAcknowledgement.setMessage("Invalid Mobile No");
        }

        userCreationAcknowledgement = userService.onboardNewUser(userCreationRequest);
        return new ResponseEntity<>(userCreationAcknowledgement, HttpStatus.OK);
    }

    @PostMapping("/validate/otp")
    public ResponseEntity<OTPVerificationResponse> validateOTPAndSaveUser(@RequestBody OTPVerificationRequest otpVerificationRequest){
      User user =  userService.validateAndSaveUser(otpVerificationRequest);
      OTPVerificationResponse otpVerificationResponse = new OTPVerificationResponse();
      otpVerificationResponse.setStatus("SUCCESS");
      if (user==null){
          otpVerificationResponse.setStatus("FAILED");
      }
        otpVerificationResponse.setSavedUser(user);
      return new ResponseEntity<>(otpVerificationResponse,HttpStatus.OK);

    }

    @GetMapping("/validate/user/{userId}")
    public String validateUser(@PathVariable("userId") String userId){
        System.out.println("Request recived");
       return userService.fetchAndReturnUser(userId);
    }
}
