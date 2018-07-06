package com.bossien.flowtreatmentservice.entity;

import lombok.Data;

import javax.persistence.Transient;

/**
 * @author gb
 */
@Data
public class GroupTotalScore {

    private String departName;

    private String departid;

    private String companyScores;

    private String count ;
    @Transient
    private String provinceRanking;
    @Transient
    private String countryRanking;
    @Transient
    private String totalPersonCount ;

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getDepartid() {
        return departid;
    }

    public void setDepartid(String departid) {
        this.departid = departid;
    }

    public String getCompanyScores() {
        return companyScores;
    }

    public void setCompanyScores(String companyScores) {
        this.companyScores = companyScores;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getCountryRanking() {
        return countryRanking;
    }

    public void setCountryRanking(String countryRanking) {
        this.countryRanking = countryRanking;
    }

    public String getTotalPersonCount() {
        return totalPersonCount;
    }

    public void setTotalPersonCount(String totalPersonCount) {
        this.totalPersonCount = totalPersonCount;
    }

    public String getProvinceRanking() {
        return provinceRanking;
    }

    public void setProvinceRanking(String provinceRanking) {
        this.provinceRanking = provinceRanking;
    }
}
