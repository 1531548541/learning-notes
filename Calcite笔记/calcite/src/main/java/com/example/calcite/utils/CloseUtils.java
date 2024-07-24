package com.example.calcite.utils;

public class CloseUtils {
    private CloseUtils() {

    }

    public static boolean closeAuto(AutoCloseable closeable){
        try {
            if(null!=closeable){
                closeable.close();
            }
            return true;
        }catch(Exception e){
            return false;
        }
    }
}
