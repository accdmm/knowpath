package com.li.zhitu.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.learning.domain.po.PointsBoard;
import com.li.zhitu.learning.domain.query.PointsBoardQuery;
import com.li.zhitu.learning.domain.vo.PointsBoardVO;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author 李梦豪
 */
public interface IPointsBoardService extends IService<PointsBoard> {
    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    void createPointsBoardTableBySeason(Integer season);

    List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize);
}
