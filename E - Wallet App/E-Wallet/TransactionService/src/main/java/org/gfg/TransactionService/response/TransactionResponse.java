package org.gfg.TransactionService.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionResponse {

    String sentTo;
    Date txnTime;
    double amount;
    String txnType;
}
