package com.example.jijipos.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.jijipos.database.dao.BusinessDao;
import com.example.jijipos.database.dao.TransactionDao;
import com.example.jijipos.database.dao.UserDao;
import com.example.jijipos.database.entity.Business;
import com.example.jijipos.database.entity.Transaction;
import com.example.jijipos.database.entity.User;

@Database(entities = {Business.class, User.class, Transaction.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract TransactionDao transactionDao();
    public abstract BusinessDao businessDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "jiji_pos_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
