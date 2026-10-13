package com.li.zhitu.user.controller;

import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.user.domain.query.UserPageQuery;
import com.li.zhitu.user.domain.vo.TeacherPageVO;
import com.li.zhitu.user.service.ITeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师详情表 前端控制器
 */

@RestController
@RequestMapping("/teachers")
@Tag(name = "用户管理接口")
public class TeacherController {

    @Autowired
    private ITeacherService teacherService;

    @GetMapping("/page")
    @Operation(summary = "分页查询教师信息")
    public PageDTO<TeacherPageVO> queryTeacherPage(UserPageQuery pageQuery){
        return teacherService.queryTeacherPage(pageQuery);
    }
}
