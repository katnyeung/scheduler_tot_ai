package com.tot.repository;


import com.tot.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionRepository extends JpaRepository<Action, String> {
    /**
     * Find actions by type
     */
    List<Action> findByActionType(String actionType);

    /**
     * Find actions created within a date range
     */
    List<Action> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find most recent actions by type, limited to a specific count
     */
    List<Action> findTop10ByActionTypeOrderByCreatedAtDesc(String actionType);

    /**
     * Find actions containing specific data (partial match)
     */
    @Query("SELECT a FROM Action a WHERE a.actionData LIKE %:keyword%")
    List<Action> findByActionDataContaining(@Param("keyword") String keyword);
}
