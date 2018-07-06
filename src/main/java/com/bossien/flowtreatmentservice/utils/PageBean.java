package com.bossien.flowtreatmentservice.utils;

import lombok.Data;

import java.util.List;

@Data
public class PageBean<T> {
    //状态
    private Integer code = 200;

    private String message = "";
    //总记录数
    private long total;
    //结果集
    private List<T> list;
    // 第几页
    private int pageNum;
    // 每页记录数
    private int pageSize;
    // 总页数
    private int pages;
    // 当前页的数量 <= pageSize，该属性来自ArrayList的size属性
    private int size;



}
