package com.umaitpen.travelx.service;

import com.umaitpen.travelx.enums.Role;
import com.umaitpen.travelx.model.User;

import java.util.List;

public interface UserService {
    User register(User user);
    User findByEmail(String email);
    User findById(Long id);
    List<User> findAll();
    List<User> findByRole(Role role);
    long countByRole(Role role);
}
