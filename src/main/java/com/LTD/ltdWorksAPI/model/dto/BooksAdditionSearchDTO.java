package com.LTD.ltdWorksAPI.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BooksAdditionSearchDTO {
    private String searchText;
    private String category;

}
