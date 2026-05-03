package org.gfg.WalletService.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "USERSERVICE", url = "http://localhost:8091/user-service")
public interface UserFeign {

    @GetMapping("/validate/user/{userId}")
    public String validateUser(@PathVariable("userId") String userId);
}
