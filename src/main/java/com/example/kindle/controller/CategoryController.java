package com.example.kindle.controller;

import com.example.kindle.entity.Book;
import com.example.kindle.entity.Category;
import com.example.kindle.repository.CategoryRepository;
import com.example.kindle.service.CategoryService;
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
    @Autowired
    private CategoryService categoryService;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public Page<Category> getCategory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return categoryService.getCategory(page,size);
    }


    // 创建分类
    @PostMapping("/create")
    public String createCategory(@RequestParam("name") String name) {
        return categoryService.createCategory(name);
    }

    // 查询所有分类
    @GetMapping("/all")
    public List<Category> getAllCategories() {
        return categoryService.findAll();
    }

    //删除分类
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        return categoryService.deteleCategory(id);
    }
}
