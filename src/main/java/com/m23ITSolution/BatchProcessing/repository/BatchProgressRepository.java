package com.m23ITSolution.BatchProcessing.repository;

import com.m23ITSolution.BatchProcessing.entity.BatchProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchProgressRepository extends JpaRepository<BatchProgress, Integer> {
}
