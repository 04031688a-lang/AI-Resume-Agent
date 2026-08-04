package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.common.ResultCode;
import com.ai.resumeagent.common.exception.BusinessException;
import com.ai.resumeagent.dto.ResumeDetailVO;
import com.ai.resumeagent.dto.ResumeVO;
import com.ai.resumeagent.entity.Resume;
import com.ai.resumeagent.mapper.ResumeMapper;
import com.ai.resumeagent.service.ResumeService;
import com.ai.resumeagent.util.FileParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "doc", "docx", "txt");
    private static final int PREVIEW_LENGTH = 2000;

    private final ResumeMapper resumeMapper;
    private final FileParser fileParser;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public ResumeVO upload(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的文件");
        }

        String originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "resume"));
        String ext = getExtension(originalName);
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持 PDF / Word(.doc/.docx) / TXT 格式");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件读取失败");
        }

        // 1. 解析内容
        String parsedContent;
        try {
            parsedContent = fileParser.parse(bytes, originalName);
        } catch (Exception e) {
            log.error("简历解析失败, file={}", originalName, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "简历解析失败，请确认文件未损坏");
        }

        // 2. 保存文件
        String relativePath = userId + "/" + UUID.randomUUID() + "." + ext;
        Path target = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            log.error("简历文件保存失败", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败");
        }

        // 3. 入库
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(originalName);
        resume.setFileUrl(relativePath);
        resume.setFileType(ext);
        resume.setFileSize(file.getSize());
        resume.setParsedContent(parsedContent);
        resume.setStatus(2);
        resumeMapper.insert(resume);
        return toVO(resume);
    }

    @Override
    public List<ResumeVO> list(Long userId) {
        return resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
                        .orderByDesc(Resume::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ResumeDetailVO detail(Long id, Long userId) {
        Resume resume = getOwned(id, userId);
        String content = resume.getParsedContent();
        String preview = content == null ? "" : content.substring(0, Math.min(content.length(), PREVIEW_LENGTH));
        return ResumeDetailVO.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .status(resume.getStatus())
                .contentPreview(preview)
                .createdAt(resume.getCreatedAt())
                .build();
    }

    @Override
    public void delete(Long id, Long userId) {
        Resume resume = getOwned(id, userId);
        resumeMapper.deleteById(id);
        // 删除本地文件（尽力而为）
        try {
            Path file = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(resume.getFileUrl());
            Files.deleteIfExists(file);
        } catch (Exception e) {
            log.warn("删除简历文件失败, id={}", id, e);
        }
    }

    public Resume getOwned(Long id, Long userId) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "简历不存在");
        }
        return resume;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private ResumeVO toVO(Resume resume) {
        return ResumeVO.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .status(resume.getStatus())
                .createdAt(resume.getCreatedAt())
                .build();
    }
}
