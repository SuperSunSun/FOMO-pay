package com.hejz.autoconfigure;

import org.springframework.context.annotation.ComponentScan;

/**
 * 配置spring扫瞄类
 */
@ComponentScan(basePackages = {
        "com.hejz.service"
})
public class WebInit {
}