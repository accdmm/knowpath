package com.li.zhitu.pay.sdk.config;


import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.li.zhitu.pay.sdk.client")
public class PayApiImportConfiguration {

}