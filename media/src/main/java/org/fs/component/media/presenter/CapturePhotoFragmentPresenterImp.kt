/*
 * Media Component Copyright (C) 2018 Fatih.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fs.component.media.presenter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.BusManager
import org.fs.architecture.common.ThreadManager
import org.fs.architecture.common.scope.ForFragment
import org.fs.component.gallery.model.entity.Media
import org.fs.component.gallery.model.event.NextSelectedEvent
import org.fs.component.gallery.model.event.SelectionEvent
import org.fs.component.gallery.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.media.common.*
import org.fs.component.media.common.annotation.Direction
import org.fs.component.media.common.annotation.FlashMode
import org.fs.component.media.common.annotation.State
import org.fs.component.media.util.*
import org.fs.component.media.util.C.Companion.FLASH_MODE_AUTO
import org.fs.component.media.util.C.Companion.FLASH_MODE_DISABLED
import org.fs.component.media.util.C.Companion.STATE_PICTURE_TAKEN
import org.fs.component.media.util.C.Companion.STATE_PREVIEW
import org.fs.component.media.util.C.Companion.STATE_WAITING_LOCK
import org.fs.component.media.util.C.Companion.STATE_WAITING_NON_PRE_CAPTURE
import org.fs.component.media.util.C.Companion.STATE_WAITING_PRE_CAPTURE
import org.fs.component.media.view.CapturePhotoFragmentView
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ForFragment
class CapturePhotoFragmentPresenterImp @Inject constructor(
    view: CapturePhotoFragmentView): AbstractPresenter<CapturePhotoFragmentView>(view), CapturePhotoFragmentPresenter {

  companion object {
    private val ORIENTATIONS = SparseIntArray().apply {
      append(Surface.ROTATION_0, 90)
      append(Surface.ROTATION_90, 0)
      append(Surface.ROTATION_180, 270)
      append(Surface.ROTATION_270, 180)
    }

    private const val CAMERA_CAPTURE_PHOTO_THREAD_NAME = "CapturePhotoBackgroundThread"

    private const val CAMERA_LOCK_TIMEOUT = 2500L
    private const val FILE_SUFFIX = ".jpeg"

    private const val MAX_IMAGE_COUNT = 2

    private const val REQUEST_CAMERA_PERMISSION_CODE = 0x99
  }

  /** surfaceTextureListener **/
  private val whenAvailable: (width: Int, height: Int) -> Unit = { width, height ->
    openCamera(width, height)
  }

  private val whenSizeChanged: (width: Int, height: Int) -> Unit = { width, height ->
    configureTransform(width, height)
  }

  private val surfaceTextureListener = SimpleSurfaceTextureListener(whenAvailable, whenSizeChanged)
  /** surfaceTextureListener **/

  /** cameraStateListener **/
  private val whenOpened: (camera: CameraDevice?) -> Unit = { camera ->
    cameraOpenCloseLock.release()
    this.camera = camera
    createCameraPreviewSession()
  }

  private val whenDisconnected: (camera: CameraDevice?) -> Unit = { camera ->
    cameraOpenCloseLock.release()
    this.camera = null
    camera?.close()
  }

  private val whenError: (camera: CameraDevice?, error: Int) -> Unit = { camera, _ ->
    whenDisconnected(camera)
  }

  private val cameraStateListener = SimpleCameraDeviceStateListener(whenOpened, whenDisconnected, whenError)
  /** cameraStateListener **/

  /** captureSessionListener **/
  private val whenProcess: (result: CaptureResult) -> Unit = { result ->
    when(state) {
      STATE_PREVIEW -> Unit
      STATE_WAITING_LOCK -> whenCapturePicture(result)
      STATE_WAITING_PRE_CAPTURE -> {
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        val hasAeStateWaiting = aeState == null
            || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
            || aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
        if (hasAeStateWaiting) {
          state = STATE_WAITING_NON_PRE_CAPTURE
        }
      }
      STATE_WAITING_NON_PRE_CAPTURE -> {
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        val hasStateMature = aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE
        if (hasStateMature) {
          state = STATE_PICTURE_TAKEN
          captureStillPicture()
        }
      }
    }
  }

  private val whenCapturePicture: (result: CaptureResult) -> Unit = { result ->
    // clear anything new in here
    val files = directory.listFiles()
    if (files.isNotEmpty()) {
      files.forEach { f -> f.delete() }
    }

    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
    if (afState == null) {
      captureStillPicture()
    } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
        || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
      val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
      if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
        state = C.STATE_PICTURE_TAKEN
        captureStillPicture()
      } else {
        runPreCaptureSequence()
      }
    }
  }

  private val cameraCaptureListener = SimpleCaptureSessionCaptureListener(whenProcess)
  /** captureSessionListener **/

  private val captureStillPicture: () -> Unit = {
    val activity = view.activity()
    camera?.let { device ->
      val rotation = activity.windowManager.defaultDisplay.rotation

      val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
        imageReader?.surface?.let { surface ->
          addTarget(surface)
          set(CaptureRequest.JPEG_ORIENTATION, (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360)
          set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
          autoFlash(this)
        }
      }

      val listener = SimpleCaptureSessionCaptureListener { result ->
        if (result is TotalCaptureResult) unlockFocus() else Unit

        val files = directory.listFiles()
        if (files.isNotEmpty()) {
          val captured = files.firstOrNull()
          if (captured != null) {
            ThreadManager.runOnUiThread(Runnable {
              if (view.isAvailable()) {
                view.bindPreview(captured)
              }
            })
          }
        }
      }
      captureSession?.apply {
        stopRepeating()
        abortCaptures()
        capture(captureBuilder.build(), listener, null)
      }
    }
  }

  private val runPreCaptureSequence: () -> Unit = {
    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
    state = C.STATE_WAITING_PRE_CAPTURE
    captureSession?.capture(previewRequestBuilder.build(), cameraCaptureListener, backgroundHandler)
  }

  private val autoFlash: (requestBuilder: CaptureRequest.Builder) -> Unit = { requestBuilder ->
    if (flashSupported) {
      requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
    }
  }

  private val disableFlash: (requestBuilder: CaptureRequest.Builder) -> Unit = { requestBuilder ->
    if (flashSupported) {
      requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
    }
  }

  private val unlockFocus: () -> Unit = {
    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
    autoFlash(previewRequestBuilder)
    captureSession?.capture(previewRequestBuilder.build(), cameraCaptureListener, backgroundHandler)
    state = C.STATE_PREVIEW
    captureSession?.setRepeatingRequest(previewRequest, cameraCaptureListener, backgroundHandler)
  }

  @SuppressLint("MissingPermission")
  private val openCamera: (width: Int, height: Int) -> Unit = { width, height->
    if (checkIfPermissionGranted()) {

      setUpCameraOutputs(width, height)
      whenSizeChanged(width, height) // configureTransform

      view.getContext()?.let { ctx ->

        val manager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (!cameraOpenCloseLock.tryAcquire(CAMERA_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
          throw RuntimeException("Timeout exception since we can not acquire camera within 2500 ms")
        }
        manager.openCamera(cameraId, cameraStateListener, backgroundHandler)
      }
    } else {
      view.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION_CODE)
    }
  }

  private val configureTransform: (width: Int, height: Int) -> Unit = { width, height ->
    val activity = view.activity()
    // transform view and size dimensions here
    val rotation = activity.windowManager.defaultDisplay.rotation
    val matrix = Matrix()
    val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    val bufferRect = RectF(0f, 0f, previewSize.width.toFloat(), previewSize.height.toFloat())
    val cx = viewRect.centerX()
    val cy = viewRect.centerY()
    // check if rotation is valid else transform it with matrix
    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
      bufferRect.offset(cx - bufferRect.centerX(), cy - bufferRect.centerY())
      val scale = Math.max(height.toFloat() / previewSize.width, width.toFloat() / previewSize.width)
      // apply matrix params
      matrix.apply {
        setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
        postScale(scale, scale, cx, cy)
        postRotate((90 * (rotation - 2)).toFloat(), cx, cy)
      }
    } else if (Surface.ROTATION_180 == rotation) {
      matrix.postRotate(180f, cx, cy)
    }
    view.transformTextureView(matrix)
  }

  private val closeCamera: () -> Unit = {
    cameraOpenCloseLock.acquire()
    captureSession?.close()
    captureSession = null
    camera?.close()
    camera = null
    imageReader?.close()
    imageReader = null
    cameraOpenCloseLock.release()
  }

  private val startBackground: () -> Unit = {
    backgroundThread = HandlerThread(CAMERA_CAPTURE_PHOTO_THREAD_NAME).also(Thread::start)
    backgroundHandler = Handler(backgroundThread?.looper)
  }

  private val stopBackground: () -> Unit = {
    backgroundThread?.quitSafely()
    backgroundThread?.join()
    backgroundThread = null
    backgroundHandler = null
  }

  private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
    backgroundHandler?.post(ImageSaveTask.newTask(reader.acquireLatestImage(), file))
  }

  private val setUpCameraOutputs: (width: Int, height: Int) -> Unit = { width, height ->
    view.getContext()?.let { ctx ->
      val manager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
      manager.cameraIdList.forEach { cameraId ->
        val characteristics = manager.getCameraCharacteristics(cameraId)

        val direction = characteristics.get(CameraCharacteristics.LENS_FACING)
        if (direction != null && direction == cameraDirection) {

          val maybeMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
          maybeMap?.let { map ->

            val selfie = cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
            val largest = chooseImageSize(map.getOutputSizes(ImageReader::class.java), width, height, selfie)
            imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, MAX_IMAGE_COUNT).apply {
              setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
            }

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, largest, selfie)

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

            flashSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

            this.cameraId = cameraId
          }
        }
      }
    }
  }

  private val createCameraPreviewSession: () -> Unit = {
    val surfaceTexture = view.surfaceTexture()

    val selfie = cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
    if (selfie) {
      surfaceTexture.setDefaultBufferSize(previewSize.height, previewSize.width)
    } else {
      surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
    }

    val surface = Surface(surfaceTexture)

    camera?.let { device ->

      previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
      previewRequestBuilder.addTarget(surface)

      device.createCaptureSession(listOf(surface, imageReader?.surface), SimpleCaptureSessionStateListener { session ->
        captureSession = session

        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        autoFlash(previewRequestBuilder)

        previewRequest = previewRequestBuilder.build()
        captureSession?.setRepeatingRequest(previewRequest, cameraCaptureListener, backgroundHandler)
      }, null)
    }
  }

  private val chooseOptimalSize: (choices: Array<Size>, width: Int, height: Int, ratio: Size, selfie: Boolean) -> Size = { choices, width, height, _, selfie ->
    val bigEnough = when {
      selfie -> choices.filter { optimal -> optimal.height >= height }
      else -> choices.filter { optimal -> optimal.width >= width }
    }

    when {
      selfie -> when {
        bigEnough.isNotEmpty() -> bigEnough.min(CompareSizesByHeight.BY_HEIGHT_COMPARATOR) ?: choices.first()
        else -> choices.first()
      }
      else -> when {
        bigEnough.isNotEmpty() -> bigEnough.min(CompareSizesByWidth.BY_WIDTH_COMPARATOR) ?: choices.first()
        else -> choices.first()
      }
    }
  }

  private val chooseImageSize: (choices: Array<Size>, width: Int, height: Int, selfie: Boolean) -> Size = { choices, width, height, selfie ->
    when {
      selfie -> choices.filter { item -> item.height >= height }.min(CompareSizesByHeight.BY_HEIGHT_COMPARATOR) ?: choices.first()
      else -> choices.filter { item -> item.width >= width }.min(CompareSizesByWidth.BY_WIDTH_COMPARATOR) ?: choices.first()
    }
  }

  // when taking picture lock focus
  private val lockFocus: () -> Unit = {
    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
    state = C.STATE_WAITING_LOCK
    captureSession?.capture(previewRequestBuilder.build(), cameraCaptureListener, backgroundHandler)
  }

  private val checkIfPermissionGranted: () -> Boolean = {
    view.getContext()?.let { ctx ->
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    } ?: false
  }

  @FlashMode private var flash = FLASH_MODE_DISABLED
  @State private var state = STATE_PREVIEW
  @Direction private var cameraDirection = CameraCharacteristics.LENS_FACING_BACK
  private var flashSupported = false
  private var cameraOpenCloseLock = Semaphore(1)
  private var sensorOrientation = 0

  private var camera: CameraDevice? = null
  private var imageReader: ImageReader? = null
  private var captureSession: CameraCaptureSession? = null
  private var backgroundThread: HandlerThread? = null
  private var backgroundHandler: Handler? = null

  private val directory by lazy { File(view.getContext()?.filesDir, "img_captures") }

  private lateinit var previewRequestBuilder: CaptureRequest.Builder
  private lateinit var previewRequest: CaptureRequest
  private lateinit var cameraId: String
  private lateinit var previewSize: Size

  private val file get() = File(directory,"${System.currentTimeMillis()}_IMG$FILE_SUFFIX")

  private val disposeBag by lazy { CompositeDisposable() }

  override fun onCreate() {
    if (view.isAvailable()) {
      view.setUp()
      // we will create this for user storage to execute video and other components
      if (!directory.exists()) {
        directory.mkdirs()
      } else {
        // get rid of previous stores
        val files = directory.listFiles()
        if (files.isNotEmpty()) {
          files.forEach { f -> f.delete() }
        }
      }
    }
  }

  override fun onStart() {
    if (view.isAvailable()) {
      disposeBag += BusManager.add(Consumer { evt -> when(evt) {
        is NextSelectedEvent -> {
            val files = directory.listFiles()
            if (files.isNotEmpty()) {
              val taken = files.firstOrNull()
              if (taken != null) {
                BusManager.send(SelectionEvent(Media(MEDIA_TYPE_IMAGE, taken, Date().time, taken.name, Uri.EMPTY, "image/jpeg")))
              }
            }
          }
        }
      })
      // will take capture here
      disposeBag += view.observeCapture()
        .subscribe { lockFocus() }
      // will configure changes
      disposeBag += view.observeChangeCamera()
        .map {
          when(cameraDirection) {
            CameraCharacteristics.LENS_FACING_BACK -> CameraCharacteristics.LENS_FACING_FRONT
            CameraCharacteristics.LENS_FACING_FRONT -> CameraCharacteristics.LENS_FACING_BACK
            else -> CameraCharacteristics.LENS_FACING_BACK
          }
        }
        .subscribe { direction ->
          cameraDirection = direction
          closeCamera()
          val (width, height) = view.textureSize()
          openCamera(width, height)
        }
      // will configure flash changes
      disposeBag += view.observeToggleFlash()
        .map { toggle ->
          val newMode = when(flash) {
            FLASH_MODE_AUTO -> C.FLASH_MODE_DISABLED
            FLASH_MODE_DISABLED -> C.FLASH_MODE_AUTO
            else -> C.FLASH_MODE_AUTO
          }
          toggle.isSelected = newMode == C.FLASH_MODE_AUTO
          newMode
        }
        .subscribe { mode ->
          flash = mode
          when(mode) {
            FLASH_MODE_AUTO -> autoFlash(previewRequestBuilder)
            FLASH_MODE_DISABLED -> disableFlash(previewRequestBuilder)
          }
        }
    }
  }

  override fun onStop() = disposeBag.clear()

  override fun onResume() {
    startBackground()

    if (view.isTextureAvailable()) {
      val (width, height) = view.textureSize()
      openCamera(width, height)
    } else {
      view.surfaceTextureListener(surfaceTextureListener)
    }
  }

  override fun onPause() {
    closeCamera()
    stopBackground()
  }
}
