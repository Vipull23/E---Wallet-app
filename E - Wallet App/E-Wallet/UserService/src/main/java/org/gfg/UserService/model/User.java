package org.gfg.UserService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gfg.model.UserIdentifier;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String name;

    @Column(unique = true)
    String email;

    String password;

    @Column(unique = true)
    String mobileNo;

    String dob;
    @Enumerated(EnumType.STRING)

    RoleType role;

    @Enumerated(value = EnumType.STRING)
    UserIdentifier userIdentifier;

    @Column(unique = true)
    String userIdentifierValue;

    @Enumerated(value = EnumType.STRING)
    UserStatus userStatus;

    @CreationTimestamp
    Date createdOn;

    @UpdateTimestamp
    Date updatedOn;
}
