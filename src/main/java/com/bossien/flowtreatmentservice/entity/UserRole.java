package com.bossien.flowtreatmentservice.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
@Data
@Table(name = "user_role")
@Entity
public class UserRole {
    @Id
    private Long id;

    private String createUser;

    private Date createDate;

    private String operUser;

    private Date operDate;

    private Long userId;

    private Long roleId;

}