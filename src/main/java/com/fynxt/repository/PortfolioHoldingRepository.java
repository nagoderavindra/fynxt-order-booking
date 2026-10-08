package com.fynxt.repository;

import com.fynxt.entity.PortfolioHolding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioHoldingRepository
        extends JpaRepository<PortfolioHolding, Long> {

    Optional<PortfolioHolding> findByTraderIdAndStock(
            String traderId,
            String stock
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PortfolioHolding p
            WHERE p.traderId = :traderId
              AND p.stock = :stock
            """)
    Optional<PortfolioHolding> findByTraderIdAndStockForUpdate(
            @Param("traderId") String traderId,
            @Param("stock") String stock
    );

    List<PortfolioHolding> findAllByTraderId(String traderId);
}