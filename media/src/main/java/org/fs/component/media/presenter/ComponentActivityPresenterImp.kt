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
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.BusManager
import org.fs.architecture.common.scope.ForActivity
import org.fs.component.media.R
import org.fs.component.media.model.event.NextSelectedEvent
import org.fs.component.media.util.C
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.CapturePhotoFragment
import org.fs.component.media.view.CaptureVideoFragment
import org.fs.component.media.view.ComponentActivityView
import org.fs.component.media.view.GalleryFragment
import javax.inject.Inject

@ForActivity
class ComponentActivityPresenterImp @Inject constructor(
  view: ComponentActivityView): AbstractPresenter<ComponentActivityView>(view), ComponentActivityPresenter {

  companion object {
    const val BUNDLE_ARGS_COMPONENT = "bundle.args.component"
  }

  private val disposeBag by lazy { CompositeDisposable() }
  private var component: Int = C.COMPONENT_ALL

  override fun restoreState(restore: Bundle?) {
    restore?.apply {
      if (containsKey(BUNDLE_ARGS_COMPONENT)) {
        component = getInt(BUNDLE_ARGS_COMPONENT)
      }
    }
  }

  override fun storeState(store: Bundle?) {
    store?.putInt(BUNDLE_ARGS_COMPONENT, component)
  }

  override fun onCreate() {
    if (view.isAvailable()) {
      view.setUp(component)
    }
  }

  override fun onStart() {
    if (view.isAvailable()) {
      disposeBag += view.observeNext()
        .map { _ -> NextSelectedEvent() }
        .subscribe(BusManager.Companion::send)

      disposeBag += view.observeCancel()
        .subscribe { _ -> onBackPressed() }

      disposeBag += view.observeSelectedTab()
        .subscribe(this::render)
    }
  }

  override fun onStop() = disposeBag.clear()

  override fun onBackPressed() {
    if (view.isAvailable()) {
      view.finish()
    }
  }

  private fun render(tab: TabLayout.Tab) {
    tab.customView?.id?.let { id ->
      val fragment = fragmentFor(id)
      val titleRes = titleResFor(id)
      view.render(fragment, titleRes)
    }
  }

  private fun fragmentFor(id: Int): Fragment = when(id) {
    R.id.gallery -> GalleryFragment.newFragment(mediaType())
    R.id.photo -> CapturePhotoFragment()
    R.id.video -> CaptureVideoFragment()
    else -> throw IllegalStateException("component is not known $component")
  }

  private fun titleResFor(id: Int): Int = when(id) {
    R.id.gallery -> R.string.str_gallery
    R.id.video -> R.string.str_video
    R.id.photo -> R.string.str_photo
    else -> throw IllegalStateException("component is not known $component")
  }

  private fun mediaType(): Int = when(component) {
    C.COMPONENT_ALL -> C.MEDIA_TYPE_ALL
    C.COMPONENT_VIDEO -> C.MEDIA_TYPE_VIDEO
    C.COMPONENT_PHOTO -> C.MEDIA_TYPE_IMAGE
    else -> throw IllegalStateException("component is not known $component")
  }
}