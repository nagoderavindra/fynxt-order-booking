package com.fynxt.repository;

import com.fynxt.entity.Trader;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TraderRepository extends JpaRepository<Trader, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trader t WHERE t.id = :traderId")
    Optional<Trader> findByIdForUpdate(String traderId);
}