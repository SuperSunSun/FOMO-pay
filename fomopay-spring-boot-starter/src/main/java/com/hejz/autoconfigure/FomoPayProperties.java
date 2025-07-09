package com.hejz.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "fomopay")
@Data
@Component
public class FomoPayProperties {
    private String apiUrl;
    private String keyId;
    private String tid;
    private String mid;
    private String privateKeyPath = "private_key.pem";  // 默认值
    private String publicKeyPath = "public_key.pem";    // 默认值
    
    // 手动添加getter方法以确保编译通过
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getKeyId() {
        return keyId;
    }
    
    public String getTid() {
        return tid;
    }
    
    public String getMid() {
        return mid;
    }
    
    public String getPrivateKeyPath() {
        return privateKeyPath;
    }
    
    public String getPublicKeyPath() {
        return publicKeyPath;
    }
    
    // 手动添加setter方法
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
    
    public void setTid(String tid) {
        this.tid = tid;
    }
    
    public void setMid(String mid) {
        this.mid = mid;
    }
    
    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }
    
    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }
}
