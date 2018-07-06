package com.bossien.flowtreatmentservice.controller;

import com.bossien.common.base.ResponseData;
import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.flowtreatmentservice.handler.child.RecordHandler;
import org.junit.validator.PublicClassValidator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rec")
public class RecController {
    @PostMapping("/accept")
    public ResponseData accept(@RequestBody ComputingResourceModel computingResourceModel) {
        if (computingResourceModel != null) {
            try {
                RecordHandler recordHandler = new RecordHandler();
                recordHandler.process(computingResourceModel);
                return ResponseData.ok();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseData.fail("-->>>>没有获取到信息...");
            }
        } else {
            return ResponseData.fail("-->>>>没有获取到信息...");
        }
    }
}
