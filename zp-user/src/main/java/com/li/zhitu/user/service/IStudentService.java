package com.li.zhitu.user.service;

import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.user.domain.dto.StudentFormDTO;
import com.li.zhitu.user.domain.query.UserPageQuery;
import com.li.zhitu.user.domain.vo.StudentPageVo;

/**
 * <p>
 * 学员详情表 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-07-12
 */
public interface IStudentService {

    void saveStudent(StudentFormDTO studentFormDTO);

    void updateMyPassword(StudentFormDTO studentFormDTO);

    PageDTO<StudentPageVo> queryStudentPage(UserPageQuery pageQuery);
}
