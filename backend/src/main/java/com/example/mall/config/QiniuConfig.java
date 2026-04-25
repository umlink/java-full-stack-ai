package com.example.mall.config;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 七牛云对象存储配置
 *
 * <p>提供上传 Token 签发功能，支持自定义上传策略：
 * <ul>
 *   <li>文件大小限制（fsizeLimit）</li>
 *   <li>MIME 类型限制（mimeLimit）</li>
 *   <li>Token 有效期（expires）</li>
 *   <li>回调 URL（callbackUrl，可选）</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "qiniu")
@Data
public class QiniuConfig {

    private String accessKey;
    private String secretKey;
    private String bucket;
    private String domain;

    /**
     * 生成七牛云上传 Token
     *
     * @param key            上传文件名（可为空，为空则由七牛自动生成）
     * @param expiresSeconds Token 有效期（秒）
     * @param mimeLimit      允许的 MIME 类型，多个用分号分隔，如 "image/jpeg;image/png;image/webp"
     * @param maxSize        文件大小上限（字节）
     * @return 上传 Token
     */
    public String getUploadToken(String key, long expiresSeconds, String mimeLimit, long maxSize) {
        Auth auth = Auth.create(accessKey, secretKey);

        StringMap putPolicy = new StringMap();
        // 文件大小限制
        putPolicy.put("fsizeLimit", maxSize);
        // MIME 类型限制
        if (mimeLimit != null && !mimeLimit.isEmpty()) {
            putPolicy.put("mimeLimit", mimeLimit);
        }
        // 非强制指定 key，允许前端上传时自定义文件名
        // 若 key 为空，七牛将自动生成为文件 hash

        // 生成上传 Token
        return auth.uploadToken(bucket, key, expiresSeconds, putPolicy);
    }

    /**
     * 生成七牛云上传 Token（不指定 key，由七牛自动生成文件名）
     *
     * @param expiresSeconds Token 有效期（秒）
     * @param mimeLimit      允许的 MIME 类型
     * @param maxSize        文件大小上限（字节）
     * @return 上传 Token
     */
    public String getUploadToken(long expiresSeconds, String mimeLimit, long maxSize) {
        return getUploadToken(null, expiresSeconds, mimeLimit, maxSize);
    }
}
