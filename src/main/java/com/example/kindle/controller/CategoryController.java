package com.example.kindle.controller;

import com.example.kindle.entity.Category;
import com.example.kindle.repository.CategoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // 创建分类
    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        return categoryRepository.save(category);
    }

    // 查询所有分类
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}
