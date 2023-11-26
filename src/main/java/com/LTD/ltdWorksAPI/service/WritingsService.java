package com.LTD.ltdWorksAPI.service;

import com.LTD.ltdWorksAPI.model.dto.FilterDTO;
import com.LTD.ltdWorksAPI.model.dto.SearchDTO;
import com.LTD.ltdWorksAPI.model.entity.Book;
import com.LTD.ltdWorksAPI.model.entity.Filter;
import com.LTD.ltdWorksAPI.utils.enums.Statuses;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.tomcat.util.buf.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class WritingsService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(WritingsService.class);
    private static final String searchSql = "(:searchText % ANY(STRING_TO_ARRAY(title, ' ')) " +
            "OR SIMILARITY(title, :searchText) > 0.3 " +
            "OR :searchText % ANY(book.author) " +
            "OR :searchText % ANY(STRING_TO_ARRAY(ARRAY_TO_STRING(book.author, ' '), ' ')) " +
            "OR SIMILARITY(description, :searchText) > 0.25) ";

    public List<Filter> countFilter(@NotNull String searchText, String category, String filterSql) {
        List<Filter> filter = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String queryCte = "filtered_books";

//        CTE
        sql.append("WITH ").append(queryCte).append(" AS (SELECT * FROM writings.").append(category);
        if (searchText.length() > 0) {
//        WHERE
            sql.append(" WHERE " + searchSql).append(filterSql);

            parameters.addValue("searchText", searchText);
        } else if (!filterSql.isEmpty()) {
            sql.append(" WHERE ").append(filterSql);
        }
        sql.append(" )");

//        Author
        sql.append("SELECT 'author' as type, author.author as name, count(author.author) quantity FROM ").append(queryCte);
        sql.append(" CROSS JOIN UNNEST (").append(queryCte).append(".author) as author ");
//        if (searchText.length() > 0) {
//            sql.append(" WHERE " + searchSql);
//            parameters.addValue("searchText", searchText);
//        }
        sql.append(" GROUP BY author.author UNION ALL ");

//        Genre
        sql.append("SELECT 'genre' as type, genre.genre as name, count(genre.genre) quantity FROM ").append(queryCte);
        sql.append(" CROSS JOIN UNNEST (").append(queryCte).append(".genre) as genre ");
//        if (searchText.length() > 0) {
//            sql.append(" WHERE " + searchSql);
//        }
        sql.append(" GROUP BY genre.genre UNION ALL ");

//        Tag
        sql.append("SELECT 'tag' as type, tag.tag as name, count(tag.tag) quantity FROM ").append(queryCte);
        sql.append(" CROSS JOIN UNNEST (").append(queryCte).append(".tag) as tag ");
//        if (searchText.length() > 0) {
//            sql.append(" WHERE " + searchSql);
//        }
        sql.append(" GROUP BY tag.tag ");

//        ORDER
        sql.append("ORDER BY type, quantity DESC");

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(jdbcTemplate.getDataSource()));

        List<Map<String, Object>> filterDAO = namedParameterJdbcTemplate.queryForList(String.valueOf(sql), parameters);
        List<Filter.FilterItem> filterByType = new ArrayList<>();
        String typeTemp = "";
        log.info("Successfully fetched filter count from DB");

        for (Map<String, Object> filterItem : filterDAO) {
            if (!Objects.equals(typeTemp, filterItem.get("type").toString()) && typeTemp.length() > 0) {
                List<Filter.FilterItem> filterByTypeTemp = new ArrayList<>(filterByType);

                filter.add(new Filter(typeTemp, filterByTypeTemp));
                filterByType.clear();
            }

            filterByType.add(new Filter.FilterItem((String) filterItem.get("name"), Math.toIntExact((Long) filterItem.get("quantity"))));

            typeTemp = filterItem.get("type").toString();
        }
        filter.add(new Filter(typeTemp, filterByType));

        log.info("Created filter list and returned");
        return filter;
    }

    public SearchDTO findWritings(String searchText, String category, @NotNull FilterDTO chosenFilter, Integer page, Boolean pageChange) {
        StringBuilder filterSql = new StringBuilder();
        StringBuilder andFilter = new StringBuilder();
        StringBuilder orFilter = new StringBuilder();
        StringBuilder notFilter = new StringBuilder();
        AtomicInteger index = new AtomicInteger(0);
        chosenFilter.getFilterCategories().forEach((filterCategory) -> {
            List<String> andItems = new ArrayList<>();
            List<String> orItems = new ArrayList<>();
            List<String> notItems = new ArrayList<>();

            filterCategory.getFilterItems().forEach(filterItem -> {
                if (Objects.equals(filterItem.getStatus(), Statuses.AND.toString())) {
                    andItems.add("'" + filterItem.getName() + "'");
                } else if (Objects.equals(filterItem.getStatus(), Statuses.OR.toString())) {
                    orItems.add("'" + filterItem.getName() + "'");
                } else {
                    notItems.add("'" + filterItem.getName() + "'");
                }
            });

            if (!andItems.isEmpty()) {
                if (index.get() != 0 && chosenFilter.getFilterCategories().subList(0, index.get()).stream()
                        .flatMap(categoryCheck -> categoryCheck.getFilterItems().stream())
                        .anyMatch(item -> Objects.equals(item.getStatus(), Statuses.AND.toString()))) {
                    andFilter.append("AND ");
                }

                andFilter.append("(ARRAY[").append(StringUtils.join(andItems, ','))
                        .append("] <@ book.").append(filterCategory.getType()).append(")");
            }
            if (!orItems.isEmpty()) {
                if (index.get() != 0 && chosenFilter.getFilterCategories().subList(0, index.get()).stream()
                        .flatMap(categoryCheck -> categoryCheck.getFilterItems().stream())
                        .anyMatch(item -> Objects.equals(item.getStatus(), Statuses.OR.toString()))) {
                    orFilter.append("OR ");
                }

                orFilter.append("(ARRAY[").append(StringUtils.join(orItems, ','))
                        .append("] && book.").append(filterCategory.getType()).append(")");
            }
            if (!notItems.isEmpty()) {
                if (index.get() != 0 && chosenFilter.getFilterCategories().subList(0, index.get()).stream()
                        .flatMap(categoryCheck -> categoryCheck.getFilterItems().stream())
                        .anyMatch(item -> Objects.equals(item.getStatus(), Statuses.NOT.toString()))) {
                    notFilter.append("AND ");
                }

                notFilter.append("(NOT ARRAY[").append(StringUtils.join(notItems, ','))
                        .append("] && book.").append(filterCategory.getType()).append(")");
            }

            index.getAndIncrement();
        });

        if (!andFilter.isEmpty()) {
            filterSql.append("(").append(andFilter).append(")").append(" AND ");
        }
        if (!orFilter.isEmpty()) {
            filterSql.append("(").append(orFilter).append(")").append(" AND ");
        }
        if (!notFilter.isEmpty()) {
            filterSql.append("(").append(notFilter).append(")").append(" AND ");
        }

        if (!filterSql.isEmpty()) {
            filterSql.delete(filterSql.length() - " AND ".length(), filterSql.length());
        }

        List<Filter> filter = null;
        if (!pageChange) {
            filter = countFilter(searchText, category, filterSql.toString());
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        sql.append("writings.").append(category);

        if (searchText.length() > 0) {
//        WHERE
            sql.append(" WHERE " + searchSql).append(filterSql);
//        ORDER
            sql.append("ORDER BY " + searchSql);

            parameters.addValue("searchText", searchText);
        } else if (!filterSql.isEmpty()) {
            sql.append(" WHERE ").append(filterSql);
        }

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(jdbcTemplate.getDataSource()));

        List<Map<String, Object>> books = namedParameterJdbcTemplate.queryForList(String.valueOf(sql), parameters);
        List<Book> bookList = new ArrayList<>();
        log.info("Successfully fetched searched books from DB");

        books.forEach(b -> {
        List<String> author = Arrays.stream(b.get("author").toString().substring(1, b.get("author").toString().length() - 1).split(",")).toList();
        List<String> genre = Arrays.stream(b.get("genre").toString().substring(1, b.get("genre").toString().length() - 1).split(",")).toList();
        List<String> tag = Arrays.stream(b.get("tag").toString().substring(1, b.get("tag").toString().length() - 1).split(",")).toList();
        List<Integer> chapterId = null;
        if (b.get("chapterId") != null) {
            chapterId = Arrays.stream(b.get("chapterId").toString().substring(1, b.get("chapterId").toString().length() - 1).split(",")).map(Integer::parseInt).collect(Collectors.toList());
        }

            Book book = new Book(
                    (Integer) b.get("id"),
                    (String) b.get("title"),
                    author,
                    (String) b.get("year"),
                    (String) b.get("description"),
                    (Integer) b.get("pages"),
                    (String) b.get("cover"),
                    genre,
                    tag,
                    (Integer) b.get("prequelId"),
                    (Integer) b.get("sequelId"),
                    chapterId,
                    (String) b.get("series"));
            bookList.add(book);
        });
        log.info("Created book list");

        //TODO Pagination
        return new SearchDTO(filter, bookList);
    }
}
