package com.LTD.ltdWorksAPI.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordDTO {
    @NotBlank
    private String token;
    @NotBlank
    private String new_password;
    @NotBlank
    private String new_password_duplicate;
}
