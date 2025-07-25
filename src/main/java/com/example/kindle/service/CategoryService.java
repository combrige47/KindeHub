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

    /**
     * 按照页搜索
     * @param page 第几页
     * @param size 每页的大小
     * @return 返回按照页搜索后的结果
     */
    public Page<Category> getCategory(int page, int size) {
        return categoryRepository.findAll(PageRequest.of(page, size));
    }

    /**
     * 创建分类
     * @param name 分类名
     * @return 返回创建后信息
     */
    @Transactional
    public String createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);
        return "分类创建成功，Id:" + category.getId();
    }

    /**
     * 获取所有分类
     * @return 返回所有分类
     */
    @Transactional
    public List<Category> findAll() {return categoryRepository.findAll();}

    /**
     * 删除分类
     * @param id 对应分类id
     * @return 返回删除后信息
     */
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
