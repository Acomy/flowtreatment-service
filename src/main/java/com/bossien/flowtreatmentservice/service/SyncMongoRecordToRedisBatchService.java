package com.bossien.flowtreatmentservice.service;

public interface SyncMongoRecordToRedisBatchService {
    public void batch();
    public void  updataCompany();
    void batchHb();
    void durationUpdata();

    void scoreUpdate();
}
