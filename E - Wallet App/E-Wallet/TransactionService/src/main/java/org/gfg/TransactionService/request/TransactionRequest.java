package org.gfg.TransactionService.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {

    String receiver;
    String purpose;
    double transferAmount;
}
