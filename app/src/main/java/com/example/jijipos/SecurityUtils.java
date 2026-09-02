package com.example.jijipos;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtils {
    public static String hashPassword(String password){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for(byte b : hash){
                String hex = Integer.toHexString(0xff & b); //0xff = 255 haizidi hapa. The 0x means the number is base 16
                if(hex.length() == 1) hexString.append(0);
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            return password;
        }
    }
}
