package com.hmdp.dto;

import lombok.Data;
/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.dto
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 14:55
 * @Description: 登录表单 dto
 * @Version: 1.0
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
