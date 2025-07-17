package com.example.kindle.controller;

import com.example.kindle.entity.Book;
import com.example.kindle.entity.Category;
import com.example.kindle.repository.BookRepository;
import com.example.kindle.repository.CategoryRepository;
import com.example.kindle.service.BookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/book")
public class BookController {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookService bookService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public BookController(BookRepository bookRepository , CategoryRepository categoryRepository, BookService bookService) {
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam("categoryId") List<Long> categoryIds
    ){
        if(coverFile.isEmpty()){
            return ResponseEntity.badRequest().body("上传文件为空");
        }
        try{
        Book book = bookService.uploadBook(title,author,coverFile,categoryIds,uploadDir);
        return ResponseEntity.ok("上传成功，id为" + book.getId());
    }
        catch (IOException e){
        e.printStackTrace();
        return ResponseEntity.status(500).body("上传失败:"+e.getMessage());
        }catch (IllegalArgumentException e) { // 捕获服务层抛出的业务异常
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public List<Book> getAllBooks(){
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable("id") long id){
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> downloadImage(@PathVariable("filename") String filename) throws IOException {
        return bookService.downloadImage(filename,uploadDir);
    }

    @GetMapping("/page")
    public Page<Book> GetBooksByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return bookService.getBookByPage(pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable long id){
        if(bookService.deleteBook(id, uploadDir)){ // 传入 uploadDir
            return ResponseEntity.ok("删除成功");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateBook(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "cover", required = false) MultipartFile coverFile,
            @RequestParam(required = false) List<Long> categoryId
            ) throws IOException {
        return bookService.updateBook(id,title,author,coverFile,categoryId,uploadDir);
    }

    @GetMapping("/search")
    public List<Book> searchBooks(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return bookService.searchBooks(keyword,pageable);
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<Book>> searchBooksByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return bookService.searchBooksByCategory(categoryId,pageable);
    }
}



