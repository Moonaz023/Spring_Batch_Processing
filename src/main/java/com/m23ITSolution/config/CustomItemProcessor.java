package com.m23ITSolution.config;

import com.m23ITSolution.entity.ProductInfo;
import org.springframework.batch.item.ItemProcessor;

public class CustomItemProcessor implements ItemProcessor<ProductInfo,ProductInfo> {
    @Override
    public ProductInfo process(ProductInfo item) throws Exception {
        return item;
    }
}
