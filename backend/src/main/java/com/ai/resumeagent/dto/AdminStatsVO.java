package com.ai.resumeagent.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 平台数据统计
 */
@Data
@Builder
public class AdminStatsVO {

    private long userTotal;

    private long userActive;

    private long resumeTotal;

    private long resumeAnalysisTotal;

    private long jobMatchTotal;

    private long interviewTotal;

    private long projectTotal;
}
