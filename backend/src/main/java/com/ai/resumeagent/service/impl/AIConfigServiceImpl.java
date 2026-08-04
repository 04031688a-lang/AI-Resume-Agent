package com.ai.resumeagent.service.impl;

import com.ai.resumeagent.entity.AiConfig;
import com.ai.resumeagent.mapper.AiConfigMapper;
import com.ai.resumeagent.service.AIConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AIConfigServiceImpl implements AIConfigService {

    private final AiConfigMapper aiConfigMapper;

    @Override
    public String get(String key) {
        AiConfig config = aiConfigMapper.selectOne(new LambdaQueryWrapper<AiConfig>()
                .eq(AiConfig::getConfigKey, key)
                .last("LIMIT 1"));
        return config == null ? null : config.getConfigValue();
    }

    @Override
    public void set(String key, String value, String description) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        AiConfig config = aiConfigMapper.selectOne(new LambdaQueryWrapper<AiConfig>()
                .eq(AiConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (config == null) {
            config = new AiConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            aiConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            config.setDescription(description);
            aiConfigMapper.updateById(config);
        }
    }
}
