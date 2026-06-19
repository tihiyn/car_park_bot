package com.tihiyn.car_park_bot.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ManagerChat {
    @Id
    private Long chatId;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false, unique = true)
    private String jwt;

    public String getUsername() {
        return username;
    }

    public ManagerChat setUsername(String username) {
        this.username = username;
        return this;
    }

    public Long getChatId() {
        return chatId;
    }

    public ManagerChat setChatId(Long chatId) {
        this.chatId = chatId;
        return this;
    }

    public String getJwt() {
        return jwt;
    }

    public ManagerChat setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }
}
