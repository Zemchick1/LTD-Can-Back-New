package com.LTD.ltdWorksAPI.model.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Data
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "confirmation_token", schema = "users")
public class ConfirmationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String token;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Builder.Default
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate = new Date();
}
