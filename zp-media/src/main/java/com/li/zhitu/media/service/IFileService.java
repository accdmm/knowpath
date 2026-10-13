package com.li.zhitu.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.media.domain.dto.FileDTO;
import com.li.zhitu.media.domain.po.File;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 文件表，可以是普通文件、图片等 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-06-30
 */
public interface IFileService extends IService<File> {

    FileDTO uploadFile(MultipartFile file);

    FileDTO getFileInfo(Long id);
}
