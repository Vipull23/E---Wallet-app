package org.gfg.UserService.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gfg.UserService.model.User;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OTPVerificationResponse {

    User savedUser;
    String status;
}
