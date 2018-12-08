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

import android.os.Bundle
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.BusManager
import org.fs.architecture.common.scope.ForFragment
import org.fs.architecture.util.ObservableList
import org.fs.component.media.common.CompareMediaByDateTaken
import org.fs.component.media.model.entity.Media
import org.fs.component.media.model.event.MediaSelectedEvent
import org.fs.component.media.repository.GalleryImageRespository
import org.fs.component.media.repository.GalleryVideoRepository
import org.fs.component.media.util.C
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_ALL
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_IMAGE
import org.fs.component.media.util.C.Companion.MEDIA_TYPE_VIDEO
import org.fs.component.media.util.async
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.GalleryFragmentView
import javax.inject.Inject

@ForFragment
class GalleryFragmentPresenterImp @Inject constructor(
    view: GalleryFragmentView,
    private val dataSet: ObservableList<Media>,
    private val galleryImageRepository: GalleryImageRespository,
    private val galleryVideoRepository: GalleryVideoRepository): AbstractPresenter<GalleryFragmentView>(view), GalleryFragmentPresenter {

  companion object {
    const val BUNDLE_ARGS_MEDIA_TYPE = "bundle.args.media.type"
    private const val BUNDLE_ARGS_MEDIA = "bundle.args.media"
  }

  private val disposeBag by lazy { CompositeDisposable() }

  private var mediaType: Int = C.MEDIA_TYPE_ALL
  private var media: Media? = Media.EMPTY

  override fun restoreState(restore: Bundle?) {
    restore?.apply {
      if (containsKey(BUNDLE_ARGS_MEDIA_TYPE)) {
        mediaType = getInt(BUNDLE_ARGS_MEDIA_TYPE)
      }
      if (containsKey(BUNDLE_ARGS_MEDIA)) {
        media = getParcelable(BUNDLE_ARGS_MEDIA) ?: Media.EMPTY
      }
    }
  }

  override fun storeState(store: Bundle?) {
    store?.putInt(BUNDLE_ARGS_MEDIA_TYPE, mediaType)
    store?.putParcelable(BUNDLE_ARGS_MEDIA, media)
  }

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
          is MediaSelectedEvent -> view.render(evt.media) // should not come empty here but just to be safe
        }
      })
      if (dataSet.isEmpty()) {
        load()
      }
    }
  }

  override fun onStop() = disposeBag.clear()

  private fun load() {
    disposeBag += dataSource()
      .flatMap { s -> Observable.fromIterable(s) }
      .toSortedList(CompareMediaByDateTaken.COMPARE_MEDIA_BY_DATE_TAKEN)
      .async(view)
      .subscribe(
      { data ->
        if (view.isAvailable()) {
          if (data.isEmpty()) {
            dataSet.addAll(data)
          }
        }
      },
      { error -> view.showError(error.toString()) })
  }

  private fun dataSource(): Observable<List<Media>> = when(mediaType) {
    MEDIA_TYPE_ALL -> Observable.concat(loadImageMedia(), loadVideoMedia())
    MEDIA_TYPE_IMAGE -> loadImageMedia()
    MEDIA_TYPE_VIDEO -> loadVideoMedia()
    else -> Observable.empty()
  }

  private fun loadImageMedia(): Observable<List<Media>> = galleryImageRepository.loadAsync()
  private fun loadVideoMedia(): Observable<List<Media>> = galleryVideoRepository.loadAsync()
}