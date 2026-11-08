package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setLogin(entity.getLogin());
        user.setPasswordHash(entity.getPasswordHash());
        user.setFirstName(entity.getFirstName());
        user.setLastName(entity.getLastName());
        user.setEmail(entity.getEmail());
        user.setImageUrl(entity.getImageUrl());
        user.setActivated(entity.isActivated());
        user.setLangKey(entity.getLangKey());
        user.setAuthorities(new HashSet<>(entity.getAuthorities()));
        user.setCreatedBy(entity.getCreatedBy());
        user.setCreatedDate(entity.getCreatedDate());
        user.setLastModifiedBy(entity.getLastModifiedBy());
        user.setLastModifiedDate(entity.getLastModifiedDate());
        return user;
    }

    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setLogin(user.getLogin());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setEmail(user.getEmail());
        entity.setImageUrl(user.getImageUrl());
        entity.setActivated(user.isActivated());
        entity.setLangKey(user.getLangKey());
        entity.setAuthorities(new HashSet<>(user.getAuthorities()));
        entity.setCreatedBy(user.getCreatedBy());
        entity.setCreatedDate(user.getCreatedDate());
        entity.setLastModifiedBy(user.getLastModifiedBy());
        entity.setLastModifiedDate(user.getLastModifiedDate());
        return entity;
    }
}
