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

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import io.reactivex.Observable
import kotlinx.android.synthetic.main.view_component_activity.*
import kotlinx.android.synthetic.main.view_tab_item.view.*
import org.fs.architecture.core.AbstractActivity
import org.fs.component.media.R
import org.fs.component.media.presenter.ComponentActivityPresenter
import org.fs.component.media.util.C
import org.fs.component.media.util.C.Companion.COMPONENT_ALL
import org.fs.component.media.util.C.Companion.COMPONENT_PHOTO
import org.fs.component.media.util.C.Companion.COMPONENT_VIDEO
import org.fs.rx.extensions.design.util.selects
import org.fs.rx.extensions.util.clicks

class ComponentActivity: AbstractActivity<ComponentActivityPresenter>(), ComponentActivityView {

  private val data by lazy { arrayOf(Pair(R.id.gallery, R.string.str_gallery), Pair(R.id.photo, R.string.str_photo), Pair(R.id.video, R.string.str_video)) }
  private val factory by lazy { LayoutInflater.from(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.view_component_activity)

    presenter.restoreState(savedInstanceState ?: intent.extras)
    presenter.onCreate()
  }

  override fun setUp(type: Int) = when(type) {
    COMPONENT_ALL -> renderTabLayout(data)
    COMPONENT_PHOTO -> renderTabLayout(data.filter { (id, _) -> id != R.id.video }.toTypedArray())
    COMPONENT_VIDEO -> renderTabLayout(data.filter { (id, _) -> id != R.id.photo }.toTypedArray())
    else -> throw IllegalArgumentException("component type is not known for $type")
  }

  override fun observeSelectedTab(): Observable<TabLayout.Tab> = viewTabLayout.selects()
  override fun observeCancel(): Observable<View> = viewButtonCancel.clicks()
  override fun observeNext(): Observable<View> = viewButtonNext.clicks()

  override fun render(fragment: Fragment, titleRes: Int) {
    // set text title
    viewTextTitle.setText(titleRes)
    // replace commit
    supportFragmentManager.beginTransaction()
      .replace(R.id.viewContentFrameLayout, fragment)
      .commit()
  }

  private fun renderTabLayout(data: Array<Pair<Int, Int>>) = data.forEach { (id, stringRes) ->
    viewTabLayout.newTab().apply {
      val view = factory.inflate(R.layout.view_tab_item, viewTabLayout, false)
      view.id = id // id
      view.viewTextTab.setText(stringRes) // text
      customView = view
      viewTabLayout.addTab(this)
    }
  }
}