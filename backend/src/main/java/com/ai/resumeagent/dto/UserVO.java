package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户信息视图对象（不含敏感字段）
 */
@Data
@Builder
public class UserVO {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String avatar;

    private String school;

    private String major;

    private String education;

    private Integer graduationYear;

    private String jobIntention;

    private Integer role;
}
