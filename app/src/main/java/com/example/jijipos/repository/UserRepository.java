package com.example.jijipos.repository;

import android.content.Context;

import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.dao.UserDao;
import com.example.jijipos.database.entity.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private final UserDao userDao;
    private final ExecutorService executorService;

    public UserRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    // Interface callback for operations returning data asynchronously
    public interface RepositoryCallBack<T>{
        void onComplete(T result);
    }

    //Safely insert a user in the background
    public void insertUser(User user, RepositoryCallBack<Long> callBack){
        executorService.execute(() -> {
            long newId = userDao.insertUser(user);
            if ( callBack != null){
                callBack.onComplete(newId);
            }
        });
    }

    //safely lookup a user by phone number
    public void getUserByPhone(String phone, RepositoryCallBack<User> callBack){
        executorService.execute(() -> {
            User user = userDao.getUserByPhone(phone);
            if (callBack != null){
                callBack.onComplete(user);
            }
        });
    }

    //safely fetch cashier sub-accounts for the manager
    public void getCashierForBusiness(long businessId, RepositoryCallBack<List<User>> callBack){
        executorService.execute(() -> {
            List<User> cashiers = userDao.getCashierForBusiness(businessId);
            if ( callBack != null ){
                callBack.onComplete(cashiers);
            }
        });
    }
}
