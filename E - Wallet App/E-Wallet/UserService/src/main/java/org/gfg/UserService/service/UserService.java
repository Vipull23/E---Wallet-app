package org.gfg.UserService.service;

import org.gfg.CommonConstants;
import org.gfg.UserService.config.RedisUtil;
import org.gfg.UserService.feign.NotificationFeign;
import org.gfg.UserService.model.RoleType;
import org.gfg.UserService.model.User;
import org.gfg.UserService.model.UserStatus;
import org.gfg.UserService.repository.UserRepository;
import org.gfg.UserService.request.OTPVerificationRequest;
import org.gfg.UserService.request.UserCreationRequest;
import org.gfg.UserService.response.UserCreationAcknowledgement;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    PasswordEncoder passwordEncoder;


    @Autowired
    UserRepository userRepository;

    @Autowired
    NotificationFeign notificationFeign;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    AddressRepository addressRepository;
    public UserCreationAcknowledgement onboardNewUser(UserCreationRequest userCreationRequest){

        User user = User.builder().name(userCreationRequest.getName()).email(userCreationRequest.getEmail())
                .mobileNo(userCreationRequest.getMobileNo()).dob(userCreationRequest.getDob()).userIdentifier(userCreationRequest.getUserIdentifier())
                .userIdentifierValue(userCreationRequest.getUserIdentifierValue()).userStatus(UserStatus.ACTIVE).build();

        user.setPassword(passwordEncoder.encode(userCreationRequest.getPassword()));
        user.setRole(RoleType.NORMAL);

        String message = notificationFeign.sendOTP(userCreationRequest.getEmail());
        UserCreationAcknowledgement userCreationAcknowledgement = new UserCreationAcknowledgement();
        if ("OK".equals(message)){
            userCreationAcknowledgement.setMessage("OTP Sent");
            userCreationAcknowledgement.setMobileNo(user.getMobileNo());
            userCreationAcknowledgement.setStatus("SUCCESS");
            redisUtil.setData(user.getMobileNo()+"USER", user);
            System.out.println("OTP sent successfully and data saved in cache");

        }else {
            userCreationAcknowledgement.setMessage("OTP Not Sent");
            userCreationAcknowledgement.setMobileNo(user.getMobileNo());
            userCreationAcknowledgement.setStatus("Failed");
        }
        return userCreationAcknowledgement;
    }


    public String fetchAndReturnUser(String username){
      User user =  userRepository.findByEmail(username);
      if (user==null){
          return null;
      }
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(CommonConstants.USER_EMAIL, user.getEmail());
        jsonObject.put(CommonConstants.USER_MOBILE, user.getMobileNo());
        jsonObject.put(CommonConstants.USER_PASSWORD,user.getPassword());

      return jsonObject.toString();
    }



    public User validateAndSaveUser(OTPVerificationRequest otpVerificationRequest){
        String otp = otpVerificationRequest.getOtp();
        String key = otpVerificationRequest.getEmail()+"OTP";
        String savedOTP = redisUtil.getOTP(key);
        System.out.println("Saved OTP: "+savedOTP);
        if (otp.equals(savedOTP)){
            // fetch the data from redis and save in database
            String userKey = otpVerificationRequest.getMobile()+"USER";
            User user = redisUtil.getData(userKey);
            System.out.println("Data from Redis: "+user);
           User savedUser = userRepository.save(user);
           String jsonData = createKafkaData(user);
           kafkaTemplate.send(CommonConstants.USER_CREATION_TOPIC,jsonData);
            System.out.println("Data send to kafka");
           return savedUser;
        }else {
            return null;
        }
    }


    public String createKafkaData(User user){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CommonConstants.USER_ID,user.getId());
        jsonObject.put(CommonConstants.USER_NAME, user.getName());
        jsonObject.put(CommonConstants.USER_EMAIL,user.getEmail());
        jsonObject.put(CommonConstants.USER_MOBILE,user.getMobileNo());
        jsonObject.put(CommonConstants.USER_IDENTIFIER,user.getUserIdentifier());
        jsonObject.put(CommonConstants.USER_IDENTIFIER_VALUE,user.getUserIdentifierValue());

        return jsonObject.toString();
    }

}
