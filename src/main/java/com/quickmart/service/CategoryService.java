package com.quickmart.service;

import com.quickmart.dto.CategoryRequest;
import com.quickmart.dto.CategoryResponse;
import com.quickmart.dto.PageResponse;
import com.quickmart.entity.Category;
import com.quickmart.exception.DuplicateResourceException;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.CategoryRepository;
import com.quickmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // Lombok: constructor injection for the final fields (testable, no @Autowired)
@Transactional(readOnly = true) // default for reads: skips dirty checking, lets the driver optimize
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public PageResponse<CategoryResponse> list(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable), CategoryResponse::from);
    }

    public CategoryResponse get(Long id) {
        return CategoryResponse.from(find(id));
    }

    @Transactional // writes override the read-only default
    public CategoryResponse create(CategoryRequest req) {
        // Friendly message for the common case. The unique index still catches the race
        // where two requests pass this check at the same time (handled as 409 in the handler).
        if (categoryRepository.existsByNameIgnoreCase(req.name())) {
            throw new DuplicateResourceException("Category '" + req.name() + "' already exists");
        }
        return CategoryResponse.from(categoryRepository.save(new Category(req.name())));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = find(id);
        if (!category.getName().equalsIgnoreCase(req.name())
                && categoryRepository.existsByNameIgnoreCase(req.name())) {
            throw new DuplicateResourceException("Category '" + req.name() + "' already exists");
        }
        // No save() call needed. The entity is "managed" inside this transaction, so Hibernate
        // detects the change (dirty checking) and issues the UPDATE on commit.
        category.setName(req.name());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new DuplicateResourceException("Category " + id + " still has products");
        }
        categoryRepository.delete(category);
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
    }
}
