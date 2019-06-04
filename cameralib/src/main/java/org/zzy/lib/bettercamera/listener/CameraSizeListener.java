package org.zzy.lib.bettercamera.listener;

import org.zzy.lib.bettercamera.bean.Size;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraSizeListener {

    /**
     * 预览尺寸更新
     * @param previewSize 预览尺寸
     */
    void onPreviewSizeUpdated(Size previewSize);

    /**
     * 视频尺寸更新
     * @param videoSize 视频尺寸
     */
    void onVideoSizeUpdated(Size videoSize);

    /**
     * 照片尺寸更新
     * @param pictureSize 照片尺寸
     */
    void onPictureSizeUpdated(Size pictureSize);

}
