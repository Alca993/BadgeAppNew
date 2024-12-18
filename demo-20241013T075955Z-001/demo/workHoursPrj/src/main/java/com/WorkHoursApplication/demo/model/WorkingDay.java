package com.WorkHoursApplication.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "workingDay")
public class WorkingDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private LocalDate date;
    private LocalTime entryTime;
    private LocalTime exitTime;
    private Duration bonusOrDebito;
    private String bonusDebFormatted;

    public WorkingDay(LocalDate date) {
        this.date = date;
    }

    public Duration getWorkedHours(){
        if(entryTime != null && exitTime!=null){
            return Duration.between(entryTime, exitTime);
        }
        return Duration.ZERO;
    }
}
