package com.LTD.ltdWorksAPI.config;

import com.LTD.ltdWorksAPI.exception.GlobalExceptionHandler;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TokenCleanUp {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Scheduled(cron = "0 0 0 * * *")
    public void ClearAllRevokedTokens(){
        String refresh_token_delete_sql = """
                DELETE FROM users.refresh_token
                WHERE (is_revoked = true OR expiry_date <= CURRENT_TIMESTAMP);
                                     """;

        String jwt_access_token_delete_sql = """
                DELETE FROM users.jwt_access_token
                WHERE expiry_date <= CURRENT_TIMESTAMP;
                                     """;

        int rowsAffected = jdbcTemplate.update(refresh_token_delete_sql);
        rowsAffected += jdbcTemplate.update(jwt_access_token_delete_sql);
        log.info("Cleanup done. Rows affected = " + rowsAffected);
    }
}
