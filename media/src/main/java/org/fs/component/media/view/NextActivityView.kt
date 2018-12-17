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
package org.fs.component.media.view

import android.content.Intent
import android.util.DisplayMetrics
import android.view.View
import io.reactivex.Observable
import org.fs.architecture.common.ViewType
import org.fs.component.gallery.model.entity.Media
import org.fs.component.media.util.Size
import org.fs.component.media.util.Timeline

interface NextActivityView: ViewType {
  fun setUp(media: Media, renderMode: Int)
  fun displayMetrics(): DisplayMetrics
  fun observeNext(): Observable<View>
  fun observeCancel(): Observable<View>
  fun observeChangeScale(): Observable<View>
  fun retrieveXY(): Size
  fun previewSize(): Size
  fun retrieveTimeline(): Timeline
  fun retrieveSize(mediaType: Int): Size
  fun render(media: Media, renderMode: Int)
  fun setResultAndFinish(data: Intent?)
}