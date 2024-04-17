package com.alibaba.cloud.nacos;

import com.alibaba.cloud.nacos.client.OvseNacosPropertySourceLocator;
import com.alibaba.cloud.nacos.encryption.NoDeductMoneyNacosEncryption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class NacosConfigBootstrapConfiguration {

    @Bean
    public NoDeductMoneyNacosEncryption noDeductMoneyNacosEncryption(Environment environment) {
        return new NoDeductMoneyNacosEncryption(environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public NacosConfigProperties nacosConfigProperties() {
        return new NacosConfigProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public NacosConfigManager nacosConfigManager(NacosConfigProperties nacosConfigProperties) {
        return new NacosConfigManager(nacosConfigProperties);
    }

    @Bean
    public OvseNacosPropertySourceLocator nacosPropertySourceLocator(NacosConfigManager nacosConfigManager, NoDeductMoneyNacosEncryption noDeductMoneyNacosEncryption) {
        return new OvseNacosPropertySourceLocator(nacosConfigManager, noDeductMoneyNacosEncryption);
    }

}
