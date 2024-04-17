package com.alibaba.cloud.nacos.client;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.cloud.nacos.NacosPropertySourceRepository;
import com.alibaba.cloud.nacos.encryption.NoDeductMoneyNacosEncryption;
import com.alibaba.cloud.nacos.parser.NacosDataParserHandler;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;

import java.util.Collections;
import java.util.Date;
import java.util.List;


public class OvseNacosPropertySourceBuilder extends NacosPropertySourceBuilder {

    private static final Logger log = LoggerFactory.getLogger(NacosPropertySourceBuilder.class);
    private ConfigService configService;
    private long timeout;
    private NoDeductMoneyNacosEncryption noDeductMoneyNacosEncryption;

    public OvseNacosPropertySourceBuilder(ConfigService configService, long timeout, NoDeductMoneyNacosEncryption noDeductMoneyNacosEncryption) {
        super(configService, timeout);
        this.configService = configService;
        this.timeout = timeout;
        this.noDeductMoneyNacosEncryption = noDeductMoneyNacosEncryption;
    }


    NacosPropertySource build(String dataId, String group, String fileExtension, boolean isRefreshable) {
        List<PropertySource<?>> propertySources = this.loadNacosData(dataId, group, fileExtension);
        NacosPropertySource nacosPropertySource = new NacosPropertySource(propertySources, group, dataId, new Date(), isRefreshable);
        NacosPropertySourceRepository.collectNacosPropertySource(nacosPropertySource);
        return nacosPropertySource;
    }

    private List<PropertySource<?>> loadNacosData(String dataId, String group, String fileExtension) {
        String data = null;

        try {
            data = this.configService.getConfig(dataId, group, this.timeout);
            if (StringUtils.isEmpty(data)) {
                log.warn("Ignore the empty nacos configuration and get it based on dataId[{}] & group[{}]", dataId, group);
                return Collections.emptyList();
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("Loading nacos data, dataId: '%s', group: '%s', data: %s", dataId, group, data));
            }

            if (this.noDeductMoneyNacosEncryption.checkProcess()) {
                data = this.noDeductMoneyNacosEncryption.process(data);
            }

            return NacosDataParserHandler.getInstance().parseNacosData(dataId, data, fileExtension);
        } catch (NacosException var6) {
            log.error("get data from Nacos error,dataId:{} ", dataId, var6);
        } catch (Exception var7) {
            log.error("parse data from Nacos error,dataId:{},data:{}", new Object[]{dataId, data, var7});
        }
        return Collections.emptyList();
    }









    public long getTimeout() {
        return this.timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public ConfigService getConfigService() {
        return this.configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
}
