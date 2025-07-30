package com.m23ITSolution.BatchProcessing.service.impl;

import com.m23ITSolution.BatchProcessing.entity.ProductInfo;
import com.m23ITSolution.BatchProcessing.repository.ProductInfoRepository;
import com.m23ITSolution.BatchProcessing.service.ProductInfoService;
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
