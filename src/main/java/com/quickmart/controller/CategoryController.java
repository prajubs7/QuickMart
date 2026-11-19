package com.quickmart.controller;

import com.quickmart.dto.CategoryRequest;
import com.quickmart.dto.CategoryResponse;
import com.quickmart.dto.PageResponse;
import com.quickmart.service.CatalogCacheService;
import com.quickmart.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Controllers stay thin: HTTP in, call the service, HTTP out. No business rules here.
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CatalogCacheService catalogCache;

    // ?page=0&size=20&sort=name,asc. Spring turns the query params into a Pageable.
    @GetMapping
    public PageResponse<CategoryResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return catalogCache.listCategories(pageable);
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        return categoryService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest req) { // @Valid triggers the DTO annotations
        return categoryService.create(req);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return categoryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
