package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseApplicationRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.ProductRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Product;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ProductCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ProductUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.ProductResponse;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final GroupPurchaseApplicationRepository applicationRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        // 공동구매 신청 ID가 들어온 경우 유효성 검증
        if (request.applicationId() != null) {
            applicationRepository.findById(request.applicationId())
                    .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        }

        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .categoryId(request.categoryId())
                .applicationId(request.applicationId())
                .reportId(request.reportId())
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    public List<ProductResponse> getAllProducts(Long categoryId) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else {
            products = productRepository.findAll();
        }
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 공동구매 신청 ID가 들어온 경우 유효성 검증
        if (request.applicationId() != null) {
            applicationRepository.findById(request.applicationId())
                    .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        }

        product.update(
                request.name(),
                request.price(),
                request.description(),
                request.imageUrl(),
                request.categoryId(),
                request.applicationId(),
                request.reportId()
        );

        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        productRepository.delete(product);
    }
}
