package com.hejz.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "fomopay")
@Data
@Component
public class FomoPayProperties {
    private String apiUrl;
    private String keyId;
    private String tid;
    private String mid;
}
