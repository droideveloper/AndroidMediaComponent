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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TimeUtils
import com.bumptech.glide.util.ByteBufferUtil.toFile
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.scope.ForActivity
import org.fs.architecture.util.EMPTY
import org.fs.component.media.common.FFmpegBinaryCallback
import org.fs.component.media.common.FFmpegCommandCallback
import org.fs.component.media.model.entity.Media
import org.fs.component.media.util.C
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.util.C.Companion.RENDER_FILL
import org.fs.component.media.util.C.Companion.RENDER_FIX
import org.fs.component.media.util.Size
import org.fs.component.media.util.async
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.NextActivityView
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ForActivity
class NextActivityPresenterImp @Inject constructor(
  view: NextActivityView): AbstractPresenter<NextActivityView>(view), NextActivityPresenter {

  companion object {
    const val BUNDLE_ARGS_MEDIA = "bundle.args.media"
  }

  private var media: Media = Media.EMPTY
  private var renderMode = RENDER_FIX // this is better to have in first case

  private val disposeBag by lazy { CompositeDisposable() }
  private val ffmpeg by lazy { FFmpeg.getInstance(view.getContext()) }
  private var supportFfmpeg = false
  private val directory by lazy { File(view.getContext()?.filesDir, "modified_file") }

  override fun restoreState(restore: Bundle?) {
    restore?.apply {
      if (containsKey(BUNDLE_ARGS_MEDIA)) {
        media = getParcelable(BUNDLE_ARGS_MEDIA) ?: Media.EMPTY
      }
    }
  }

  override fun storeState(store: Bundle?) {
    store?.putParcelable(BUNDLE_ARGS_MEDIA, media)
  }

  override fun onCreate() {
    if (view.isAvailable()) {
      view.setUp(media, renderMode)
      // will create directory for temp file
      if (!directory.exists()) {
        directory.mkdirs()
      } else {
        // if already exists then we clear everything previously left over
        val files = directory.listFiles()
        if (files.isNotEmpty()) {
          files.forEach { f -> f.delete() }
        }
      }
    }
    // load ffmpeg binaries
    ffmpeg.loadBinary(FFmpegBinaryCallback(success = {
      supportFfmpeg = true
    }))
  }

  override fun onStart() {
    if (view.isAvailable()) {
      // much better approach
      disposeBag += view.observeNext()
        .subscribe {scaleOrCropAndPad(media, renderMode) }

      disposeBag += view.observeChangeScale()
        .doOnNext {
          it.isSelected = !it.isSelected
        }
        .map { if (it.isSelected) RENDER_FILL else RENDER_FIX }
        .subscribe {
          renderMode = it // grab reference for future use
          view.render(media, renderMode)
        }

      disposeBag += view.observeCancel()
        .subscribe { onBackPressed() }
    }
  }

  override fun onStop() = disposeBag.clear()

  override fun onBackPressed() {
    if (view.isAvailable()) {
      view.finish()
    }
  }

  private fun scaleOrCropAndPad(media: Media, renderMode: Int) = when(media.type) {
    MEDIA_TYPE_IMAGE -> {
      val (x, y) = view.retrieveXY()
      val (w, h) = view.retrieveSize(MEDIA_TYPE_IMAGE)

      when(renderMode) {
        RENDER_FILL -> {
          disposeBag += Completable.fromAction {
            val bitmap = BitmapFactory.decodeFile(media.file.absolutePath)
            val displayMetrics = view.displayMetrics()
            val dx = Math.round(displayMetrics.density * x)
            val dy = Math.round(displayMetrics.density * y)
            val cropped = Bitmap.createBitmap(bitmap, dx, dy, bitmap.width - dx, bitmap.height - dy)
            bitmap.recycle()
            val output = FileOutputStream(toFile(media).absolutePath)
            cropped.compress(Bitmap.CompressFormat.JPEG, 100, output)
            output.close()
            cropped.recycle()
          }.async(view)
           .subscribe()
        }
        RENDER_FIX -> {
          disposeBag += Completable.fromAction {
            val bitmap = BitmapFactory.decodeFile(media.file.absolutePath)
            val scaled = Bitmap.createScaledBitmap(bitmap, w, h, false)
            bitmap.recycle()
            val max = Math.max(w, h)
            val bmp = Bitmap.createBitmap(max, max, scaled.config)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.BLACK)
            val left = (max - w) / 2f
            val top = (max - h) / 2f
            canvas.drawBitmap(scaled, left, top, null)
            scaled.recycle()
            val output = FileOutputStream(toFile(media).absolutePath)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, output)
            output.close()
            bmp.recycle()
          }.async(view)
           .subscribe()
        }
        else -> Unit
      }
    }
    MEDIA_TYPE_VIDEO -> {
      val (x, y) = view.retrieveXY()
      val (w, h) = view.retrieveSize(MEDIA_TYPE_VIDEO)
      val (pw, ph) = view.previewSize()
      val timeline = view.retrieveTimeline()
      val max = Math.max(w, h)

      val command = ArrayList<String>()
      command.add("-y")
      command.add("-i")
      command.add(media.file.absolutePath)
      command.add("-ss") // start position of video crop by time
      command.add(TimeUnit.MILLISECONDS.toSeconds(timeline.start).toString()) // start

      when(renderMode) {
        RENDER_FILL -> {
          command.add("-vf")
          command.add("crop=$pw:$ph:$x:$y")
          command.add("-t")
          command.add(TimeUnit.MILLISECONDS.toSeconds(timeline.end).toString())
          command.add("-vcodec")
          command.add("libx264")
          command.add("-threads")
          command.add("2")
          command.add("-preset")
          command.add("ultrafast")
          command.add(toFile(media).absolutePath)

          ffmpeg.execute(command.toTypedArray(), FFmpegCommandCallback(start = {
            view.showProgress()
          }, success =  {
            view.hideProgress()
          }))
        }
        RENDER_FIX -> {
          val vf = when(max) {
            w -> "scale=$pw:-1,pad=$pw:$ph:(ow-iw)/2:(oh-ih)/2:color=black"
            h -> "scale=-1:$ph,pad=$pw:$ph:(ow-iw)/2:(oh-ih)/2:color=black"
            else -> String.EMPTY
          }
          command.add("-vf")
          command.add(vf)
          command.add("-t")
          command.add(TimeUnit.MILLISECONDS.toSeconds(timeline.end).toString())
          command.add("-vcodec")
          command.add("libx264")
          command.add("-threads")
          command.add("2")
          command.add("-preset")
          command.add("ultrafast")
          command.add(toFile(media).absolutePath)

          ffmpeg.execute(command.toTypedArray(), FFmpegCommandCallback(start = {
            view.showProgress()
          }, success =  {
            view.hideProgress()
          }))
        }
        else -> Unit
      }
    }
    else -> Unit
  }

  private fun toFile(media: Media): File = File(directory, media.file.name)
}