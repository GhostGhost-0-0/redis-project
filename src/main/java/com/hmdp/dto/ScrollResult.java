package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * @BelongProject: hm-dianping
 * @BelongPackage: com.hmdp.dto
 * @Author: 那个小楠瓜
 * @CreateTime: 2022-09-19 21:20
 * @Description: 滚动分页结果集
 * @Version: 1.0
 */
@Data
public class ScrollResult {

    /**
     * 小于指定时间戳的集合
     */
    private List<?> list;

    /**
     * 本次查询的推送的最小时间戳
     */
    private Long minTime;
    /**
     * 偏移量
     */
    private Integer offset;
}
