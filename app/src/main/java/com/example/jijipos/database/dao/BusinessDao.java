package com.example.jijipos.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.jijipos.database.entity.Business;

@Dao
public interface BusinessDao {
    @Insert
    long insertBusiness(Business business);

    // Add this query directly inside your BusinessDao.java interface block
    @Query("SELECT * FROM business WHERE id = :id LIMIT 1")
    com.example.jijipos.database.entity.Business getBusinessById(long id);

}
