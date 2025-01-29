package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;


import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()  // Désactive la protection CSRF si nécessaire (surtout pour les API)
                .authorizeRequests()
                .requestMatchers("/**").permitAll()  // Remplacé antMatchers par requestMatchers
                .anyRequest().permitAll()  // Permet également l'accès à toutes les autres routes
                .and()
                .exceptionHandling()
                .accessDeniedPage("/accessDenied"); // Redirection en cas de permission refusée

        // Log pour confirmer la configuration de sécurité
        System.out.println("Sécurité désactivée pour toutes les routes.");

        return http.build();
    }
}
