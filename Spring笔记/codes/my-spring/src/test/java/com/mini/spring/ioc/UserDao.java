package com.mini.spring.ioc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDao {
    private Map<String,String> userMap=new HashMap<>();

    public UserDao(){
        userMap.put("xiaowang","19");
        userMap.put("xiaoli","54");
    }

    public String selectByName(String name) {
        return userMap.get(name);
    }
}
