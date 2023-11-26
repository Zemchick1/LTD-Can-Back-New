package com.LTD.ltdWorksAPI.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "refresh_token", schema = "users")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String token;

    @Builder.Default
    private boolean is_revoked = false;

    @Column
    @Builder.Default
    private Date expiry_date =
            new Date(System.currentTimeMillis() +
                    30L * 24 * 60 * 60 * 1000 * 12); // 1 year

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name="user_id")
    private User user;

    @OneToMany(mappedBy = "refresh_token")
    private List<JwtAccessToken> jwtAccessToken;
}