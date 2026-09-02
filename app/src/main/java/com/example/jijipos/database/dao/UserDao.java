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

    @Query("SELECT * FROM users WHERE phoneNumber =:phone  LIMIT 1")
    User getUserByPhone(String phone);

    @Query("SELECT * FROM users WHERE businessId= :businessId AND role = 'CASHIER' ")
    List<User> getCashierForBusiness(long businessId);
}
