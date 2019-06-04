package org.zzy.lib.bettercamera.config.creator.impl;

import android.content.Context;
import android.os.Build;

import org.zzy.lib.bettercamera.config.creator.CameraManagerCreator;
import org.zzy.lib.bettercamera.manager.CameraManager;
import org.zzy.lib.bettercamera.manager.impl.Camera1Manager;
import org.zzy.lib.bettercamera.manager.impl.Camera2Manager;
import org.zzy.lib.bettercamera.preview.CameraPreview;
import org.zzy.lib.bettercamera.utils.CameraHelper;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/4
 */
public class CameraManagerCreatorImpl implements CameraManagerCreator {


    /**
     * 目前的策略是大于5.0版本，并且Camera2可用使用的情况下，使用Camera2，否则使用Camera1
     * 以后大于5.0版本会改为使用谷歌新出的CameraX
     */
    @Override
    public CameraManager create(Context context, CameraPreview cameraPreview) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CameraHelper.hasCamera2(context)) {
            return new Camera2Manager(cameraPreview);
        }
        return new Camera1Manager(cameraPreview);
    }
}
