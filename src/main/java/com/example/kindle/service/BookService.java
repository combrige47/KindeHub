package com.example.kindle.service;


import com.example.kindle.entity.Book;
import com.example.kindle.entity.Category;
import com.example.kindle.repository.BookRepository;
import com.example.kindle.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    //构造
    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Book uploadBook(String title, String author, MultipartFile coverFile, List<Long> categoryIds, String uploadDir) throws IOException {
        String filename = System.currentTimeMillis() + "_"  + coverFile.getOriginalFilename();
        Path saveDir = Paths.get(uploadDir);
        if (!Files.exists(saveDir)) {
            Files.createDirectories(saveDir);
        }
        Path savePath = saveDir.resolve(filename);
        coverFile.transferTo(savePath);

        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if(categories.isEmpty() && !categoryIds.isEmpty()) {
            throw new IllegalArgumentException("分类不存在");
        }
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setCoverPath(savePath.toString());
        book.getCategories().addAll(categories);
        return bookRepository.save(book);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    public Page<Book> getBookByPage(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Transactional
    public boolean deleteBookById(Long id,String uploadDir) {
        Optional<Book> Optbook = bookRepository.findById(id);
        if(Optbook.isEmpty()) return false;

        Book book = Optbook.get();
        if(book.getCoverPath() != null) {
            File coverFile = new File(book.getCoverPath());
            if(coverFile.exists()) {
                if(!coverFile.delete()) {
                    System.err.println("未能删除封面: " + book.getCoverPath());
                }
            }
        }
        bookRepository.delete(book);
        return true;
    }


}
