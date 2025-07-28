package com.example.kindle.controller;

import com.example.kindle.entity.Book;
import com.example.kindle.repository.BookRepository;
import com.example.kindle.repository.CategoryRepository;
import com.example.kindle.service.BookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;
import jakarta.mail.MessagingException;
import java.io.FileNotFoundException;

@RestController
@RequestMapping("/book")
public class BookController {
    private final BookService bookService;

    @Value("${file.upload-dir}")
    private String uploadDir;   //文件下载路径

    public BookController(BookRepository bookRepository , CategoryRepository categoryRepository, BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 上传ebook
     * @param ebookFile 电子书文件
     * @param categoryIds 书籍所属分类
     * @return 返回对应Book类
     */
    @PostMapping("/up")
    public Book upBook(
            @RequestParam("ebook") MultipartFile ebookFile,
            @RequestParam("categoryId") List<Long> categoryIds
    ) throws IOException {
        return bookService.uploadEbookFile(ebookFile, categoryIds);
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
        e.fillInStackTrace();
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

    /**
     * 将电子书发送到Kindle邮箱
     * @param bookId 书籍ID
     * @param kindleEmail Kindle邮箱地址
     * @return 发送结果
     */
    @PostMapping("/send-to-kindle/{bookId}")
    public ResponseEntity<String> sendToKindle(
            @PathVariable Long bookId,
            @RequestParam String kindleEmail
    ) {
        try {
            bookService.sendToKindle(bookId, kindleEmail);
            return ResponseEntity.ok("电子书已成功发送到Kindle邮箱: " + kindleEmail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (MessagingException e) {
            return ResponseEntity.status(500).body("发送邮件失败: " + e.getMessage());
        }
    }
}



