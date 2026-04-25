package com.example.mall.module.upload.controller;

import com.example.mall.common.Result;
import com.example.mall.config.QiniuConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 *
 * <p>前端直传七牛云方案：后端仅签发 uploadToken，文件由前端直传七牛云，
 * 减少后端带宽压力。上传成功后前端将返回的 key 提交给业务接口。
 *
 * @see QiniuConfig
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final QiniuConfig qiniuConfig;

    /**
     * Token 有效期：10 分钟
     */
    private static final long TOKEN_EXPIRES = 10 * 60;

    /**
     * 商品图片大小限制：5MB
     */
    private static final long PRODUCT_MAX_SIZE = 5 * 1024 * 1024;

    /**
     * 头像大小限制：2MB
     */
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024;

    /**
     * 允许的图片 MIME 类型
     */
    private static final String ALLOWED_IMAGE_MIME = "image/jpeg;image/png;image/webp";

    /**
     * 获取七牛云上传 Token
     *
     * <p>根据 type 参数返回不同限制的上传凭证：
     * <ul>
     *   <li>product：商品图片，最大 5MB</li>
     *   <li>avatar：用户头像，最大 2MB</li>
     * </ul>
     *
     * @param type 上传类型，可选值为 product / avatar，默认 product
     * @return 包含 token、key、domain 的 Map
     */
    @GetMapping("/token")
    public Result<Map<String, String>> getUploadToken(
            @RequestParam(defaultValue = "product") String type) {

        UploadPolicy policy = resolvePolicy(type);
        // 生成 UUID 文件名，保留原始文件扩展名由七牛自动处理
        // 这里不强制指定 key，由七牛自动生成
        String key = null;

        String token = qiniuConfig.getUploadToken(key, TOKEN_EXPIRES, policy.mimeLimit, policy.maxSize);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("key", key);
        result.put("domain", qiniuConfig.getDomain());

        return Result.success(result);
    }

    /**
     * 根据 type 解析上传策略
     */
    private UploadPolicy resolvePolicy(String type) {
        return switch (type) {
            case "avatar" -> new UploadPolicy(AVATAR_MAX_SIZE, ALLOWED_IMAGE_MIME);
            case "product" -> new UploadPolicy(PRODUCT_MAX_SIZE, ALLOWED_IMAGE_MIME);
            default -> throw new IllegalArgumentException("Unsupported upload type: " + type);
        };
    }

    /**
     * 上传策略内部类
     */
    private record UploadPolicy(long maxSize, String mimeLimit) {
    }
}
