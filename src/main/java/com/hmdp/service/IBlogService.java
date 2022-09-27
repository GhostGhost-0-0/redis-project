package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.service
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:15
 * @Description: 用户日记表服务类
 * @Version: 1.0
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long blogId);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long blogId);

}
