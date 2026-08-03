package com.ai.resumeagent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    private String email;

    private String phone;

    private String avatar;

    private String school;

    private String major;

    /** 学历：本科/硕士/博士 */
    private String education;

    private Integer graduationYear;

    /** 求职意向 */
    private String jobIntention;

    /** 0=学生，1=管理员 */
    private Integer role;

    /** 0=禁用，1=正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
