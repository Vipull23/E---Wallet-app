package org.gfg.WalletService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gfg.model.UserIdentifier;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "wallet")
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column(unique = true)
    int userId;
    @Column(unique = true)
    String mobileNo;
    @Enumerated(EnumType.STRING)
    WalletStatus walletStatus;

    @Enumerated(EnumType.STRING)
    UserIdentifier userIdentifier;

    String userIdentifierValue;
    double balance;

    @CreationTimestamp
    Date createdOn;
    @UpdateTimestamp
    Date updatedOn;
}
