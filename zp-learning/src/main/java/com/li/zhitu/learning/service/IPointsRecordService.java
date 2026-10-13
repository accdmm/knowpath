package com.li.zhitu.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.learning.domain.po.PointsRecord;
import com.li.zhitu.learning.domain.vo.PointsStatisticsVO;
import com.li.zhitu.learning.enums.PointsRecordType;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author 李梦豪
 */
public interface IPointsRecordService extends IService<PointsRecord> {
    void addPointsRecord(Long userId, int points, PointsRecordType type);

    List<PointsStatisticsVO> queryMyPointsToday();

}
