package com.example.kindle.service;


import com.example.kindle.entity.Book;
import com.example.kindle.entity.Category;
import com.example.kindle.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private CategoryRepository categoryRepository;


    public Page<Category> getCategory(int page, int size) {
        return categoryRepository.findAll(PageRequest.of(page, size));
    }

    public String createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);
        return "分类创建成功，Id:" + category.getId();
    }

    public List<Category> findAll() {return categoryRepository.findAll();}

    @Transactional
    public ResponseEntity<String> deteleCategory(Long id) {
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
