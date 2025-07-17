package com.example.kindle.service;


import com.example.kindle.entity.Book;
import com.example.kindle.entity.Category;
import com.example.kindle.repository.BookRepository;
import com.example.kindle.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
    public ResponseEntity<Resource> downloadImage(String filename,String uploadDir) throws IOException {
        try{
            File saveDir = new File(uploadDir);
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if(!resource.exists()){
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            if(filename.endsWith(".png")) contentType = "image/png";
            else if(filename.endsWith(".jpg")||filename.endsWith(".jpeg")) contentType = "image/jpeg";
            else if(filename.endsWith(".gif")) contentType = "image/gif";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (MalformedURLException e){
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }


    @Transactional
    public boolean deleteBook(Long id,String uploadDir) {
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

    @Transactional
    public ResponseEntity<String> updateBook(Long id, String title, String author, MultipartFile coverFile, List<Long> categoryId, String uploadDir) throws IOException {
        Optional<Book> opt = bookRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Book book = opt.get();

        //更新字段
        book.setTitle(title);
        book.setAuthor(author);
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                //生成新文件
                String filename = System.currentTimeMillis() + "_" + coverFile.getOriginalFilename();
                File saveDir = new File(uploadDir);
                if (!saveDir.exists()) saveDir.mkdirs();
                File newPath = new File(saveDir, filename);
                coverFile.transferTo(newPath);
                //删除旧封面
                File oldFile = new File(book.getCoverPath());
                if (oldFile.exists()) oldFile.delete();
                book.setCoverPath(newPath.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("封面保存失败" + e.getMessage());
            }
        }
        if (categoryId != null && !categoryId.isEmpty()) {
            Set<Category> categoryIdSet = new HashSet<>(categoryRepository.findAllById(categoryId));
            book.setCategories(categoryIdSet);
        }

        bookRepository.save(book);
        return ResponseEntity.ok("保存成功");
    }

    @Transactional
    public List<Book> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.searchByKeyword(keyword,pageable);
    }

    @Transactional
    public ResponseEntity<List<Book>> searchBooksByCategory(Long categoryId, Pageable pageable) {
        Optional<Category> opt = categoryRepository.findById(categoryId);
        if(opt.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        List<Book> books = bookRepository.findByCategories_id(categoryId,pageable);
        return ResponseEntity.ok(books);
    }

}
