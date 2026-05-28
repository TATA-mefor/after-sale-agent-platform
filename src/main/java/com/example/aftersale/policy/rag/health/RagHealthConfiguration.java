package com.example.aftersale.policy.rag.health;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RagHealthProperties.class)
public class RagHealthConfiguration {
}
