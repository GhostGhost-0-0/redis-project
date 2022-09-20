package com.hmdp.utils;


import cn.hutool.core.util.RandomUtil;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.utils
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:20
 * @Description: 密码编码
 * @Version: 1.0
 */
public class PasswordEncoder {

    public static String encode(String password) {
        // 生成盐
        String salt = RandomUtil.randomString(20);
        // 加密
        return encode(password,salt);
    }
    private static String encode(String password, String salt) {
        // 加密
        return salt + "@" + DigestUtils.md5DigestAsHex((password + salt).getBytes(StandardCharsets.UTF_8));
    }
    public static Boolean matches(String encodedPassword, String rawPassword) {
        if (encodedPassword == null || rawPassword == null) {
            return false;
        }
        if(!encodedPassword.contains("@")){
            throw new RuntimeException("密码格式不正确！");
        }
        String[] arr = encodedPassword.split("@");
        // 获取盐
        String salt = arr[0];
        // 比较
        return encodedPassword.equals(encode(rawPassword, salt));
    }
}
