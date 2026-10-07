package kr.noco.qticket.ui.realtime.api;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(QueueStreamProperties.class)
public class QueueStreamConfiguration {
}
