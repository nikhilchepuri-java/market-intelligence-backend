package com.marketintel.repository;

import com.marketintel.model.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, Long> {
    List<AlertRuleEntity> findBySymbolIgnoreCaseOrderByIdAsc(String symbol);
}
