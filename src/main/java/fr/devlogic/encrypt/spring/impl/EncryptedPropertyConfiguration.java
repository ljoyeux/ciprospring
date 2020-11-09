package fr.devlogic.encrypt.spring.impl;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class EncryptedPropertyConfiguration {
    @Bean
    public BeanPostProcessor beanPostProcessor(Environment environment) {
        return new EncryptedPropertyBeanProcessor(environment);
    }
}
