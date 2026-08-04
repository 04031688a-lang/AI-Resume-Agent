package com.ai.resumeagent.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 简历文本解析单元测试
 */
class FileParserTest {

    @Test
    void parseTxtContent() throws Exception {
        FileParser parser = new FileParser();
        String content = "姓名：张三\n求职意向：Java 后端开发\n技能：Spring Boot、MySQL";

        String parsed = parser.parse(content.getBytes(StandardCharsets.UTF_8), "resume.txt");

        assertTrue(parsed.contains("张三"));
        assertTrue(parsed.contains("Java 后端开发"));
        assertTrue(parsed.contains("Spring Boot"));
        assertFalse(parsed.isBlank());
    }
}
