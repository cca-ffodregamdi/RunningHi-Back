package com.runninghi.runninghibackv2.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "runninghi.version")
public class VersionCheckerConfig {

    private String current;
    private List<String> exceptions;

}
