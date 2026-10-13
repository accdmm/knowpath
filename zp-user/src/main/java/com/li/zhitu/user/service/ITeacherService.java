package com.li.zhitu.user.service;

import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.user.domain.query.UserPageQuery;
import com.li.zhitu.user.domain.vo.TeacherPageVO;

/**
 * <p>
 * 教师详情表 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-07-12
 */
public interface ITeacherService{
    PageDTO<TeacherPageVO> queryTeacherPage(UserPageQuery pageQuery);

}
