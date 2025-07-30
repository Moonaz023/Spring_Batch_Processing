package com.m23ITSolution.BatchProcessing.repository;

import com.m23ITSolution.BatchProcessing.entity.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInfoRepository extends JpaRepository<ProductInfo,Long> {
}
