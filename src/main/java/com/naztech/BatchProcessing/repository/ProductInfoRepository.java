package com.naztech.BatchProcessing.repository;

import com.naztech.BatchProcessing.entity.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInfoRepository extends JpaRepository<ProductInfo ,Long> {
}
