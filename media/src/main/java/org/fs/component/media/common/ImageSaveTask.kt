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
    // create byte array for size
    val bytes = ByteArray(buffer.remaining())
    // read those bytes
    buffer.get(bytes)
    // kotlin goodies goes on here
    FileOutputStream(file).use { output ->
      // write
      output.write(bytes)
      // close source it
      image.close()
      // close destination it
      output.close()
    }
  }
}