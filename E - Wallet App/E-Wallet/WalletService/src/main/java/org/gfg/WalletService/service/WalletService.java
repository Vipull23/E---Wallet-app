package org.gfg.WalletService.service;

import org.gfg.CommonConstants;
import org.gfg.WalletService.model.Wallet;
import org.gfg.WalletService.model.WalletStatus;
import org.gfg.WalletService.repository.WalletRepository;
import org.gfg.model.UserIdentifier;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {


    @Value("${wallet.initial.amount}")
    private String walletBalance;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    String status;
    String message;


    public void createWalletAccount(String mobile, UserIdentifier userIdentifier, String userIdentifierValue, int userid){

        Wallet wallet = Wallet.builder().userId(userid).mobileNo(mobile).userIdentifier(userIdentifier).userIdentifierValue(userIdentifierValue).build();
        wallet.setWalletStatus(WalletStatus.ACTIVE);
        wallet.setBalance(Double.parseDouble(walletBalance));

        walletRepository.save(wallet);

        System.out.println("Wallet Account created");


    }


    @Transactional
    public void updateWalletBalance(String senderId, String receiverId, double amount, String txnId){
        Wallet senderWallet = walletRepository.findByMobileNo(senderId);
        Wallet receiverWallet = walletRepository.findByMobileNo(receiverId);

        if (receiverWallet!=null && receiverWallet.getWalletStatus().equals(WalletStatus.ACTIVE)){
            if (receiverWallet.getBalance()>amount){
                System.out.println("going to update the balance");
                if (updateWalletBalance(senderId,receiverId,amount)){
                    status = "SUCCESS";
                    message = "Transaction is success";
                }else {
                    status = "PENDING";
                    message = "Transaction is pending";
                }
            }
            else {
                status = "FAILED";
                message = "Insufficient Balance";
            }
        }else if (receiverWallet==null){
            status = "FAILED";
            message = "receiver wallet doesn't exist";
        }else {
            status = "FAILED";
            message = "receiver account is blocked";
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CommonConstants.TRANSACTION_AMOUNT,amount);
        jsonObject.put(CommonConstants.TRANSACTION_ID,txnId);
        jsonObject.put(CommonConstants.SENDER_ID,senderId);
        jsonObject.put(CommonConstants.RECEIVER_ID,receiverId);
        jsonObject.put(CommonConstants.TRANSACTION_STATUS, status);
        jsonObject.put(CommonConstants.TRANSACTION_MESSAGE,message);

        kafkaTemplate.send("TXN_UPDATE_TOPIC", jsonObject.toString());

        System.out.println("Updated data send to Kafka: "+jsonObject.toString());

    }

    @Transactional
    public boolean updateWalletBalance(String sender, String receiver, double amount){
        boolean isUpdated = true;
        System.out.println("sender id "+sender);
        try {
            walletRepository.updateSenderWalletBalance(sender, amount);
            walletRepository.updateWalletBalance(receiver, amount);
        }
        catch (Exception exception){
            System.out.println("Some exception");
            isUpdated = false;
        }
        return isUpdated;
    }


    public String getWalletBalance(String username){
      double balance =  walletRepository.findByMobileNo(username).getBalance();
      return Double.toString(balance);
    }
}
