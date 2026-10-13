package com.li.zhitu.search.service;

import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.search.domain.query.CoursePageQuery;
import com.li.zhitu.search.domain.vo.CourseVO;

import java.util.List;

public interface ISearchService {

    List<CourseVO> queryCourseByCateId(Long cateLv2Id);

    List<CourseVO> queryBestTopN();

    List<CourseVO> queryNewTopN();

    List<CourseVO> queryFreeTopN();

    PageDTO<CourseVO> queryCoursesForPortal(CoursePageQuery query);

    List<Long> queryCoursesIdByName(String keyword);
}
