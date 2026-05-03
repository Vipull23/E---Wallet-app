package org.gfg.TransactionService.config;

import org.gfg.CommonConstants;
import org.gfg.TransactionService.feign.UserFeign;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    UserFeign userFeign;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Going to make a call");
        String response = userFeign.validateUser(username);
        if (response==null){
            System.out.println("response is null");
            throw new UsernameNotFoundException("user not found");
        }
        System.out.println("Response: "+response);

        JSONObject jsonObject = new JSONObject(response);
        String password = jsonObject.optString(CommonConstants.USER_PASSWORD);
        String mobile = jsonObject.optString(CommonConstants.USER_MOBILE);

        UserDetails userDetails = User.builder().username(mobile).password(password).build();
        return userDetails;
    }
}
