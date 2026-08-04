package com.ai.resumeagent.service;

import com.ai.resumeagent.dto.ResumeDetailVO;
import com.ai.resumeagent.dto.ResumeVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    ResumeVO upload(MultipartFile file, Long userId);

    List<ResumeVO> list(Long userId);

    ResumeDetailVO detail(Long id, Long userId);

    void delete(Long id, Long userId);
}
