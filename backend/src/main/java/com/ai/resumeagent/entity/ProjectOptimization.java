package com.ai.resumeagent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目优化记录表
 */
@Data
@TableName("project_optimization")
public class ProjectOptimization {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String projectName;

    /** 担任角色 */
    private String role;

    /** 用户原始描述 */
    private String originalContent;

    /** STAR 优化后文案 */
    private String optimizedContent;

    /** STAR 四要素（JSON） */
    private String starContent;

    /** 量化与关键词建议 */
    private String suggestions;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
