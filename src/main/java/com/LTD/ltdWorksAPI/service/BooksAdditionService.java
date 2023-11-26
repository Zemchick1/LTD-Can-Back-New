package com.LTD.ltdWorksAPI.service;

import com.LTD.ltdWorksAPI.model.dto.BooksAdditionSearchDTO;
import com.LTD.ltdWorksAPI.model.entity.Book;
import com.LTD.ltdWorksAPI.repository.BookRepository;
import com.LTD.ltdWorksAPI.utils.enums.Roles;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@AllArgsConstructor
public class BooksAdditionService {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(WritingsService.class);
    private static final String searchSql = "(:searchText % ANY(STRING_TO_ARRAY(name, ' ')) OR SIMILARITY(name, :searchText) > 0.25)";
    private final BookRepository bookRepository;
    private final HelperFunctionsService helperFunctionsService;

    public void addBook(Book book, HttpServletRequest request){
        if (helperFunctionsService.getRole() == Roles.Admin || helperFunctionsService.getRole() == Roles.Moderator){
            bookRepository.save(book); // No Validation
            log.info("Added a book");
        }
        else {
            requestToAddBook(book);
        }
    }

    public void requestToAddBook(Book book){
        log.info("Requested a book");
    }

    public List<String> search(BooksAdditionSearchDTO booksAdditionSearchDTO) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT ON (name) name \n");
        // FROM
        sql.append("FROM writings.").append(booksAdditionSearchDTO.getCategory().toLowerCase()).append(" \n");

        // WHERE

        sql.append("WHERE ").append(searchSql).append("\n");

        // ORDER BY

        sql.append("ORDER BY name,").append(searchSql).append(" DESC \n");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("searchText", booksAdditionSearchDTO.getSearchText());
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(jdbcTemplate.getDataSource()));

        List<Map<String, Object>> queryRes = namedParameterJdbcTemplate.queryForList(sql.toString(), parameters);
        log.info("Successfully fetched searched object from DB");
        List<String> res = new ArrayList<>();

        for (Map<String, Object> map : queryRes) {
            for (Object value : map.values()) {
                res.add(value.toString());
            }
        }

        return res;
    }
}
