package com.li.zhitu.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.media.domain.dto.MediaDTO;
import com.li.zhitu.media.domain.dto.MediaUploadResultDTO;
import com.li.zhitu.media.domain.po.Media;
import com.li.zhitu.media.domain.query.MediaQuery;
import com.li.zhitu.media.domain.vo.MediaVO;
import com.li.zhitu.media.domain.vo.VideoPlayVO;

/**
 * <p>
 * 媒资表，主要是视频文件 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-06-30
 */
public interface IMediaService extends IService<Media> {

    String getUploadSignature();

    VideoPlayVO getPlaySignatureBySectionId(Long fileId);

    MediaDTO save(MediaUploadResultDTO mediaResult);

    void updateMediaProcedureResult(Media media);

    void deleteMedia(String fileId);

    VideoPlayVO getPlaySignatureByMediaId(Long mediaId);

    PageDTO<MediaVO> queryMediaPage(MediaQuery query);
}
