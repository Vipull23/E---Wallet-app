package org.gfg.UserService.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserCreationAcknowledgement extends Response{
    String mobileNo;
}
