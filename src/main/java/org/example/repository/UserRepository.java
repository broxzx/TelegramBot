package org.example.repository;

import org.example.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
// Клас для створення запитів в базу даних
public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
