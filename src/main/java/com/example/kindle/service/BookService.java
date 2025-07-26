package com.example.kindle.service;


import com.example.kindle.entity.Book;
import com.example.kindle.entity.Category;
import com.example.kindle.repository.BookRepository;
import com.example.kindle.repository.CategoryRepository;
import com.example.kindle.service.book.EbookProcessor;
import com.example.kindle.service.book.EbookProcessorFactory;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
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
    private final EbookProcessorFactory ebookProcessorFactory;
    @Value("${file.upload-dir}")
    private String uploadDir;
    @Value("${file.ebook-dir}")
    private String ebookDir; // 电子书文件和封面图片将保存在这里
    @Value("${file.cover-dir}")
    private String coverDir;
    //构造
    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository, EbookProcessorFactory ebookProcessorFactory) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.ebookProcessorFactory = ebookProcessorFactory;
    }

    /**
     * 上传电子书
     * @param ebookFile 上传电子书文件
     * @param categoryIds 分类
     * @return 返回上传后对应的Book类
     */
    @Transactional
    public Book uploadEbookFile(MultipartFile ebookFile,List<Long> categoryIds) throws IOException {
        String extension = getExtension(ebookFile);

        Optional<EbookProcessor> processorOpt = ebookProcessorFactory.getProcessor(extension);
        if(processorOpt.isEmpty()) {
            throw new IllegalArgumentException("不支持该电子书格式:"+extension);
        }
        String uniqueFilename = UUID.randomUUID().toString()+"."+extension;
        EbookProcessor processor = processorOpt.get();

        Path saveDir = Paths.get(uploadDir);
        Map<String, String> metadata = processor.process(ebookFile.getInputStream(), uniqueFilename, saveDir);
        String title = metadata.get("title");
        String author = metadata.get("author");
        String filePath = metadata.get("filePath");
        String coverPath = metadata.get("coverPath");
        String originalFilename = ebookFile.getOriginalFilename();

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setFilePath(filePath);
        book.setCoverPath(coverPath);
        book.setOriginalFilename(originalFilename);

        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if(categories.isEmpty() &&  !categoryIds.isEmpty()) {
            throw new IllegalArgumentException("分类不存在");
        }

        book.getCategories().addAll(categories);
        return bookRepository.save(book);
    }

    /**
     * 获取扩展名
     * @param ebookFile 电子书文件
     * @return 返回电子书扩展名
     */
    private static String getExtension(MultipartFile ebookFile) {
        if(ebookFile.isEmpty()) {
            throw new IllegalArgumentException("上传电子书为空");
        }
        String filename = ebookFile.getOriginalFilename();
        if(filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名为空");
        }
        //获取文件扩展名
        String extension = "";
        int dotIndex = filename.lastIndexOf(".");
        if(dotIndex > 0 && dotIndex < filename.length() - 1) {
            extension = filename.substring(dotIndex + 1);
        } else {
            throw new IllegalArgumentException("无法识别扩展名");
        }
        return extension;
    }

    /**
     * 不上传文件，仅上传电子书信息
     * @param title 标题
     * @param author 作者
     * @param coverFile 封面
     * @param categoryIds 分类
     * @param uploadDir 下载文件夹
     * @return 返回上传后的Book类
     */
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

    /**
     * 获取所有Book类
     * @return 返回所有电子书
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /**
     * 通过id查找电子书
     * @param id 电子书对应id
     * @return 返回id对应的电子书
     */
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    /**
     * 按页获取所有电子书
     * @param pageable 对应页
     * @return 返回按照页的电子书
     */
    public Page<Book> getBookByPage(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    /**
     * 获取封面图片
     * @param filename 对应封面文件名称
     * @param uploadDir 封面保存文件夹
     * @return 返回对应封面图片
     */
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
            e.fillInStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除对应电子书
     * @param id 对应电子书id
     * @return 删除成功返回1 失败返回0
     */
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

    /**
     * 更新电子书信息
     * @param id 对应电子书id
     * @param title 修改后的名称
     * @param author 修改后的作家
     * @param categoryId 修改后的分类
     * @return 返回修改后的信息
     * @throws IOException
     */
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
                e.fillInStackTrace();
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

    /**
     * 搜索电子书
     * @param keyword 搜索电子书的关键词
     * @return 返回按照关键词搜索后的电子书
     */
    @Transactional
    public List<Book> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.searchByKeyword(keyword,pageable);
    }

    /**\
     * 通过分类查找电子书
     * @param categoryId 对应分类
     * @return 返回对应分类的电子书
     */
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
