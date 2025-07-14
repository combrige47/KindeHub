package com.example.kindle.controller;

import com.example.kindle.entity.Book;
import com.example.kindle.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/book")
public class BookController {
    private final BookRepository bookRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @PostMapping("/upload")
    public String uploadBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("cover") MultipartFile coverFile
    ){
        if(coverFile.isEmpty()){
            return "上传文件为空";
        }
        try{
        //文件名处理(加时间戳防重)
        String filename = System.currentTimeMillis() + "_" + coverFile.getOriginalFilename();

        //文件保存路径
            String projectDir = new File("").getAbsolutePath();
            File saveDir = new File(projectDir, uploadDir);
            if (!saveDir.exists()) {
                boolean created = saveDir.mkdirs();
                if (!created) {
                    return "上传失败: 无法创建目录 " + saveDir.getAbsolutePath();
                }
            }
            File savePath = new File(saveDir, filename);
            coverFile.transferTo(savePath); //保存到本地

        //保存信息到数据库
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setCoverPath(savePath.getPath());
        bookRepository.save(book);
        return "上传成功  图书id为" + book.getId();
    }
        catch (IOException e){
        e.printStackTrace();
        return "上传失败:" + e.getMessage();
        }
    }

    @GetMapping("/all")
    public List<Book> getAllBooks(){
        return bookRepository.findAll();
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable("id") long id){
        return bookRepository.findById(id).orElse(null);
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> downloadImage(@PathVariable("filename") String filename){
        try{
            String projectDir = new File("").getAbsolutePath(); // 获取项目根目录
            File saveDir = new File(projectDir, uploadDir);     // 拼接 uploads/ 路径
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

    @GetMapping("/page")
    public Page<Book> GetBooksByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return bookRepository.findAll(pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable long id){
        Optional<Book> optionalBook = bookRepository.findById(id);
        if(optionalBook.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        Book book = optionalBook.get();

        File coverFile = new File(book.getCoverPath());
        if(coverFile.exists()){
            if(!coverFile.delete()){
                return ResponseEntity.status(500).body("删除封面失败");
            }
        }

        bookRepository.delete(book);
        return ResponseEntity.ok("删除成功");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateBook(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "cover", required = false) MultipartFile coverFile
            ) {
        //查数据库
        Optional<Book> opt = bookRepository.findById(id);
        if(opt.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Book book = opt.get();

        //更新字段
        book.setTitle(title);
        book.setAuthor(author);
        if(coverFile != null && !coverFile.isEmpty()){
            try{
                //生成新文件
                String filename = System.currentTimeMillis() + "_" + coverFile.getOriginalFilename();
                String projectDir = new File("").getAbsolutePath();
                File saveDir = new File(projectDir, uploadDir);
                if (!saveDir.exists()) saveDir.mkdirs();
                File newPath = new File(saveDir, filename);
                coverFile.transferTo(newPath);
                //删除旧封面
                File oldFile = new File(book.getCoverPath());
                if(oldFile.exists()) oldFile.delete();
                book.setCoverPath(newPath.getPath());
            } catch(IOException e){
                e.printStackTrace();
                return ResponseEntity.status(500).body("封面保存失败"+e.getMessage());
            }
        }
        bookRepository.save(book);
        return ResponseEntity.ok("保存成功");
    }

    @GetMapping("/search")
    public List<Book> searchBooks(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return bookRepository.searchByKeyword(keyword,pageable);
    }
}



