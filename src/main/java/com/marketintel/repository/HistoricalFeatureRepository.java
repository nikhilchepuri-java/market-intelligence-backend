package com.marketintel.repository;
import com.marketintel.model.HistoricalFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface HistoricalFeatureRepository extends JpaRepository<HistoricalFeature,Long> {
 List<HistoricalFeature> findBySymbolAndTimestampBetweenOrderByTimestampAsc(String symbol,Instant from,Instant to);
 List<HistoricalFeature> findBySymbolOrderByTimestampAsc(String symbol);
}