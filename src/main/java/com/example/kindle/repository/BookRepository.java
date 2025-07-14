package com.example.kindle.repository;

import com.example.kindle.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface BookRepository extends JpaRepository<Book,Long> {
    @Query("SELECT b FROM Book b WHERE b.title LIKE %:kw% OR b.author LIKE %:kw%")
    List<Book> searchByKeyword(@Param("kw") String keyword, Pageable pageable);
}
