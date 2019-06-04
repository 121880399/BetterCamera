package org.zzy.lib.bettercamera.config.creator;

import android.content.Context;

import org.zzy.lib.bettercamera.manager.CameraManager;
import org.zzy.lib.bettercamera.preview.CameraPreview;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public interface CameraManagerCreator {
    /**
     * 用于创建CameraManager
     */
    CameraManager create(Context context, CameraPreview cameraPreview);
}
