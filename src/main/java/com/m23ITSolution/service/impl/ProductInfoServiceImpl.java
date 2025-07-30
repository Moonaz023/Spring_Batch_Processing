package com.m23ITSolution.service.impl;

import com.m23ITSolution.entity.ProductInfo;
import com.m23ITSolution.repository.ProductInfoRepository;
import com.m23ITSolution.service.ProductInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductInfoServiceImpl implements ProductInfoService {
    private  final ProductInfoRepository productInfoRepository;

    @Override
    public ProductInfo saveProduct(ProductInfo newProduct) {
        return productInfoRepository.save(newProduct);
    }
}
