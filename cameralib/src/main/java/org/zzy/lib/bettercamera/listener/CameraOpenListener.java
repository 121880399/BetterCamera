package org.zzy.lib.bettercamera.listener;

import android.hardware.Camera;

import org.zzy.lib.bettercamera.constant.CameraConstant;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraOpenListener {
    /**
     * 相机打开
     */
    void onCameraOpened(@CameraConstant.Face int cameraFace);

    /**
     * 相机打开失败
     */
    void onCameraOpenError(Throwable throwable);
}
