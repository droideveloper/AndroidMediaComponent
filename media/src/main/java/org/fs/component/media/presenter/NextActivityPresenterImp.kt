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
import android.os.Bundle
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import io.reactivex.disposables.CompositeDisposable
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.scope.ForActivity
import org.fs.architecture.util.EMPTY
import org.fs.component.media.model.entity.Media
import org.fs.component.media.util.C
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.util.C.Companion.RENDER_FILL
import org.fs.component.media.util.C.Companion.RENDER_FIX
import org.fs.component.media.util.Size
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.NextActivityView
import java.io.File
import javax.inject.Inject

@ForActivity
class NextActivityPresenterImp @Inject constructor(
  view: NextActivityView): AbstractPresenter<NextActivityView>(view), NextActivityPresenter {

  companion object {
    const val BUNDLE_ARGS_MEDIA = "bundle.args.media"
  }

  private var media: Media = Media.EMPTY
  private var renderMode = RENDER_FILL

  private val disposeBag by lazy { CompositeDisposable() }
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
  }

  override fun onStart() {
    if (view.isAvailable()) {
      disposeBag += view.observeNext()
        .subscribe { when(media.type) {
            C.MEDIA_TYPE_VIDEO -> cropVideo(media)
            C.MEDIA_TYPE_IMAGE -> cropImage(media)
            else -> Unit
          }
        }

      disposeBag += view.observeChangeScale()
        .doOnNext {
          it.isSelected = !it.isSelected
        }
        .map { if (it.isSelected) RENDER_FIX else RENDER_FILL }
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

  private fun cropImage(media: Media) {
    // TODO do crop
  }

  private fun cropVideo(media: Media) {
    val ffmpeg = FFmpeg.getInstance(view.getContext())
    ffmpeg.loadBinary(object: FFmpegLoadBinaryResponseHandler {
      override fun onFinish() = Unit
      override fun onFailure() = Unit
      override fun onStart() = Unit

      override fun onSuccess() {
        ffmpeg.execute(toScaleAndPad(media),
            object : FFmpegExecuteResponseHandler {
              override fun onFinish() = Unit

              override fun onSuccess(message: String?) {
                Log.e("FFMPEG-Success", message ?: String.EMPTY)
              }

              override fun onFailure(message: String?) {
                Log.e("FFMPEG-Error", message ?: String.EMPTY)
              }

              override fun onProgress(message: String?) {
                Log.e("FFMPEG-Progress", message ?: String.EMPTY)
              }

              override fun onStart() = Unit
            })
      }
    })
  }

  private fun toScaleAndPad(media: Media): Array<String> = arrayOf("-y",
      "-i", media.file.absolutePath,
      "-ss", "10",
      "-vf", "scale=-1:720,pad=720:ih:(ow-iw)/2:color=white",
      "-t", "10",
      "-vcodec", "libx264",
      "-threads", "5",
      "-preset", "ultrafast",
      "-strict", "-2",
      toFile(media).absolutePath)

  private fun toScaleAndCrop(media: Media): Array<String> = arrayOf("-y",
      "-i", media.file.absolutePath,
      "-vf", "scale=-1:720,crop=iw:720",
      "-vcodec", "libx264",
      "-threads", "5",
      "-preset", "ultrafast",
      "-strict", "-2",
      toFile(media).absolutePath)

  private fun toScaleAndCrop(media: Media, renderMode: Int) = when(media.type) {
    MEDIA_TYPE_IMAGE -> when(renderMode) {
      RENDER_FILL -> {
        val (x, y) = view.retrieveXY()
        val (w, h) = view.retrieveSize(MEDIA_TYPE_IMAGE)
        // TODO continue
      }
      RENDER_FIX -> {
        val (w, h) = view.retrieveSize(MEDIA_TYPE_IMAGE)
        val max = Math.max(w, h) // we retrieve max value among those what we receive
        // TODO do it with task
      }
      else -> Unit
    }
    MEDIA_TYPE_VIDEO -> when(renderMode) {
      RENDER_FILL -> Unit
      RENDER_FIX -> Unit
      else -> Unit
    }
    else -> Unit // Do not
  }

  private fun toScaleAndPad(media: Media, renderMode: Int) = when(media.type) {
    MEDIA_TYPE_IMAGE -> when(renderMode) {
      RENDER_FILL -> Unit
      RENDER_FIX -> Unit
      else -> Unit
    }
    MEDIA_TYPE_VIDEO -> when(renderMode) {
      RENDER_FILL -> Unit
      RENDER_FIX -> Unit
      else -> Unit
    }
    else -> Unit
  }

  private fun toFile(media: Media): File = File(directory, media.file.name)
}