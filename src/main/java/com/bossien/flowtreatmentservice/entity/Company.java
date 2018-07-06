package com.bossien.flowtreatmentservice.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
public class Company {
    @Id
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.code
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String code;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.company_name
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String companyName;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.business_id
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Long businessId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.region_id
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Long regionId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.legal_person
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String legalPerson;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.contacter
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String contacter;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.contacter_telephone
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String contacterTelephone;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.post_code
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String postCode;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.address
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String address;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.is_regulatory
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Byte isRegulatory;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.hierarchy
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Byte hierarchy;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.pid
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Long pid;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.state
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Byte state;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.orderby
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Integer orderby;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.remark
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String remark;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.create_user
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String createUser;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.create_date
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Date createDate;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.oper_user
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private String operUser;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.oper_date
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Date operDate;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column company.type_id
     *
     * @mbggenerated Tue Apr 24 13:25:12 CST 2018
     */
    private Byte typeId;

    private Integer peopleNumber;

    @Transient
    private List<Company> child;

    public Company(){

    }
    public Company(Long id ,Long pid){
          this.id =id ;
          this.pid =pid ;
    }

    public Company(Long id, String companyName) {
        this.id = id;
        this.companyName = companyName;
    }
    public Company(Long id, String companyName,Integer peopleNumber) {
        this.id = id;
        this.companyName = companyName;
        this.peopleNumber = peopleNumber;
    }
}