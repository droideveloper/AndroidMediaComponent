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
import android.os.Bundle
import android.support.v4.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import org.fs.architecture.common.AbstractPresenter
import org.fs.architecture.common.BusManager
import org.fs.architecture.common.scope.ForActivity
import org.fs.component.gallery.model.event.NextSelectedEvent
import org.fs.component.gallery.presenter.GalleryFragmentPresenterImp
import org.fs.component.gallery.util.C.Companion.COMPONENT_PICK_ALL
import org.fs.component.gallery.util.C.Companion.COMPONENT_PICK_IMAGE
import org.fs.component.gallery.util.C.Companion.COMPONENT_PICK_VIDEO
import org.fs.component.gallery.view.GalleryFragment
import org.fs.component.media.R
import org.fs.component.media.util.C.Companion.COMPONENT_ALL
import org.fs.component.media.util.C.Companion.COMPONENT_PHOTO
import org.fs.component.media.util.C.Companion.COMPONENT_VIDEO
import org.fs.component.media.util.plusAssign
import org.fs.component.media.view.CapturePhotoFragment
import org.fs.component.media.view.CaptureVideoFragment
import org.fs.component.media.view.ComponentActivityView
import org.fs.component.media.view.NextActivity
import javax.inject.Inject

@ForActivity
class ComponentActivityPresenterImp @Inject constructor(
  view: ComponentActivityView): AbstractPresenter<ComponentActivityView>(view), ComponentActivityPresenter {

  companion object {
    const val BUNDLE_ARGS_COMPONENT = "bundle.args.component"
    private const val BUNDLE_ARGS_SELECTION = "bundle.args.selection"
  }

  private val disposeBag by lazy { CompositeDisposable() }
  private var component: Int = COMPONENT_ALL
  private var selection  = -1

  override fun restoreState(restore: Bundle?) {
    restore?.apply {
      if (containsKey(BUNDLE_ARGS_COMPONENT)) {
        component = getInt(BUNDLE_ARGS_COMPONENT)
      }
      if (containsKey(BUNDLE_ARGS_SELECTION)) {
        selection = getInt(BUNDLE_ARGS_SELECTION, -1)
      }
    }
  }

  override fun storeState(store: Bundle?) {
    store?.putInt(BUNDLE_ARGS_COMPONENT, component)
    store?.putInt(BUNDLE_ARGS_SELECTION, selection)
  }

  override fun onCreate() {
    if (view.isAvailable()) {
      view.setUp(component)
    }
  }

  override fun onStart() {
    if (view.isAvailable()) {
      disposeBag += view.observeNext()
        .map { NextSelectedEvent(Intent(view.getContext(), NextActivity::class.java)) }
        .subscribe(BusManager.Companion::send)

      disposeBag += view.observeCancel()
        .subscribe { onBackPressed() }

      disposeBag += view.observeSelectedTab()
        .map { it.customView?.id ?: -1 }
        .filter { selection != it }
        .doOnNext {
          selection = it
        }
        .subscribe(this::render)

      checkIfInitialLoadNeeded()
    }
  }

  override fun onStop() = disposeBag.clear()

  override fun onBackPressed() {
    if (view.isAvailable()) {
      view.finish()
    }
  }

  private fun checkIfInitialLoadNeeded() {
    if (selection == -1) {
      selection = R.id.gallery
      render(selection)
    }
  }

  private fun render(tabId: Int) {
    val fragment = fragmentFor(tabId)
    val titleRes = titleResFor(tabId)
    view.render(fragment, titleRes)
  }

  private fun fragmentFor(id: Int): Fragment = when(id) {
    R.id.gallery -> GalleryFragment().apply {
      arguments = Bundle().apply {
        putInt(GalleryFragmentPresenterImp.BUNDLE_ARGS_COMPONENT, mediaType())
      }
    }
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
    COMPONENT_ALL -> COMPONENT_PICK_ALL
    COMPONENT_VIDEO -> COMPONENT_PICK_VIDEO
    COMPONENT_PHOTO -> COMPONENT_PICK_IMAGE
    else -> throw IllegalStateException("component is not known $component")
  }
}