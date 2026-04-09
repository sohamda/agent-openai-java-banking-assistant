// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.assistant.config;

import com.microsoft.openai.samples.assistant.agent.cache.InMemoryToolsExecutionCache;
import com.microsoft.openai.samples.assistant.agent.cache.ToolsExecutionCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class ToolExecutionCacheConfiguration {

    @Bean
    @SessionScope(proxyMode = ScopedProxyMode.INTERFACES)
    public ToolsExecutionCache inMemorytoolExecutionCache() {
        return new InMemoryToolsExecutionCache();
    }
}
