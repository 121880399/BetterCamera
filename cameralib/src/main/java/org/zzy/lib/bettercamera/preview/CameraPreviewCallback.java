package org.zzy.lib.bettercamera.preview;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraPreviewCallback {

    /**
     * 当预览可用的时候回调
     */
    void onAvailable(CameraPreview cameraPreview);
}
