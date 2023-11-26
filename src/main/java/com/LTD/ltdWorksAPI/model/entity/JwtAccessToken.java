package com.LTD.ltdWorksAPI.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "jwt_access_token", schema = "users")
public class JwtAccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String token;

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "refresh_token_id")
    private RefreshToken refresh_token;

    @Builder.Default
    private boolean is_revoked = false;

    private Date expiry_date;
}
