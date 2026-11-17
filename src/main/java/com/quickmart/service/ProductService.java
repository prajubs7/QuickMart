package com.quickmart.service;

import com.quickmart.dto.PageResponse;
import com.quickmart.dto.ProductRequest;
import com.quickmart.dto.ProductResponse;
import com.quickmart.entity.Category;
import com.quickmart.entity.Product;
import com.quickmart.exception.DuplicateResourceException;
import com.quickmart.exception.ResourceNotFoundException;
import com.quickmart.repository.CategoryRepository;
import com.quickmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Mapping to DTOs happens HERE, inside the transaction. open-in-view is off,
    // so touching a lazy field after this method returns would throw.
    public PageResponse<ProductResponse> list(Long categoryId, Pageable pageable) {
        Page<Product> page = (categoryId == null)
                ? productRepository.findAll(pageable)
                : productRepository.findByCategoryId(categoryId, pageable);
        return PageResponse.from(page, ProductResponse::from);
    }

    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsBySku(req.sku())) {
            throw new DuplicateResourceException("SKU '" + req.sku() + "' already exists");
        }
        Product product = new Product();
        apply(product, req);
        if (req.active() != null) product.setActive(req.active());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product product = find(id);
        if (!product.getSku().equals(req.sku()) && productRepository.existsBySku(req.sku())) {
            throw new DuplicateResourceException("SKU '" + req.sku() + "' already exists");
        }
        apply(product, req);
        if (req.active() != null) product.setActive(req.active());
        return ProductResponse.from(product); // dirty checking writes the UPDATE on commit
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(find(id));
    }

    private void apply(Product product, ProductRequest req) {
        // findById, not getReferenceById: a reference doesn't check existence,
        // so a bad categoryId would only fail later as an FK violation instead of a clear 404.
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category " + req.categoryId() + " not found"));
        product.setSku(req.sku());
        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setCategory(category);
    }

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }
}
