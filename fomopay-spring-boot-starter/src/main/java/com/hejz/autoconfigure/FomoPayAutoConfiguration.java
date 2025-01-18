package com.hejz.autoconfigure;

import com.hejz.service.FomoPayService;
import com.hejz.service.FomoPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(FomoPayProperties.class)
@Import(WebInit.class)
@Slf4j
public class FomoPayAutoConfiguration {
    @Bean
    public FomoPayService fomoPayService(FomoPayProperties fomoPayProperties) {
        return new FomoPayServiceImpl(fomoPayProperties);
    }
}
