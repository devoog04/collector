package com.github.devoog04.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collector")
@Getter
@Setter
public class CollectorProperties {
    private boolean enabled = true;

}