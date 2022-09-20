package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.utils
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:30
 * @Description: 用户日记评论表服务类
 * @Version: 1.0
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
