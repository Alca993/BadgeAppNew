package com.WorkHoursApplication.demo.service;

import com.WorkHoursApplication.demo.model.WorkingDay;
import com.WorkHoursApplication.demo.repository.WorkingDayRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class WorkHoursService {

    @Autowired
    private WorkingDayRepository workingDayRepository;

    @Autowired
    private EntityManager entityManager;

    private final int FridayWorkHours = 6;
    private final int WeeklyWorkHours = 8;
    private LocalTime registeredPauseExit = null;
    private int calculatedPauseExit = 0;

    public void registerEntry(LocalTime entryTime){
        WorkingDay today = workingDayRepository.findByDate(LocalDate.now())
                .orElse(new WorkingDay(LocalDate.now()));
        //day.setDate(LocalDate.now());
        today.setEntryTime(entryTime);
        today.setBonusOrDebito(Duration.ZERO);
        workingDayRepository.save(today);
    }

    public void registerExit(LocalTime exitTime){
        WorkingDay today = workingDayRepository.findAll()
                .stream()
                .filter(day -> day.getDate().equals(LocalDate.now()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Orario d'ingresso non trovato!"));
        today.setExitTime(exitTime);
        DayOfWeek todayOfWeek = today.getDate().getDayOfWeek();
        if(todayOfWeek == DayOfWeek.SATURDAY  || todayOfWeek == DayOfWeek.SUNDAY){
            today.setBonusOrDebito(Duration.ZERO);
        }else {
            Duration dailyWorkedHours = calculateWorkedHours(today.getEntryTime(), today.getExitTime());
            Duration requiredHours = calculateRequiredDailyHours(today.getDate().getDayOfWeek());
            Duration bonusOrDebito = dailyWorkedHours.minus(requiredHours);
            if(calculatedPauseExit>0){
                bonusOrDebito = bonusOrDebito.minus(Duration.ofMinutes(calculatedPauseExit));
            }
            today.setBonusOrDebito(bonusOrDebito);
            today.setBonusDebFormatted(formatDuration(bonusOrDebito.abs()));
        }
        workingDayRepository.save(today);
    }

    private Duration calculateWorkedHours(LocalTime entry, LocalTime exit){
        return Duration.between(entry,exit);
    }

    public Duration calculateRequiredDailyHours(DayOfWeek dayOfWeek){
        if(dayOfWeek == DayOfWeek.FRIDAY){
            return Duration.ofHours(FridayWorkHours);
        }else if (dayOfWeek.getValue()>=DayOfWeek.MONDAY.getValue() && dayOfWeek.getValue()<=DayOfWeek.SUNDAY.getValue()){
            return Duration.ofHours(WeeklyWorkHours);
        }else{
            return Duration.ZERO;
        }
    }

    public LocalTime registerPauseExit(){
        registeredPauseExit = LocalTime.now();
        return LocalTime.now();
    }

    public LocalTime registerPauseEntry(){
        return LocalTime.now();
    }


    public int calculatePauseTime(){
        int pauseTime = registerPauseEntry().getMinute()-registeredPauseExit.getMinute();
        if(pauseTime>30){
            System.out.println("Pausa superiore a 30 minuti!");
            pauseTime = pauseTime-30;
            calculatedPauseExit = pauseTime;
        }
        return pauseTime;
    }

    public Duration calculateTotalBonusOrDebito(){
        List<WorkingDay> days = workingDayRepository.findAll();
        return days.stream()
                .filter(day ->{
                    DayOfWeek dayOfWeek = day.getDate().getDayOfWeek();
                    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
                })
                .map(day ->{
                    Duration workedHours = day.getWorkedHours();
                    Duration requiredHours = calculateRequiredDailyHours(day.getDate().getDayOfWeek());
                    return workedHours.minus(requiredHours);
                })
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);
    }
   public Duration getBonusOrDebitoCumulativo(){
        Duration totalBonusOrDebito = calculateTotalBonusOrDebito();

        WorkingDay today = workingDayRepository.findAll()
                .stream()
                .filter(day -> day.getDate().equals(LocalDate.now()))
                .findFirst()
                .orElse(null);

        if (today!=null){
            return totalBonusOrDebito.plus(today.getBonusOrDebito());
        }else{
            return totalBonusOrDebito;
        }
   }

   public Duration calculateTotalBonusOrDebitoCumulat(){
       List<WorkingDay> workingDays = workingDayRepository.findAll();
       Duration total = workingDays.stream().map(WorkingDay::getBonusOrDebito)
               .reduce(Duration.ZERO, Duration::plus);
       return total;
   }

    public String OrarioDiUscitaPrevisto() {
        String result = "";
        WorkingDay today = workingDayRepository.findByDate(LocalDate.now())
                .orElse(new WorkingDay(LocalDate.now()));
        if (calculateTotalBonusOrDebitoCumulat().isNegative()) {
            try {
                result= "Orario di uscita previsto: " + today.getEntryTime().plus(calculateRequiredDailyHours(today.getDate().getDayOfWeek()).plus(calculateTotalBonusOrDebitoCumulat().abs()));
            } catch (NullPointerException e) {
                result= "Attenzione! Per visualizzare l'orario di uscita previsto deve essere badgiato necessariamente l'ingresso dell'attuale giornata lavorativa!";
            }
        } else {
            try {
                result= "Orario di uscita previsto: " + today.getEntryTime().plus(calculateRequiredDailyHours(today.getDate().getDayOfWeek()).minus(calculateTotalBonusOrDebitoCumulat().abs()));
            } catch (NullPointerException e) {
                result= "Attenzione! Per visualizzare l'orario di uscita previsto deve essere badgiato necessariamente l'ingresso dell'attuale giornata lavorativa!";
            }
        }
        return result;
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void clearPreviousWeekRecords(){
        LocalDate today = LocalDate.now();
        LocalDate startOfCurrentWeek = today.with(DayOfWeek.MONDAY);

        workingDayRepository.deleteAllByDateBefore(startOfCurrentWeek);
        System.out.println("Eliminati i record prima del: " + startOfCurrentWeek);
    }

    public void registerWorkHours(LocalTime entryTime, LocalTime exitTime){
        WorkingDay today = workingDayRepository.findByDate(LocalDate.now())
                .orElse(new WorkingDay(LocalDate.now()));
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("L'orario di uscita deve essere successivo all'orario di entrata.");
        }
        today.setEntryTime(entryTime);
        today.setExitTime(exitTime);
        Duration diff = calculateWorkedHours(entryTime,exitTime);
        today.setBonusOrDebito(diff.minus(calculateRequiredDailyHours(LocalDate.now().getDayOfWeek())));
        today.setBonusDebFormatted(formatDuration(diff.minus(calculateRequiredDailyHours(LocalDate.now().getDayOfWeek())).abs()));
        //System.out.println("Bonus/Debito: " + today.getBonusDebFormatted());
        workingDayRepository.save(today);
    }
   public void addWorkingDay(LocalTime entryTime, LocalTime exitTime){
      WorkingDay workingDay = new WorkingDay();
      //workingDay.setEntryTime(entryTime);
      //workingDay.setExitTime(exitTime);
       Duration diff = calculateWorkedHours(entryTime,exitTime);
       workingDay.setBonusDebFormatted(formatDuration(diff.minus(calculateRequiredDailyHours(LocalDate.now().getDayOfWeek()))));
      //workingDay.setBonusOrDebito(Duration.between(calculateWorkedHours(entryTime,exitTime),)));

       //workingDayRepository.save(workingDay);
    }

    public List<WorkingDay> getAllWorkingDays() {
        return workingDayRepository.findAll();
    }
    private String formatDuration(Duration duration){
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d",hours,minutes);
    }

    public Optional<WorkingDay> findById(Long id) {
        return workingDayRepository.findById(id);
    }

    public void updateWorkHours(Long id, LocalTime entryTime, LocalTime exitTime){
        WorkingDay workingDay = workingDayRepository.findById(id).orElseThrow();
        workingDay.setEntryTime(entryTime);
        workingDay.setExitTime(exitTime);
        Duration workedHours = calculateWorkedHours(entryTime, exitTime);
        workingDay.setBonusOrDebito(workedHours.minus(calculateRequiredDailyHours(LocalDate.now().getDayOfWeek())));
        workingDay.setBonusDebFormatted(formatDuration(workedHours.minus(calculateRequiredDailyHours(LocalDate.now().getDayOfWeek())).abs()));
        workingDayRepository.save(workingDay);
    }

    public void deleteById(Long id) {
        workingDayRepository.deleteById(id);
    }

    @Transactional
    @EventListener(ContextRefreshedEvent.class)
    public void clearPreviousWeekRecordsOnStartup() {
        LocalDate today = LocalDate.now();
        LocalDate startOfCurrentWeek = today.with(DayOfWeek.MONDAY);

        resetAutoIncrement();
        workingDayRepository.deleteAllByDateBefore(startOfCurrentWeek);
        System.out.println("Eliminati i record prima del: " + startOfCurrentWeek + " all'avvio dell'app.");
    }
    
    public void resetAutoIncrement(){
        entityManager.createNativeQuery("ALTER TABLE working_day AUTO_INCREMENT = 1").executeUpdate();
    }
}
