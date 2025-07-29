package com.naztech.BatchProcessing.repository;

import com.naztech.BatchProcessing.entity.BatchProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchProgressRepository extends JpaRepository<BatchProgress, Integer> {
}
