package com.li.zhitu.course.domain.dto;

import com.li.zhitu.course.constants.CourseErrorInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 课程id
 */
@Data
@Schema(description = "课程id")
public class CourseIdDTO {
    @Schema(description = "课程id", example = "1")
    @NotNull(message = CourseErrorInfo.Msg.COURSE_OPERATE_ID_NULL)
    private Long id;
}
