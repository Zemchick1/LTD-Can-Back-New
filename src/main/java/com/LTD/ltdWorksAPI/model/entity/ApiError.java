package com.LTD.ltdWorksAPI.model.entity;

public record ApiError(
        String message,
        int statusCode
){
}
