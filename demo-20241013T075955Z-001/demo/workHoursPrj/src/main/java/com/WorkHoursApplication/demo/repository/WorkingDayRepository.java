package com.WorkHoursApplication.demo.repository;

import com.WorkHoursApplication.demo.model.WorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WorkingDayRepository extends JpaRepository<WorkingDay, Long> {
    void deleteAllByDateBefore(LocalDate date);
    Optional<WorkingDay> findByDate(LocalDate date);
}
