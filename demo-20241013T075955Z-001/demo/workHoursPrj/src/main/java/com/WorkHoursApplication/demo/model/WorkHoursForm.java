package com.WorkHoursApplication.demo.model;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WorkHoursForm {
    private Long id;
    @NotNull(message = "L'orario di ingresso è obbligatorio")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime entryTime;
    @NotNull(message = "L'orario di uscita è obbligatorio")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime exitTime;

    public WorkHoursForm(LocalTime entryTime, LocalTime exitTime) {
        this.entryTime = entryTime;
        this.exitTime = exitTime;
    }
}
