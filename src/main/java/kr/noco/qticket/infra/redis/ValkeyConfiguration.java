package kr.noco.qticket.infra.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ValkeyProperties.class)
public class ValkeyConfiguration {}
