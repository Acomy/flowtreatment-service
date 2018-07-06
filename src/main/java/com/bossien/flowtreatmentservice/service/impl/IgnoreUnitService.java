package com.bossien.flowtreatmentservice.service.impl;

import com.bossien.flowtreatmentservice.constant.IgnoreUnitEnum;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.utils.PlatformCode;
import com.bossien.flowtreatmentservice.utils.TimeManager;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class IgnoreUnitService {

    private List<Long> ignoreUnits;

    private List<Long> countryIgnoreUnits;
    @Autowired
    CompanyRepository companyRepository;
    @Autowired
    TimeManager timeManager;

    public List<Long> getIgnoreUnitIds() {
        if(timeManager.getCode().equals(PlatformCode.HB)){
            //不需要统计的的单位
            if (ignoreUnits == null ||ignoreUnits.size()==0) {
                String hubeijianyuxitong = IgnoreUnitEnum.HUBEIJIANYUXITONG.getUnitName();
                String hubeijieduxiton = IgnoreUnitEnum.HUBEIJIEDUXITON.getUnitName();
                String hubeishengsifating = IgnoreUnitEnum.HUBEISHENGSIFATING.getUnitName();
                ignoreUnits = companyRepository.findIdsByHql(Arrays.asList(hubeijianyuxitong, hubeijieduxiton, hubeishengsifating));
            }
        }else{
            ignoreUnits =new ArrayList<>();
        }

        return ignoreUnits;
    }
    public List<Long> getCountryIgnoreUnitIds() {
        if(timeManager.getCode().equals(PlatformCode.COUNTRY)){
            //不需要统计的的单位
            if (countryIgnoreUnits == null ||countryIgnoreUnits.size()==0) {
                countryIgnoreUnits = companyRepository.queryIdsIgnoreByHql();
            }
        }else{
            countryIgnoreUnits =new ArrayList<>();
        }

        return countryIgnoreUnits;
    }
}
