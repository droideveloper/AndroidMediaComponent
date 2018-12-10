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
package org.fs.component.media.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.media.Image
import java.io.File
import java.io.FileOutputStream

class ImageSaveTask private constructor(private val image: Image, private val file: File): Runnable {

  companion object {
    @JvmStatic fun newTask(image: Image, file: File): ImageSaveTask = ImageSaveTask(image, file)
  }

  override fun run() {
    // read first buffer
    val buffer = image.planes.first().buffer
    val sizeBuffer = buffer.rewind()
    // might tread it better
    val bytes = ByteArray(sizeBuffer.remaining())
    buffer.get(bytes)

    val width = image.width
    val height = image.height

    FileOutputStream(file).use { output ->
      output.write(bytes, 0, bytes.size)
      image.close()
      output.close()
    }

    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    if (bitmap.width != width || bitmap.height != height) {
      val w = Math.min(width, height) // get proper value

      val tmp = Bitmap.createBitmap(w, w, bitmap.config)

      val dw = Math.abs(bitmap.width - w)
      val dh = Math.abs(bitmap.height - w)

      val src = Rect(dw / 2, dh / 2, bitmap.width - dw / 2, bitmap.height - dh / 2)
      val dst = Rect(0, 0, w, w)
      val canvas = Canvas(tmp)
      canvas.drawBitmap(bitmap, src, dst, null)

      if (file.exists()) {
        file.delete()
      }

      FileOutputStream(file).use { output ->
        tmp.compress(Bitmap.CompressFormat.JPEG, 100, output)
        output.close()
      }
      tmp.recycle()
    }
    bitmap.recycle()
  }
}