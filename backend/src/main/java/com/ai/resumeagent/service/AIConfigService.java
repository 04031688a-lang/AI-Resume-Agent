package com.ai.resumeagent.service;

public interface AIConfigService {

    String get(String key);

    void set(String key, String value, String description);
}
