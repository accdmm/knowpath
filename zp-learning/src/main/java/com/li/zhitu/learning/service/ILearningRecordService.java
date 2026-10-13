package com.li.zhitu.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.api.dto.leanring.LearningLessonDTO;
import com.li.zhitu.learning.domain.dto.LearningRecordFormDTO;
import com.li.zhitu.learning.domain.po.LearningRecord;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-12-10
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    void addLearningRecord(LearningRecordFormDTO formDTO);
}
