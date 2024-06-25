package com.itheima.mybatistest.config;


import com.itheima.mybatistest.utils.ParameterMapping;

import java.util.List;

public class BoundSql {

    /**
     * 预编译能直接使用的sql
     * select a from tb where a = ?
     */
    private String finalSql;

    /**
     * 字段映射:id name (源码抄过来的，主要是ParameterMappingTokenHandler#getParameterMappings)
     */
    private List<ParameterMapping> parameterMappingList;

    public BoundSql(String finalSql, List<ParameterMapping> parameterMappingList) {
        this.finalSql = finalSql;
        this.parameterMappingList = parameterMappingList;
    }

    public String getFinalSql() {
        return finalSql;
    }

    public void setFinalSql(String finalSql) {
        this.finalSql = finalSql;
    }

    public List<ParameterMapping> getParameterMappingList() {
        return parameterMappingList;
    }

    public void setParameterMappingList(List<ParameterMapping> parameterMappingList) {
        this.parameterMappingList = parameterMappingList;
    }
}
