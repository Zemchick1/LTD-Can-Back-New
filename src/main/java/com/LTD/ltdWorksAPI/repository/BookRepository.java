package com.LTD.ltdWorksAPI.repository;

import com.LTD.ltdWorksAPI.model.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
}
