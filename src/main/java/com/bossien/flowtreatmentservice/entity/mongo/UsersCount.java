package com.bossien.flowtreatmentservice.entity.mongo;

public class UsersCount {

    private Long companyId ;

    private Long  count ;

    public UsersCount(Long companyId, Long count) {
        this.companyId = companyId;
        this.count = count;
    }
}
