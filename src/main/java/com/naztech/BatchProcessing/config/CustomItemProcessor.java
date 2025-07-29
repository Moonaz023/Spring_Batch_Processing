package com.naztech.BatchProcessing.config;

import com.naztech.BatchProcessing.entity.ProductInfo;
import org.springframework.batch.item.ItemProcessor;

public class CustomItemProcessor implements ItemProcessor<ProductInfo,ProductInfo> {
    @Override
    public ProductInfo process(ProductInfo item) throws Exception {
        return item;
    }
}
