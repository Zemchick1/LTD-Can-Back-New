package com.LTD.ltdWorksAPI.model.dto;

import com.LTD.ltdWorksAPI.model.entity.Filter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FilterDTO {
    private List<Filter> filterCategories;
    private Integer yearFrom;
    private Integer yearTo;
}
