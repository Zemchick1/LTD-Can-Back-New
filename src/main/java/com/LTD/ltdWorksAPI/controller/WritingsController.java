package com.LTD.ltdWorksAPI.controller;

import com.LTD.ltdWorksAPI.model.dto.FilterDTO;
import com.LTD.ltdWorksAPI.model.dto.SearchDTO;
import com.LTD.ltdWorksAPI.service.WritingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/search")
@RequiredArgsConstructor
public class WritingsController {
    private final WritingsService filterService;
    private static final Logger log = LoggerFactory.getLogger(WritingsController.class);

    @PostMapping
    public Object findWritings(@RequestParam(value = "searchText") String searchText, @RequestParam(value = "category") String category, @RequestParam(value = "page") Integer page, @RequestParam(value = "pageChange") Boolean pageChange, @RequestBody FilterDTO chosenFilter) {
        SearchDTO searchResult = filterService.findWritings(searchText, category, chosenFilter, page, pageChange);
        log.info("Got book search result from DB");

        if (searchResult.getFilter() == null) {
            return searchResult.getBooks();
        }
        return searchResult;
    }
}
