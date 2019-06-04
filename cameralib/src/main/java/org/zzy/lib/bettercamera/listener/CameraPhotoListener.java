package org.zzy.lib.bettercamera.listener;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraPhotoListener {

    /**
     * 得到照片
     * @param data 照片数据
     */
    void onPictureTaken(byte[] data);

    /**
     * 照片获取失败
     * @param throwable 异常信息
     */
    void onCaptureFailed(Throwable throwable);
}
