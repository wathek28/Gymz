package com.example.demo.model;

import com.example.demo.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventReminderScheduler {

    @Autowired
    private NotificationService notificationService;

    // Se lance tous les jours à 9h00
    @Scheduled(cron = "0 0 9 * * ?")
    public void envoyerRappelsEvenements() {
        notificationService.notifierRappelEvenement();
    }
}