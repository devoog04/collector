package com.github.devoog04.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan("com.github.devoog04.scheduling")
@EnableConfigurationProperties(CollectorProperties.class)
@ConditionalOnProperty(prefix = "collector", name = "enabled", havingValue = "true")
public class CollectorAutoConfiguration {

}