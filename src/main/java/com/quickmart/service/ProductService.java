package com.quickmart.service;

import com.quickmart.config.Actor;
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
import org.springframework.security.access.AccessDeniedException;
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

    // The owner comes from the token, never from the body: a VENDOR's product is theirs,
    // an ADMIN's product is a platform catalog product (vendorId null).
    @Transactional
    public ProductResponse create(Actor actor, ProductRequest req) {
        if (productRepository.existsBySku(req.sku())) {
            throw new DuplicateResourceException("SKU '" + req.sku() + "' already exists");
        }
        Product product = Product.ownedBy(actor.isAdmin() ? null : actor.vendorId());
        apply(product, req);
        if (req.active() != null) product.setActive(req.active());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Actor actor, Long id, ProductRequest req) {
        Product product = find(id);
        requireCanManage(actor, product); // before the SKU check: don't tell a non-owner anything about SKUs
        if (!product.getSku().equals(req.sku()) && productRepository.existsBySku(req.sku())) {
            throw new DuplicateResourceException("SKU '" + req.sku() + "' already exists");
        }
        apply(product, req);
        if (req.active() != null) product.setActive(req.active());
        return ProductResponse.from(product); // dirty checking writes the UPDATE on commit
    }

    @Transactional
    public void delete(Actor actor, Long id) {
        Product product = find(id);
        requireCanManage(actor, product);
        productRepository.delete(product);
    }

    // Object-level check. SecurityConfig only knows "this caller is a VENDOR"; it can't know WHICH
    // product belongs to which vendor. Without this, any vendor could reprice a competitor's products.
    //
    // 403, not 404: products are public (GET /api/products/{id}), so hiding existence gains nothing
    // and "not yours" is the honest answer. Orders/addresses use 404 because their ids ARE private.
    private static void requireCanManage(Actor actor, Product product) {
        if (actor.isAdmin() || actor.isVendor(product.getVendorId())) {
            return;
        }
        throw new AccessDeniedException("Product " + product.getId() + " belongs to another owner");
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
