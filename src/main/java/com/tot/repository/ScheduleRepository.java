package com.tot.repository;


import com.tot.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {
    /**
     * Find schedules by status
     */
    List<Schedule> findByStatus(String status);

    /**
     * Find schedules due before a specific time with given status
     */
    List<Schedule> findByScheduledTimeBeforeAndStatus(LocalDateTime dateTime, String status);

    /**
     * Find schedules due within a specific time window with given status
     */
    List<Schedule> findByScheduledTimeBetweenAndStatus(
            LocalDateTime startDateTime, LocalDateTime endDateTime, String status);

    /**
     * Find schedules for a specific target node
     */
    List<Schedule> findByTargetNodeId(String targetNodeId);

    /**
     * Find upcoming schedules within a time window
     */
    List<Schedule> findByScheduledTimeBetweenOrderByScheduledTimeAsc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Count schedules by status
     */
    long countByStatus(String status);
}
