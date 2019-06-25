package org.zzy.lib.bettercamera;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.zzy.lib.bettercamera.bean.AspectRatio;
import org.zzy.lib.bettercamera.bean.Size;
import org.zzy.lib.bettercamera.bean.SizeMap;
import org.zzy.lib.bettercamera.config.ConfigProvider;
import org.zzy.lib.bettercamera.constant.CameraConstant;
import org.zzy.lib.bettercamera.constant.MediaConstant;
import org.zzy.lib.bettercamera.constant.PreviewConstant;
import org.zzy.lib.bettercamera.listener.CameraCloseListener;
import org.zzy.lib.bettercamera.listener.CameraOpenListener;
import org.zzy.lib.bettercamera.listener.CameraPhotoListener;
import org.zzy.lib.bettercamera.listener.CameraSizeListener;
import org.zzy.lib.bettercamera.listener.CameraVideoListener;
import org.zzy.lib.bettercamera.listener.OnMoveListener;
import org.zzy.lib.bettercamera.manager.CameraManager;
import org.zzy.lib.bettercamera.preview.CameraPreview;
import org.zzy.lib.bettercamera.widget.DisplayOrientationDetector;
import org.zzy.lib.bettercamera.widget.FocusMarkerLayout;

import java.io.File;

/**
 * @作者 ZhouZhengyi
 * @创建日期 2019/6/25
 */
public class CameraView extends FrameLayout {

    private static final String TAG = "CameraView";

    private CameraManager cameraManager;

    private CameraPreview cameraPreview;

    private AspectRatio aspectRatio;

    private FocusMarkerLayout focusMarkerLayout;

    private DisplayOrientationDetector displayOrientationDetector;
    @PreviewConstant.AdjustType
    private int adjustType = PreviewConstant.NONE;

    private boolean clipScreen;

    private boolean adjustViewBounds;

    public CameraView(@NonNull Context context) {
        super(context);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCameraView(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initCameraView(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int
            defStyleRes) {
        cameraPreview = ConfigProvider.getInstance().getCameraPreviewCreator().create(getContext(), this);
        cameraManager = ConfigProvider.getInstance().getCameraManagerCreator().create(context, cameraPreview);
        cameraManager.initialize(context);
        cameraManager.addCameraSizeListener(new CameraSizeListener() {
            @Override
            public void onPreviewSizeUpdated(Size previewSize) {
                aspectRatio = cameraManager.getAspectRatio();
                if (displayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
                    aspectRatio = aspectRatio.inverse();
                }
                requestLayout();
            }

            @Override
            public void onVideoSizeUpdated(Size videoSize) {
            }

            @Override
            public void onPictureSizeUpdated(Size pictureSize) {
            }
        });
        focusMarkerLayout = new FocusMarkerLayout(context);
        focusMarkerLayout.setCameraView(this);
        focusMarkerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.addView(focusMarkerLayout);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                R.style.Widget_CameraView);
        adjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        cameraManager.switchCamera(a.getInt(R.styleable.CameraView_cameraFace, CameraConstant.FACE_REAR));
        cameraManager.setMediaType(a.getInt(R.styleable.CameraView_mediaType, MediaConstant.TYPE_PICTURE));
        cameraManager.setVoiceEnable(a.getBoolean(R.styleable.CameraView_voiceEnable, true));
        String strAspectRatio = a.getString(R.styleable.CameraView_aspectRatio);
        aspectRatio = TextUtils.isEmpty(strAspectRatio) ?
                      ConfigProvider.getInstance().getDefaultAspectRatio() : AspectRatio.parse(strAspectRatio);
        cameraManager.setExpectAspectRatio(aspectRatio);
        cameraManager.setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true));
        cameraManager.setFlashMode(a.getInt(R.styleable.CameraView_flash, CameraConstant.FLASH_AUTO));
        String zoomString = a.getString(R.styleable.CameraView_zoom);
        if (!TextUtils.isEmpty(zoomString)) {
            try {
                setZoom(Float.valueOf(zoomString));
            } catch (NumberFormatException e) {
                setZoom(1.0f);
            }
        } else {
            setZoom(1.0f);
        }
        clipScreen = a.getBoolean(R.styleable.CameraView_clipScreen, false);
        adjustType = a.getInt(R.styleable.CameraView_cameraAdjustType, adjustType);
        focusMarkerLayout.setScaleRate(a.getInt(R.styleable.CameraView_scaleRate, FocusMarkerLayout.DEFAULT_SCALE_RATE));
        focusMarkerLayout.setTouchZoomEnable(a.getBoolean(R.styleable.CameraView_touchRoom, true));
        focusMarkerLayout.setUseTouchFocus(a.getBoolean(R.styleable.CameraView_touchFocus, true));
        a.recycle();
        displayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                cameraManager.setDisplayOrientation(displayOrientation);
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            displayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            displayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (clipScreen) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            switch (adjustType) {
                case PreviewConstant.WIDTH_FIRST:
                    height = width * aspectRatio.heightRatio / aspectRatio.widthRatio;
                    break;
                case PreviewConstant.HEIGHT_FIRST:
                    width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    break;
                case PreviewConstant.SMALLER_FIRST:
                    if (width * aspectRatio.heightRatio < height * aspectRatio.widthRatio) {
                        height = width * aspectRatio.heightRatio / aspectRatio.widthRatio;
                    } else {
                        width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    }
                    break;
                case PreviewConstant.LARGER_FIRST:
                    if (width * aspectRatio.heightRatio < height * aspectRatio.widthRatio) {
                        width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    } else {
                        height = width * aspectRatio.heightRatio / aspectRatio.widthRatio;
                    }
                    break;
                case PreviewConstant.NONE:
                default:
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            return;
        }

        if (adjustViewBounds) {
            if (!isCameraOpened()) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final AspectRatio ratio = aspectRatio;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.ratio());
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * aspectRatio.ratio());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        // Always smaller first! But use the effect to the CameraPreview instead of the CameraView.
        if (height < width * aspectRatio.heightRatio / aspectRatio.widthRatio) {
            cameraPreview.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * aspectRatio.heightRatio / aspectRatio.widthRatio,
                            MeasureSpec.EXACTLY));
        } else {
            cameraPreview.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * aspectRatio.widthRatio / aspectRatio.heightRatio,
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    public void openCamera(CameraOpenListener cameraOpenListener) {
        cameraManager.openCamera(cameraOpenListener);
    }

    public boolean isCameraOpened() {
        return cameraManager.isCameraOpened();
    }

    @CameraConstant.Face
    public int getCameraFace() {
        return cameraManager.getCameraFace();
    }

    public void switchCamera(@CameraConstant.Face int cameraFace) {
        cameraManager.switchCamera(cameraFace);
    }

    public void setMediaType(@MediaConstant.Type int mediaType) {
        cameraManager.setMediaType(mediaType);
    }

    public void setVoiceEnable(boolean voiceEnable) {
        cameraManager.setVoiceEnable(voiceEnable);
    }

    public boolean isVoiceEnable() {
        return cameraManager.isVoiceEnable();
    }

    public void setAutoFocus(boolean autoFocus) {
        cameraManager.setAutoFocus(autoFocus);
    }

    public boolean isAutoFocus() {
        return cameraManager.isAutoFocus();
    }

    public void setFlashMode(@CameraConstant.FlashMode int flashMode) {
        cameraManager.setFlashMode(flashMode);
    }

    @CameraConstant.FlashMode
    public int getFlashMode() {
        return cameraManager.getFlashMode();
    }

    public void setZoom(float zoom) {
        cameraManager.setZoom(zoom);
    }

    public float getZoom() {
        return cameraManager.getZoom();
    }

    public float getMaxZoom() {
        return cameraManager.getMaxZoom();
    }

    public void setExpectSize(Size expectSize) {
        cameraManager.setExpectSize(expectSize);
    }

    public void setExpectAspectRatio(AspectRatio aspectRatio) {
        cameraManager.setExpectAspectRatio(aspectRatio);
    }

    public Size getSize(@CameraConstant.SizeFor int sizeFor) {
        return cameraManager.getSize(sizeFor);
    }

    public SizeMap getSizes(@CameraConstant.SizeFor int sizeFor) {
        return cameraManager.getSizes(sizeFor);
    }

    public AspectRatio getAspectRatio() {
        return cameraManager.getAspectRatio();
    }

    public void addCameraSizeListener(CameraSizeListener cameraSizeListener) {
        cameraManager.addCameraSizeListener(cameraSizeListener);
    }

    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        cameraManager.takePicture(cameraPhotoListener);
    }

    public void setVideoFileSize(long videoFileSize) {
        cameraManager.setVideoFileSize(videoFileSize);
    }

    public void setVideoDuration(int videoDuration) {
        cameraManager.setVideoDuration(videoDuration);
    }

    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        cameraManager.startVideoRecord(file, cameraVideoListener);
    }

    public void stopVideoRecord() {
        cameraManager.stopVideoRecord();
    }

    public void resumePreview() {
        cameraManager.resumePreview();
    }

    public void closeCamera(CameraCloseListener cameraCloseListener) {
        cameraManager.closeCamera(cameraCloseListener);
    }

    public void releaseCamera() {
        cameraManager.releaseCamera();
    }

    public void setOnMoveListener(OnMoveListener onMoveListener) {
        focusMarkerLayout.setOnMoveListener(onMoveListener);
    }

    public void setTouchAngle(int touchAngle) {
        if (focusMarkerLayout != null) {
            focusMarkerLayout.setTouchAngle(touchAngle);
        }
    }

    public void setScaleRate(int scaleRate) {
        focusMarkerLayout.setScaleRate(scaleRate);
    }

    public void setTouchZoomEnable(boolean touchZoomEnable) {
        focusMarkerLayout.setTouchZoomEnable(touchZoomEnable);
    }

    public void setUseTouchFocus(boolean useTouchFocus) {
        focusMarkerLayout.setUseTouchFocus(useTouchFocus);
    }

}
