package com.itheima.mapper;

import com.itheima.pojo.User;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {

  /**
   * 根据条件（id）查询用户
   */
  User findByCondition(int id);

}
