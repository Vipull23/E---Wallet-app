package org.gfg.TransactionService.controller;

import org.gfg.TransactionService.request.TransactionRequest;
import org.gfg.TransactionService.response.TransactionResponse;
import org.gfg.TransactionService.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transaction-service")
public class TransactionController {

    @Autowired
    TransactionService transactionService;

    @PostMapping("/create/txn")
    public ResponseEntity<String> initiateTransaction(@RequestBody TransactionRequest transactionRequest){
        if (transactionRequest==null){
            return new ResponseEntity<>("Invalid Request", HttpStatus.OK);
        }

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String response = transactionService.initiateTransaction(transactionRequest, userDetails.getUsername());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("/get/transaction/history")
    public List<TransactionResponse> getTransactionHistory(){
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();

        return transactionService.getTransactionResponse(currentUser);
    }
}
