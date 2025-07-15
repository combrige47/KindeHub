package com.example.kindle.controller;

import com.example.kindle.entity.Book;
import com.example.kindle.entity.Category;
import com.example.kindle.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.beans.Transient;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Autowired
    private CategoryRepository CategoryRepository;
    @GetMapping
    public Page<Category> getCategory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return categoryRepository.findAll(PageRequest.of(page, size));
    }


    // 创建分类
    @PostMapping("/create")
    public String createCategory(@RequestParam("name") String name) {
        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);
        return "分类创建成功，Id:" + category.getId();
    }

    // 查询所有分类
    @GetMapping("/all")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    //删除分类
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Category category = categoryOpt.get();
        //解除与图书绑定
        for(Book book : category.getBooks()) {
            book.getCategories().remove(category);
        }
        category.getBooks().clear();
        categoryRepository.delete(category);

        return ResponseEntity.ok("分类删除成功");
    }
}
