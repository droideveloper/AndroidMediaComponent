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

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.scope.ForActivity
import org.fs.architecture.util.EMPTY
import org.fs.component.gallery.model.entity.Media
import org.fs.component.gallery.util.C.Companion.BUNDLE_ARGS_MEDIA
import org.fs.component.gallery.util.C.Companion.BUNDLE_ARGS_SELECTED_MEDIA
import org.fs.component.gallery.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.gallery.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.common.FFmpegBinaryCallback
import org.fs.component.media.common.FFmpegCommandCallback
import org.fs.component.media.util.C.Companion.MIME_GIF
import org.fs.component.media.util.C.Companion.RENDER_FILL
import org.fs.component.media.util.C.Companion.RENDER_FIX
import org.fs.component.media.util.async
import org.fs.component.media.util.log
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.NextActivityView
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@ForActivity
class NextActivityPresenterImp @Inject constructor(
  view: NextActivityView): AbstractPresenter<NextActivityView>(view), NextActivityPresenter {

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
    }, error = {
      if (view.isAvailable()) {
        view.showError("We can not load binaries for video processing")
      }
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

  private fun scaleOrCropAndPad(media: Media, renderMode: Int) = when(media.mediaType) {
    MEDIA_TYPE_IMAGE -> {
      val (x, y) = view.retrieveXY()
      val (w, h) = view.retrieveSize(MEDIA_TYPE_IMAGE)
      val (pw, ph) = view.previewSize()

      if (media.mime == MIME_GIF) {
        // this is render fix, I need to make render fill mode too
        val command = ArrayList<String>()
        command.add("-threads")
        command.add("8")
        command.add("-y")
        command.add("-i")
        command.add(media.file.absolutePath)
        command.add("-movflags")
        command.add("faststart")
        command.add("-pix_fmt")
        command.add("yuv420p")
        when(renderMode) {
          RENDER_FILL -> {
            command.add("-vf")
            val wi = if (pw > w) w else pw
            val hi = if (ph > h) h else ph
            command.add("crop=$wi:$hi:$x:$y")
            command.add("-preset")
            command.add("ultrafast")
            command.add(toFileMp4(media).absolutePath)

            ffmpeg.execute(command.toTypedArray(), FFmpegCommandCallback(start = {
              if (view.isAvailable()) {
                view.showProgress()
              }
            }, success =  {
              if (view.isAvailable()) {
                view.hideProgress()
                view.setResultAndFinish(Intent().apply {
                  putExtra(BUNDLE_ARGS_SELECTED_MEDIA, media.copy(file = toFileMp4(media)))
                })
              }
            }, error = {
              if (view.isAvailable()) {
                view.showError(it ?: String.EMPTY)
                log(Log.ERROR, it ?: String.EMPTY)
                view.hideProgress()
              }
            }))
          }
          RENDER_FIX -> {
            command.add("-vf")
            if (w > h) {
              val r = h / w.toFloat()
              val hh = Math.round(pw * r)
              val hr = when(hh % 2) {
                0 -> hh
                else -> hh + 1
              }
              command.add("scale=$pw:$hr")//,pad=720:720:(ow-iw)/2:(oh-ih)/2:color=black")
            } else {
              val r = w / h.toFloat()
              val ww = Math.round(ph * r)
              val wr = when(ww % 2) {
                0 -> ww
                else -> ww + 1
              }
              command.add("scale=$wr:$ph")//,pad=720:720:(ow-iw)/2:(oh-ih)/2:color=black")
            }
            command.add("-preset")
            command.add("ultrafast")
            command.add(toFileMp4(media).absolutePath)

            ffmpeg.execute(command.toTypedArray(), FFmpegCommandCallback(start = {
              if (view.isAvailable()) {
                view.showProgress()
              }
            }, success =  {
              if (view.isAvailable()) {
                view.hideProgress()
                view.setResultAndFinish(Intent().apply {
                  putExtra(BUNDLE_ARGS_SELECTED_MEDIA, media.copy(file = toFileMp4(media)))
                })
              }
            }, error = {
              if (view.isAvailable()) {
                view.showError(it ?: String.EMPTY)
                log(Log.ERROR, it ?: String.EMPTY)
                view.hideProgress()
              }
            }))
          }
          else -> Unit
        }
      } else {
        when (renderMode) {
          RENDER_FILL -> {
            disposeBag += Completable.fromAction {
              val bitmap = BitmapFactory.decodeFile(media.file.absolutePath)
              val displayMetrics = view.displayMetrics()
              val dx = Math.round(displayMetrics.density * x)
              val dy = Math.round(displayMetrics.density * y)
              val wf = if (bitmap.width < pw + dx) bitmap.width else pw + dx
              val hf = if (bitmap.height < ph + dy) bitmap.height else ph + dy
              val cropped = Bitmap.createBitmap(bitmap, dx, dy, wf, hf)
              val output = FileOutputStream(toFile(media).absolutePath)
              cropped.compress(Bitmap.CompressFormat.JPEG, 100, output)
              output.close()
              cropped.recycle()
              bitmap.recycle()
            }.async(view)
             .subscribe {
               view.setResultAndFinish(Intent().apply {
                 putExtra(BUNDLE_ARGS_SELECTED_MEDIA, media.copy(file = toFile(media)))
               })
             }
          }
          RENDER_FIX -> {
            disposeBag += Completable.fromAction {
              val bitmap = BitmapFactory.decodeFile(media.file.absolutePath)
              val r = if(bitmap.height > bitmap.width) bitmap.width / bitmap.height.toFloat() else bitmap.height / bitmap.width.toFloat()
              val dw = when {
                bitmap.height > bitmap.width -> Math.round(pw * r)
                else -> pw
              }
              val dh = when {
                bitmap.width > bitmap.height -> Math.round(ph * r)
                else -> ph
              }
              val scaled = Bitmap.createScaledBitmap(bitmap, dw, dh, false)
              val output = FileOutputStream(toFile(media).absolutePath)
              scaled.compress(Bitmap.CompressFormat.JPEG, 100, output)
              output.close()
              scaled.recycle()
              bitmap.recycle()
            }.async(view)
             .subscribe {
               view.setResultAndFinish(Intent().apply {
                 putExtra(BUNDLE_ARGS_SELECTED_MEDIA, media.copy(file = toFile(media)))
               })
             }
          }
          else -> Unit
        }
      }
    }
    MEDIA_TYPE_VIDEO -> {
      val (x, y) = view.retrieveXY()
      val (w, h) = view.retrieveSize(MEDIA_TYPE_VIDEO)
      val (pw, ph) = view.previewSize()
      //val timeline = view.retrieveTimeline()
      val max = Math.max(w, h)

      val command = ArrayList<String>()
      command.add("-threads")
      command.add("8")
      command.add("-y")
      command.add("-i")
      command.add(media.file.absolutePath)
      //command.add("-ss") // start position of video crop by time
      //command.add(TimeUnit.MILLISECONDS.toSeconds(timeline.start).toString()) // start

      //ffmpeg -i input.mp4 -lavfi '[0:v]scale=ih*16/9:-1,boxblur=luma_radius=min(h\,w)/20:luma_power=1:chroma_radius=min(cw\,ch)/20:chroma_power=1[bg];[bg][0:v]overlay=(W-w)/2:(H-h)/2,crop=h=iw*9/16' -vb 800K output.webm

      when(renderMode) {
        RENDER_FILL -> {
          command.add("-vf")
          val wi = if (pw > w) w else pw
          val hi = if (ph > h) h else ph
          command.add("crop=$wi:$hi:$x:$y")
          //command.add("-t")
          //command.add(TimeUnit.MILLISECONDS.toSeconds(timeline.end).toString())
          //command.add("-vcodec")
          //command.add("libx264")
          //command.add("-threads")
          //command.add("2")
          //command.add("-preset")
          //command.add("ultrafast")
          //command.add(toFile(media).absolutePath)
          //command.add("-threads")
          //command.add("16")
          //command.add("-c:v")
          command.add("-vcodec")
          command.add("libx264")
          command.add("-preset")
          command.add("ultrafast")
          //command.add("-bufsize")
          //command.add("8196k")
          //command.add("-crf") decrease quality
          //command.add("28")
          command.add(toFile(media).absolutePath)
          ffmpeg.execute(command.toTypedArray(), FFmpegCommandCallback(start = {
            if (view.isAvailable()) {
              view.showProgress()
            }
          }, success =  {
            if (view.isAvailable()) {
              view.hideProgress()
              view.setResultAndFinish(Intent().apply {
                putExtra(BUNDLE_ARGS_SELECTED_MEDIA, media.copy(file = toFile(media)))
              })
            }
          }, error = {
            if (view.isAvailable()) {
              view.showError(it ?: String.EMPTY)
              view.hideProgress()
            }
          }))
        }
        // "[0:v]scale=-1:iw,boxblur=luma_radius=min(h\\,w)/20:luma_power=1:chroma_radius=min(cw\\,ch)/20:chroma_power=1[bg];[bg][0:v]overlay=(W-w)/2:(H-h)/2,crop=w=ih"
        // "[0:v]scale=ih:-1,boxblur=luma_radius=min(h\\,w)/20:luma_power=1:chroma_radius=min(cw\\,ch)/20:chroma_power=1[bg];[bg][0:v]overlay=(W-w)/2:(H-h)/2,crop=h=iw"
        RENDER_FIX -> {
          val vf = when(max) {
            w -> {
              val r = h / w.toFloat()
              val hh = Math.round(pw * r)
              val hr = when(hh % 2) {
                0 -> hh
                else -> hh + 1
              }
              "scale=$pw:$hr"
            }//,crop=$pw:ih"//,pad=$pw:$ph:(ow-iw)/2:(oh-ih)/2:color=black"
            h -> {
              val r = w / h.toFloat()
              val ww = Math.round(ph * r)
              val wr = when(ww % 2) {
                0 -> ww
                else -> ww + 1
              }
              "scale=$wr:$ph"
            }//,crop=iw:$ph"//,pad=$pw:$ph:(ow-iw)/2:(oh-ih)/2:color=black"
            else -> String.EMPTY
          }
          command.add("-vf")
          //command.add("-lavfi")
          command.add(vf)
          //command.add("-t")
          //command.add(TimeUnit.MILLISECONDS.toSeconds(timeline.end).toString())
          //command.add("-vcodec")
          //command.add("libx264")
          //command.add("-threads")
          //command.add("1024")
          // command.add("-c:v")
          //command.add("-c:v")
          command.add("-vcodec")
          command.add("libx264")
          command.add("-preset")
          command.add("ultrafast")
          //command.add("-bufsize")
          //command.add("8196k")
          //command.add("-crf") decrease quality
          //command.add("28")
          //command.add("-threads")
          //command.add("16")
          //command.add("-preset")
          //command.add("ultrafast")
          command.add(toFile(media).absolutePath)

          ffmpeg.execute(command.toTypedArray(), FFmpegCommandCallback(start = {
            if (view.isAvailable()) {
              view.showProgress()
            }
          }, success =  {
            if (view.isAvailable()) {
              view.hideProgress()
              view.setResultAndFinish(Intent().apply {
                putExtra(BUNDLE_ARGS_SELECTED_MEDIA, media.copy(file = toFile(media)))
              })
            }
          }, error = {
            if (view.isAvailable()) {
              view.showError(it ?: String.EMPTY)
              view.hideProgress()
            }
          }))
        }
        else -> Unit
      }
    }
    else -> Unit
  }

  private fun toFile(media: Media): File = File(directory, media.file.name)
  private fun toFileMp4(media: Media): File = File(directory, "${media.file.name}.mp4")
}