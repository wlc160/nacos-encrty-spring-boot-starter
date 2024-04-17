package com.alibaba.cloud.nacos.client;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.cloud.nacos.NacosPropertySourceRepository;
import com.alibaba.cloud.nacos.encryption.NoDeductMoneyNacosEncryption;
import com.alibaba.cloud.nacos.parser.NacosDataParserHandler;
import com.alibaba.cloud.nacos.refresh.NacosContextRefresher;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OvseNacosPropertySourceLocator extends NacosPropertySourceLocator {
    private static final Logger log = LoggerFactory.getLogger(NacosPropertySourceLocator.class);
    private static final String NACOS_PROPERTY_SOURCE_NAME = "NACOS";
    private static final String SEP1 = "-";
    private static final String DOT = ".";
    private NacosPropertySourceBuilder nacosPropertySourceBuilder;
    private NacosConfigProperties nacosConfigProperties;
    private NacosConfigManager nacosConfigManager;
    private NoDeductMoneyNacosEncryption noDeductMoneyNacosEncryption;

    @Deprecated
    public OvseNacosPropertySourceLocator(NacosConfigProperties nacosConfigProperties) {
        super(nacosConfigProperties);
        this.nacosConfigProperties = nacosConfigProperties;
    }

    public OvseNacosPropertySourceLocator(NacosConfigManager nacosConfigManager, NoDeductMoneyNacosEncryption noDeductMoneyNacosEncryption) {
        super(nacosConfigManager);
        this.nacosConfigManager = nacosConfigManager;
        this.nacosConfigProperties = nacosConfigManager.getNacosConfigProperties();
        this.noDeductMoneyNacosEncryption = noDeductMoneyNacosEncryption;
    }

    public PropertySource<?> locate(Environment env) {
        this.nacosConfigProperties.setEnvironment(env);
        ConfigService configService = this.nacosConfigManager.getConfigService();
        if (null == configService) {
            log.warn("no instance of config service found, can't load config from nacos");
            return null;
        } else {
            long timeout = (long) this.nacosConfigProperties.getTimeout();
            this.nacosPropertySourceBuilder = new OvseNacosPropertySourceBuilder(configService, timeout, this.noDeductMoneyNacosEncryption);
            String name = this.nacosConfigProperties.getName();
            String dataIdPrefix = this.nacosConfigProperties.getPrefix();
            if (StringUtils.isEmpty(dataIdPrefix)) {
                dataIdPrefix = name;
            }

            if (StringUtils.isEmpty(dataIdPrefix)) {
                dataIdPrefix = env.getProperty("spring.application.name");
            }

            PropertySource composite = new CompositePropertySource("NACOS");
            this.loadSharedConfiguration((CompositePropertySource) composite);
            this.loadExtConfiguration((CompositePropertySource) composite);
            this.loadApplicationConfiguration((CompositePropertySource) composite, dataIdPrefix, this.nacosConfigProperties, env);
            return composite;
        }
    }

    private void loadSharedConfiguration(CompositePropertySource compositePropertySource) {
        List<NacosConfigProperties.Config> sharedConfigs = this.nacosConfigProperties.getSharedConfigs();
        if (!CollectionUtils.isEmpty(sharedConfigs)) {
            this.checkConfiguration(sharedConfigs, "shared-configs");
            this.loadNacosConfiguration(compositePropertySource, sharedConfigs);
        }

    }

    private void loadExtConfiguration(CompositePropertySource compositePropertySource) {
        List<NacosConfigProperties.Config> extConfigs = this.nacosConfigProperties.getExtensionConfigs();
        if (!CollectionUtils.isEmpty(extConfigs)) {
            this.checkConfiguration(extConfigs, "extension-configs");
            this.loadNacosConfiguration(compositePropertySource, extConfigs);
        }
    }


    private void loadApplicationConfiguration(CompositePropertySource compositePropertySource, String dataIdPrefix, NacosConfigProperties properties, Environment environment) {
        String fileExtension = properties.getFileExtension();
        String nacosGroup = properties.getGroup();
        this.loadNacosDataIfPresent(compositePropertySource, dataIdPrefix, nacosGroup, fileExtension, true);
        this.loadNacosDataIfPresent(compositePropertySource, dataIdPrefix + "." + fileExtension, nacosGroup, fileExtension, true);
        String[] var7 = environment.getActiveProfiles();
        int var8 = var7.length;

        for (int var9 = 0; var9 < var8; ++var9) {
            String profile = var7[var9];
            String dataId = dataIdPrefix + "-" + profile + "." + fileExtension;
            this.loadNacosDataIfPresent(compositePropertySource, dataId, nacosGroup, fileExtension, true);
        }
    }


    private void loadNacosConfiguration(CompositePropertySource composite, List<NacosConfigProperties.Config> configs) {
        Iterator var3 = configs.iterator();
        while (var3.hasNext()) {
            NacosConfigProperties.Config config = (NacosConfigProperties.Config) var3.next();
            this.loadNacosDataIfPresent(composite, config.getDataId(), config.getGroup(), NacosDataParserHandler.getInstance().getFileExtension(config.getDataId()), config.isRefresh());
        }
    }


    private void checkConfiguration(List<NacosConfigProperties.Config> configs, String tips) {
        for (int i = 0; i < configs.size(); ++i) {
            String dataId = ((NacosConfigProperties.Config) configs.get(i)).getDataId();
            if (dataId == null || dataId.trim().length() == 0) {
                throw new IllegalStateException(String.format("the [ spring.cloud.nacos.config.%s[%s] ] must give a dataId", tips, i));
            }
        }

    }

    private void loadNacosDataIfPresent(CompositePropertySource composite, String dataId, String group, String fileExtension, boolean isRefreshable) {
        if (null != dataId && dataId.trim().length() >= 1) {
            if (null != group && group.trim().length() >= 1) {
                NacosPropertySource propertySource = this.loadNacosPropertySource(dataId, group, fileExtension, isRefreshable);
                this.addFirstPropertySource(composite, propertySource, false);
            }
        }
    }

    private NacosPropertySource loadNacosPropertySource(String dataId, String group, String fileExtension, boolean isRefreshable) {
        return NacosContextRefresher.getRefreshCount() != 0L && !isRefreshable ? NacosPropertySourceRepository.getNacosPropertySource(dataId, group) : this.nacosPropertySourceBuilder.build(dataId, group, fileExtension, isRefreshable);
    }

    private void addFirstPropertySource(CompositePropertySource composite, NacosPropertySource nacosPropertySource, boolean ignoreEmpty) {
        if (null != nacosPropertySource && null != composite) {
            if (!ignoreEmpty || !((Map) nacosPropertySource.getSource()).isEmpty()) {
                composite.addFirstPropertySource(nacosPropertySource);
            }
        }
    }

    public void setNacosConfigManager(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

}
