package org.gfg.TransactionService.service;

import org.gfg.CommonConstants;
import org.gfg.TransactionService.model.Transaction;
import org.gfg.TransactionService.model.TxnStatus;
import org.gfg.TransactionService.repository.TransactionRepository;
import org.gfg.TransactionService.request.TransactionRequest;
import org.gfg.TransactionService.response.TransactionResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    public String initiateTransaction(TransactionRequest transactionRequest, String senderId){
        String receiverid = transactionRequest.getReceiver();
        double amount = transactionRequest.getTransferAmount();
        String purpose = transactionRequest.getPurpose();

        Transaction transaction = Transaction.builder().senderId(senderId).receiverId(receiverid)
                .purpose(purpose).transferAmount(amount).txnMessage("Transaction Initiated")
                .txnStatus(TxnStatus.INITIATE).build();

        String txnid = UUID.randomUUID().toString();
        transaction.setTxnId(txnid);


        transactionRepository.save(transaction);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CommonConstants.SENDER_ID, transaction.getSenderId());
        jsonObject.put(CommonConstants.RECEIVER_ID, transaction.getReceiverId());
        jsonObject.put(CommonConstants.TRANSACTION_AMOUNT, transaction.getTransferAmount());
        jsonObject.put(CommonConstants.TRANSACTION_ID, transaction.getTxnId());

        kafkaTemplate.send("TXN_TOPIC", jsonObject.toString());

        System.out.println("data saved and sent to kafka");

        return txnid;
    }


    public void updateTransaction(String txnId,TxnStatus status,String message){
        System.out.println("txnid: "+txnId);
        transactionRepository.updateTransaction(txnId,status,message);

    }



    public List<TransactionResponse> getTransactionResponse(String user){
       List<Transaction> transactionList = transactionRepository.findBySenderIdOrReceiverId(user,user);

       List<TransactionResponse> ans = new ArrayList<>();

       for (Transaction t: transactionList){
           TransactionResponse transactionResponse = new TransactionResponse();
           transactionResponse.setSentTo(t.getReceiverId());
           transactionResponse.setAmount(t.getTransferAmount());
           transactionResponse.setTxnTime(t.getCreatedOn());
           if (t.getSenderId().equals(user)){
               transactionResponse.setTxnType("USER_DEBIT");
           }else {
               transactionResponse.setTxnType("USER_CREDIT");
           }
           ans.add(transactionResponse);
       }
       return ans;
    }
}
