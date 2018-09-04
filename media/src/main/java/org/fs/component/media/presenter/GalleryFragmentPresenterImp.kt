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

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.BusManager
import org.fs.architecture.common.scope.ForFragment
import org.fs.component.media.model.entity.Media
import org.fs.component.media.model.event.MediaSelectedEvent
import org.fs.component.media.repository.GalleryImageRespository
import org.fs.component.media.repository.GalleryVideoRepository
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.GalleryFragmentView
import javax.inject.Inject

@ForFragment
class GalleryFragmentPresenterImp @Inject constructor(
    view: GalleryFragmentView,
    private val galleryImageRepository: GalleryImageRespository,
    private val galleryVideoRepository: GalleryVideoRepository): AbstractPresenter<GalleryFragmentView>(view), GalleryFragmentPresenter {

  companion object {

  }

  private val disposeBag by lazy { CompositeDisposable() }

  private var media: Media = Media.EMPTY

  override fun onCreate() {
    if (view.isAvailable()) {
      view.setUp()
      view.render(media) // if empty we ignore it
    }
  }

  override fun onStart() {
    if (view.isAvailable()) {
      disposeBag += BusManager.add(Consumer { evt ->
        when(evt) {
          is MediaSelectedEvent -> view.render(evt.media ?: Media.EMPTY) // should not come empty here but just to be safe
        }
      })
    }
  }

  override fun onStop() = disposeBag.clear()
}