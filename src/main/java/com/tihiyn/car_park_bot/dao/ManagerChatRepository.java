package com.tihiyn.car_park_bot.dao;

import com.tihiyn.car_park_bot.dao.model.ManagerChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerChatRepository extends JpaRepository<ManagerChat, String> {
    boolean existsManagerChatByChatId(Long chatId);
    Optional<ManagerChat> findManagerChatByChatId(Long chatId);
}
