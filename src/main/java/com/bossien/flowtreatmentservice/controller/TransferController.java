package com.bossien.flowtreatmentservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Iterator;
import java.util.Set;

@RestController
@RequestMapping("/transfer")
public class TransferController {
    @Autowired
    RedisTemplate redisTemplate ;
    @GetMapping("/mongo")
    public String transferToMongo(){
       return "ok";
    }
}
