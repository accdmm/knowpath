package com.li.zhitu.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.learning.domain.po.PointsBoardSeason;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 李梦豪
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    Integer querySeasonByTime(LocalDateTime time);
}
