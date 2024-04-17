package com.alibaba.cloud.nacos.encryption;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.utils.AesUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class NoDeductMoneyNacosEncryption {

    private static boolean secretAvailable;
    private static String CIPHER_PREFIX;
    private static String CIPHER_SUFFIX;
    private static String CIPHER_KEY;


    public NoDeductMoneyNacosEncryption(Environment environment) {
        if (StrUtil.isNotEmpty(environment.getProperty("my.cloud.encryption.enable")) && Boolean.parseBoolean(environment.getProperty("my.cloud.encryption.enable"))) {
            String encryptKey = System.getProperty("my.cloud.encryption.key");
            String encryptPrefix = environment.getProperty("my.cloud.encryption.prefix");
            String encryptSuffix = environment.getProperty("my.cloud.encryption.suffix");
            if (StrUtil.isEmpty(encryptKey)) {
                throw new RuntimeException("解析失败");
            } else if (StrUtil.isEmpty(encryptPrefix)) {
                throw new RuntimeException("加密前缀有误");
            } else if (StrUtil.isEmpty(encryptSuffix)) {
                throw new RuntimeException("加密后缀有误");
            } else {
                secretAvailable = true;
                CIPHER_PREFIX = encryptPrefix;
                CIPHER_SUFFIX = encryptSuffix;
                CIPHER_KEY = encryptKey;
            }
        } else {
            secretAvailable = false;
        }
    }

    public Boolean checkProcess() {
        return secretAvailable;
    }

    public String process(String source) {
        while(source.contains(CIPHER_PREFIX)) {
            int startIndex = source.indexOf(CIPHER_PREFIX);
            int endIndex = source.indexOf(CIPHER_SUFFIX);
            if (startIndex > endIndex) {
                throw new RuntimeException("ovse cipher config end cannot before start: " + source);
            }

            String cipher = source.substring(startIndex + CIPHER_PREFIX.length(), endIndex);
            String plain = this.cipher2Plain(cipher);
            source = source.substring(0, startIndex) + plain + source.substring(endIndex + CIPHER_SUFFIX.length());
        }

        return source;
    }

    private String cipher2Plain(String cipher) {
        try {
            return AesUtils.decrypt(cipher, CIPHER_KEY);
        } catch (Exception var3) {
            throw new RuntimeException("ovse cipher config format error", var3);
        }
    }

}
