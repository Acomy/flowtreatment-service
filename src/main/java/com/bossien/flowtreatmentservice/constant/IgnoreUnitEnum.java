package com.bossien.flowtreatmentservice.constant;


/**
 * 忽略单位
 *
 * @author gb
 */
public enum IgnoreUnitEnum {

    JIANANZHONGXIN("水利部建安中心"),
    HUBEIJIANYUXITONG("湖北监狱系统"),
    HUBEIJIEDUXITON("湖北戒毒系统"),
    HUBEISHENGSIFATING("湖北省司法厅");

    private final String unitName;

    IgnoreUnitEnum(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitName() {
        return unitName;
    }
}
