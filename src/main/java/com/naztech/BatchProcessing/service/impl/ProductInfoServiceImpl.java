package com.naztech.BatchProcessing.service.impl;

import com.naztech.BatchProcessing.entity.ProductInfo;
import com.naztech.BatchProcessing.repository.ProductInfoRepository;
import com.naztech.BatchProcessing.service.ProductInfoService;
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
