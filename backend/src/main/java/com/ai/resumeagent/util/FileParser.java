package com.ai.resumeagent.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 简历文件内容解析：支持 PDF / Word / TXT
 */
@Slf4j
@Component
public class FileParser {

    private final Tika tika = new Tika();

    /**
     * 从字节数组解析文本内容
     */
    public String parse(byte[] bytes, String fileName) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            String content = tika.parseToString(input);
            if (content == null) {
                return "";
            }
            // 压缩多余空白，便于 AI 读取
            return content.replaceAll("[\\t\\r\\n]+", "\n").trim();
        } catch (org.apache.tika.exception.TikaException e) {
            throw new IOException("简历解析失败", e);
        }
    }
}
