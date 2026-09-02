package com.example.jijipos.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import com.example.jijipos.database.entity.Business;

@Dao
public interface BusinessDao {
    @Insert
    long insertBusiness(Business business);
}
