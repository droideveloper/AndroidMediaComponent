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
package org.fs.component.media.repository

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import io.reactivex.Observable
import org.fs.component.media.model.entity.Media
import org.fs.component.media.util.C
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryImageRespository @Inject constructor(context: Context): AbstractRepository() {

  companion object {
    // private const val DATE_REPRESENTATION = "yyyy.MM.dd"

    private const val INDEX_DATA = 1
    private const val INDEX_DISPLAY_NAME = 2
    private const val INDEX_TAKEN = 3
    private const val INDEX_MIME = 4
  }

  private val contextResolver by lazy { context.contentResolver }
  private val uri by lazy { MediaStore.Images.Media.EXTERNAL_CONTENT_URI }

  private val projection by lazy {
    arrayOf(
        MediaStore.Images.ImageColumns._ID,
        MediaStore.Images.ImageColumns.DATA,
        MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Images.ImageColumns.DATE_TAKEN,
        MediaStore.Images.ImageColumns.MIME_TYPE)
  }

  private val orderBy by lazy { MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC" }
  //private val simpleDateFormat = SimpleDateFormat(DATE_REPRESENTATION, Locale.US)

  //private var latest: Date? = null

  fun loadAsync(cursor: Cursor = contextResolver.query(uri, projection, null, null, orderBy)): Observable<List<Media>> = Observable.just(cursor)
    .map { c -> c.toList()}
    /*.filter { media ->
      if (latest == null) {
        latest = simpleDateFormat.parse(simpleDateFormat.format(Date(media.taken)))
      }
      val current = simpleDateFormat.parse(simpleDateFormat.format(Date(media.taken)))
      ((latest?.time ?: 0L) - current.time) == 0L
    }
    */

  private fun Cursor.toList(): List<Media> {
    val list = ArrayList<Media>()
    while (moveToNext()) {
      list.add(Media(C.MEDIA_TYPE_IMAGE,
        File(getString(INDEX_DATA)),
        getLong(INDEX_TAKEN),
        getString(INDEX_DISPLAY_NAME),
        getString(INDEX_MIME)))
    }
    return list.also {
      close()
    }
  }
}