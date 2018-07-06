package com.bossien.flowtreatmentservice.service;

import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.GroupTotalScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseService {

    private Map<String, Object> limitGroup(int currentPage, int pageSize, List<GroupTotalScore> maps) {
        int size = maps.size();
        int currIdx = (currentPage > 1 ? (currentPage - 1) * pageSize : 0);
        List<GroupTotalScore> pageMap = new ArrayList<>();
        for (int i = 0; i < pageSize && i < maps.size() - currIdx; i++) {
            GroupTotalScore groupTotalScore = maps.get(currIdx + i);
            pageMap.add(groupTotalScore);
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", size);
        resultMap.put("data", pageMap);
        return resultMap;
    }

    private Map<String, Object> limitMap(int currentPage, int pageSize, List<Map<String, Object>> maps) {
        int size = maps.size();
        int currIdx = (currentPage > 1 ? (currentPage - 1) * pageSize : 0);
        List<Map<String, Object>> pageMap = new ArrayList<>();
        for (int i = 0; i < pageSize && i < maps.size() - currIdx; i++) {
            Map<String, Object> stringObjectMap = maps.get(currIdx + i);
            pageMap.add(stringObjectMap);
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", size);
        resultMap.put("data", pageMap);
        return resultMap;
    }
    private List<Long> typeToIds(List<Company> companies1) {
        List<Long> longs = new ArrayList<>();
        for (Company company : companies1) {
            longs.add(company.getId());
        }
        return longs;
    }

    private boolean checkNotNullList(List<?> list) {
        if (null != list && list.size() != 0) {
            return true;
        } else {
            return false;
        }
    }

}
