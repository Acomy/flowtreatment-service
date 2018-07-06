package com.bossien.flowtreatmentservice.utils;

/**
 * 得分获取
 * @author gb
 */
public class ScoreListHelper {

    public static int getDayCompanyScore(Long score) {
        int companyScore = 0;
        if (score < 40) {
        } else if (40 <= score && score <= 49) {
        companyScore = 1;
        } else if (50 <= score && score <= 59) {
        companyScore = 2;
        } else if (60 <= score && score <= 69) {
        companyScore = 3;
        } else if (70 <= score && score <= 79) {
        companyScore = 4;
        } else if (80 <= score && score <= 89) {
        companyScore = 5;
        } else if (90 <= score && score <= 100) {
        companyScore = 6;
        }
        return companyScore;
        }

        }
