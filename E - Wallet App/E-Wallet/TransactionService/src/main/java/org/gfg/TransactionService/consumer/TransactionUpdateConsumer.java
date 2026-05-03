package org.gfg.TransactionService.consumer;

import org.gfg.CommonConstants;
import org.gfg.TransactionService.model.TxnStatus;
import org.gfg.TransactionService.repository.TransactionRepository;
import org.gfg.TransactionService.service.TransactionService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionUpdateConsumer {

    @Autowired
   TransactionService transactionService;

    @KafkaListener(topics = "TXN_UPDATE_TOPIC", groupId = "txn-update-group")
    public void listenUpdatedTransactions(String data){
        System.out.println("Update txn details received: "+data);

        JSONObject jsonObject = new JSONObject(data);
        String senderId = jsonObject.optString(CommonConstants.SENDER_ID);
        String receiverId = jsonObject.optString(CommonConstants.RECEIVER_ID);
        String txnId = jsonObject.optString(CommonConstants.TRANSACTION_ID);
        System.out.println("recived txn id: "+txnId);
        String txnStatus = jsonObject.optString(CommonConstants.TRANSACTION_STATUS);
        String message = jsonObject.optString(CommonConstants.TRANSACTION_MESSAGE);


        TxnStatus status = null;
        if ("FAILED".equals(txnStatus)){
            status = TxnStatus.FAILED;
        }else if ("SUCCESS".equals(txnStatus)){
            status = TxnStatus.SUCCESS;
        }else {
            status = TxnStatus.PENDING;
        }

        transactionService.updateTransaction(txnId,status,message);

        System.out.println("Transaction updated");

    }
}
