package org.zzy.lib.bettercamera.listener;

import java.io.File;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraVideoListener {
    /**
     * 视频开始录制
     */
    void onVideoRecordStart();

    /**
     * 视频停止录制
     */
    void onVideoRecordStop(File file);

    /**
     * 视频录制失败
     */
    void onVideoRecordError(Throwable throwable);
}
