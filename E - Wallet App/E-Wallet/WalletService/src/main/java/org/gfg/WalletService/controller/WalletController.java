package org.gfg.WalletService.controller;

import org.gfg.WalletService.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet-service")
public class WalletController {

    @Autowired
    WalletService walletService;

    @GetMapping("/get/balance")
    public Double getWalletBalance(){
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

       String balance = walletService.getWalletBalance(userDetails.getUsername());

       return Double.parseDouble(balance);
    }
}
