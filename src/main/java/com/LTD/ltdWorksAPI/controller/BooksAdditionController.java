package com.LTD.ltdWorksAPI.controller;

import com.LTD.ltdWorksAPI.model.dto.BooksAdditionSearchDTO;
import com.LTD.ltdWorksAPI.model.entity.Book;
import com.LTD.ltdWorksAPI.service.BooksAdditionService;
import com.LTD.ltdWorksAPI.utils.enums.BooksAdditionSearchCategory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/book/addition")
@RequiredArgsConstructor
public class BooksAdditionController {
    private final BooksAdditionService booksAdditionService;
    @PostMapping("/add")
    public ResponseEntity<String> addBook(@RequestBody Book book, HttpServletRequest request) {
        booksAdditionService.addBook(book, request);
        return ResponseEntity.ok("Successfully added Book");
    }

    @PostMapping("/search")
    public ResponseEntity<List<String>> search(@RequestBody @NonNull BooksAdditionSearchDTO booksAdditionSearchDTO) {
        BooksAdditionSearchCategory.valueOf(booksAdditionSearchDTO.getCategory().toUpperCase()); // Validation
        return ResponseEntity.ok(booksAdditionService.search(booksAdditionSearchDTO));
    }
}
