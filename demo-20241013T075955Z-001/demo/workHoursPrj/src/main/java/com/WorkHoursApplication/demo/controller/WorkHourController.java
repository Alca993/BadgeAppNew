package com.WorkHoursApplication.demo.controller;

import com.WorkHoursApplication.demo.model.WorkHoursForm;
import com.WorkHoursApplication.demo.model.WorkingDay;
import com.WorkHoursApplication.demo.repository.WorkingDayRepository;
import com.WorkHoursApplication.demo.service.WorkHoursService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/work-hours")
public class WorkHourController {

    @Autowired
    private WorkHoursService workHoursService;


    @PostMapping("/entry")
    public ResponseEntity<String> registerEntry(@RequestParam String entryTime){
        workHoursService.registerEntry(LocalTime.parse(entryTime));
        return ResponseEntity.ok("Ingresso registrato.");
    }

    @PostMapping("/exit")
    public ResponseEntity<String> registerExit(@RequestParam String exitTime){
        workHoursService.registerExit(LocalTime.parse(exitTime));
        return ResponseEntity.ok("Uscita registrata.");
    }

    @GetMapping("/bonus-debito-cumulativo")
    public ResponseEntity<String> calculateBonusOrDebito() {
           Duration saldoCumulativo = workHoursService.calculateTotalBonusOrDebitoCumulat();
           System.out.println(saldoCumulativo);
           long hours = saldoCumulativo.toHours();
           long minutes = saldoCumulativo.toMinutesPart();
           if(saldoCumulativo==Duration.ZERO){
               if(workHoursService.isTheFirstWorkDayOfTheWeek()){
                   return ResponseEntity.ok("Sei in pari!" + "\n" + workHoursService.OrarioDiUscitaPrevisto());
               }else {
                   return ResponseEntity.ok("Sei in pari!");
               }
           } else if (saldoCumulativo.isPositive()) {
               return ResponseEntity.ok("Hai accumulato un bonus di : " + hours + " ore e " + minutes + " minuti." + "\n" +workHoursService.OrarioDiUscitaPrevisto());
           } else {
               return ResponseEntity.ok("Hai accumulato un debito: " + Math.abs(hours) + " ore e " + Math.abs(minutes) + " minuti."  + "\n" +workHoursService.OrarioDiUscitaPrevisto());
           }
       }
    @GetMapping("/weekStatus")
    public String getCurrentWorkHoursStatus(){
        List<WorkingDay> workingDays = workHoursService.getAllWorkingDays();

        String situation = workingDays.stream()
                .map(this::formatWorkingDay)
                .collect(Collectors.joining("\n"));

        Duration totalBonusOrDebt = workHoursService.calculateTotalBonusOrDebitoCumulat();
        if(totalBonusOrDebt==Duration.ZERO){
            situation += "\nSei in pari!";
        }else if(totalBonusOrDebt.isPositive()) {
            situation += "\n\nBonus cumulato: " + formatDuration(totalBonusOrDebt) + "\n" +workHoursService.OrarioDiUscitaPrevisto();
        }else{
            situation += "\n\nDebito cumulato: " + formatDuration(totalBonusOrDebt) + "\n" +workHoursService.OrarioDiUscitaPrevisto();
        }
        return situation;
    }

    @GetMapping("/pauseExit")
    public ResponseEntity<String> registerPauseExit(@RequestParam String exitTime){
        workHoursService.registerPauseExit(LocalTime.parse(exitTime));
        return ResponseEntity.ok("Uscita registrata.");
    }

    @GetMapping("/pauseEntry")
    public ResponseEntity<String> registerPauseEntry(@RequestParam String entryTime){
        workHoursService.registerPauseEntry(LocalTime.parse(entryTime));
        return ResponseEntity.ok("Ingresso registrato.");
    }

    private String formatWorkingDay(WorkingDay workingDay){
        String entryTime=null;
        String exitTime;
        String workedHours;
        String bonusOrDebt;
        try {
            entryTime = workingDay.getEntryTime().toString();
            exitTime = workingDay.getExitTime().toString();
            workedHours = formatDuration(workingDay.getWorkedHours());
            bonusOrDebt = workingDay.getBonusDebFormatted();
        }catch (NullPointerException n){
            return String.format("Data: %s | Ingresso: %s \n",
                    workingDay.getDate().toString(),entryTime);
        }
        return String.format("Data: %s | Ingresso: %s | Uscita: %s | Ore Lavorate: %s | Bonus/Debito: %s\n",
                workingDay.getDate().toString(),entryTime,exitTime,workedHours,bonusOrDebt);
    }

    private String formatDuration(Duration duration){
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if(hours<0 || minutes<0 || hours<0 && minutes<0){
            hours=Math.abs(hours);
            minutes=Math.abs(minutes);
        }
        if(hours==0 && minutes==1){
            return String.format("%02d ore e %02d minuto",hours,minutes);
        }else if(hours==0 && minutes>0){
            return String.format("%02d ore e %02d minuti",hours,minutes);
        }else {
            return String.format("%02d ore e %02d minuti", hours, minutes);
        }
    }
    @RequestMapping("/health")
    public class HealthController {
        @GetMapping
        public ResponseEntity<String> healthCheck() {
            return ResponseEntity.ok("OK");
        }
    }
}
