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
import io.reactivex.disposables.CompositeDisposable
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.scope.ForActivity
import org.fs.component.media.model.entity.Media
import org.fs.component.media.util.C
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.NextActivityView
import javax.inject.Inject

@ForActivity
class NextActivityPresenterImp @Inject constructor(
  view: NextActivityView): AbstractPresenter<NextActivityView>(view), NextActivityPresenter {

  companion object {
    const val BUNDLE_ARGS_MEDIA = "bundle.args.media"
  }

  private var media: Media = Media.EMPTY

  private val disposeBag by lazy { CompositeDisposable() }

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
      view.setUp(media)
    }
  }

  override fun onStart() {
    if (view.isAvailable()) {
      disposeBag += view.observeNext()
        .subscribe { _ -> when(media.type) {
            C.MEDIA_TYPE_VIDEO -> cropVideo(media)
            C.MEDIA_TYPE_IMAGE -> cropImage(media)
            else -> Unit
          }
        }

      disposeBag += view.observeCancel()
        .subscribe { _ -> onBackPressed() }
    }
  }

  override fun onStop() = disposeBag.clear()

  override fun onBackPressed() {
    if (view.isAvailable()) {
      view.finish()
    }
  }

  private fun cropImage(media: Media?) {
    // TODO do crop
  }

  private fun cropVideo(media: Media?) {
    // TODO do crop
  }
}