package com.LTD.ltdWorksAPI.model.dto;

import com.LTD.ltdWorksAPI.model.entity.Book;
import com.LTD.ltdWorksAPI.model.entity.Filter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchDTO {
    private List<Filter> filter;
    private List<Book> books;
}
