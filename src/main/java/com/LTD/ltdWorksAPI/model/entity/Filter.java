package com.LTD.ltdWorksAPI.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class Filter {
    private String type;
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FilterItem {
        private String name;
        private Integer quantity;
        private String status;

        public FilterItem(String name, Integer quantity) {
            this.name = name;
            this.quantity = quantity;
        }

        public FilterItem(String name, String status) {
            this.name = name;
            this.status = status;
        }
    }
    private List<FilterItem> filterItems;
}
