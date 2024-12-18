package com.WorkHoursApplication.demo.controller;

import com.WorkHoursApplication.demo.model.WorkHoursForm;
import com.WorkHoursApplication.demo.model.WorkingDay;
import com.WorkHoursApplication.demo.service.WorkHoursService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class WorkHoursController {
    @Autowired
    private WorkHoursService workHoursService;

    @GetMapping("/workhours")
    public String showWorkHoursForm(Model model){
        model.addAttribute("workHoursForm", new WorkHoursForm());
        return "workhours";
    }

    @PostMapping("/workhours")
    public String submitWorkHoursForm(@ModelAttribute WorkHoursForm form, Model model){
        try{
            workHoursService.registerWorkHours(form.getEntryTime(), form.getExitTime());
            Duration bonus = workHoursService.calculateTotalBonusOrDebitoCumulat();
            if(bonus==Duration.ZERO){
                model.addAttribute("bonus",0);
            }else if(bonus.isPositive()) {
                model.addAttribute("bonus","Bonus cumulato: " + formatDuration(bonus));
            }else{
                model.addAttribute("bonus","Debito cumulato: " + formatDuration(bonus));
            }
            //model.addAttribute("bonus", formatDuration(bonus));
            model.addAttribute("workingDays", workHoursService.getAllWorkingDays());
            return "workhours";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "L'orario d'uscita è successivo a quello d'ingresso, il record non sarà inserito! Riprovare con i dati corretti.");
            model.addAttribute("workingDays", workHoursService.getAllWorkingDays());
            return "workhours";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Si è verificato un errore imprevisto.");
            model.addAttribute("workingDays", workHoursService.getAllWorkingDays());
            return "workhours";
        }
    }

    @GetMapping("/weekStatus")
    public String getCurrentWorkHoursStatus(Model model){
        List<WorkingDay> workingDays = workHoursService.getAllWorkingDays();
        Duration totalBonusOrDebt = workHoursService.calculateTotalBonusOrDebitoCumulat();
        model.addAttribute("workingDays",workingDays);
        if(totalBonusOrDebt==Duration.ZERO){
            model.addAttribute("totalBonusOrDebt",0);
        }else if(totalBonusOrDebt.isPositive()) {
            model.addAttribute("totalBonusOrDebt","Bonus cumulato: " + formatDuration(totalBonusOrDebt));
        }else{
            model.addAttribute("totalBonusOrDebt","Debito cumulato: " + formatDuration(totalBonusOrDebt));
        }
        //model.addAttribute("totalBonusOrDebt",formatDuration(totalBonusOrDebt));

        return "weekStatus";
    }
    @GetMapping("/workhours/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model){
        Optional<WorkingDay> workingDay = workHoursService.findById(id);
        if(workingDay.isPresent()){
            WorkingDay day = workingDay.get();
            WorkHoursForm form = new WorkHoursForm(day.getEntryTime(), day.getExitTime());
            form.setId(day.getId());
            model.addAttribute("workHoursForm", form);
            return "edit-workhours";
        } else {
            model.addAttribute("errorMessage", "Orario di lavoro non trovato");
            return "redirect:/workhours";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateWorkHours(@ModelAttribute WorkHoursForm form,@PathVariable("id") Long id){
        workHoursService.updateWorkHours(id, form.getEntryTime(), form.getExitTime());
        return "redirect:/workhours";
    }

    @GetMapping("/workhours/delete/{id}")
    @Transactional
    public String deleteWorkHours(@PathVariable("id") Long id){
        workHoursService.deleteById(id);
        workHoursService.resetAutoIncrement();
        return "redirect:/workhours";
    }




    private String formatDuration(Duration duration) {
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
}
