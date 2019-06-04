package org.zzy.lib.bettercamera.listener;

import android.hardware.Camera;

import org.zzy.lib.bettercamera.constant.CameraConstant;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraCloseListener {
    /**
     * 相机关闭
     */
    void onCameraClosed(@CameraConstant.Face int cameraFace);
}
