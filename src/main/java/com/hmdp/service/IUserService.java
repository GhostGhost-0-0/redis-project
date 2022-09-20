package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.service
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:15
 * @Description: 用户表服务类
 * @Version: 1.0
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm);
}
