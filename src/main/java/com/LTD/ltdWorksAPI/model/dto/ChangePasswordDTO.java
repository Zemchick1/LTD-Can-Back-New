package com.LTD.ltdWorksAPI.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangePasswordDTO {
    @NotBlank
    private String old_password;
    @NotBlank
    private String new_password;
    @NotBlank
    private String new_password_duplicate;
}
