package com.example.jijipos.database.dao;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.jijipos.database.entity.User;
import com.example.jijipos.database.entity.Business;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    Long insertUser(User user);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("SELECT * FROM users WHERE phoneNumber =:phone  LIMIT 1")
    User getUserByPhone(String phone);

    @Query("SELECT * FROM users WHERE businessId= :businessId AND role = 'CASHIER' ")
    List<User> getCashierForBusiness(long businessId);

    @Query("SELECT businessId FROM users WHERE phoneNumber = :invitationCode AND role = 'MANAGER' LIMIT 1")
    Long verifyManagerInvitationToken(String invitationCode);

    @Query("SELECT * FROM users WHERE role = 'CASHIER' AND businessId = (SELECT businessId FROM users WHERE phoneNumber = :managerPhone AND role = 'MANAGER' LIMIT 1)")
    List<User> getCashiersForManager(String managerPhone);
}
