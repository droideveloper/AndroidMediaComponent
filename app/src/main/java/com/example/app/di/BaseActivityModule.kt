/*
 * Playz Android Kotlin Copyright (C) 2018 Fatih, Playz.lol.
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

package com.example.app.di

import dagger.Module
import dagger.Provides
import org.fs.architecture.common.scope.ForActivity
import org.fs.architecture.util.ObservableList
import org.fs.component.gallery.model.entity.Media
import org.fs.component.gallery.presenter.GalleryActivityPresenter
import org.fs.component.gallery.presenter.GalleryActivityPresenterImp
import org.fs.component.gallery.presenter.GalleryFragmentPresenter
import org.fs.component.gallery.presenter.GalleryFragmentPresenterImp
import org.fs.component.gallery.view.GalleryActivity
import org.fs.component.gallery.view.GalleryActivityView
import org.fs.component.media.presenter.ComponentActivityPresenter
import org.fs.component.media.presenter.ComponentActivityPresenterImp
import org.fs.component.media.presenter.NextActivityPresenter
import org.fs.component.media.presenter.NextActivityPresenterImp
import org.fs.component.media.view.ComponentActivity
import org.fs.component.media.view.ComponentActivityView
import org.fs.component.media.view.NextActivity
import org.fs.component.media.view.NextActivityView

@Module
class BaseActivityModule {

  @ForActivity @Provides fun provideComponentActivityView(activity: ComponentActivity): ComponentActivityView = activity
  @ForActivity @Provides fun provideComponentActivityPresenter(presenter: ComponentActivityPresenterImp): ComponentActivityPresenter = presenter

  @ForActivity @Provides fun provideNextActivityView(activity: NextActivity): NextActivityView = activity
  @ForActivity @Provides fun provideNextActivityPresenter(presenter: NextActivityPresenterImp): NextActivityPresenter = presenter

  @ForActivity @Provides fun provideGalleryActivityView(activity: GalleryActivity): GalleryActivityView = activity
  @ForActivity @Provides fun provideGalleryActivityPresenter(presenter: GalleryActivityPresenterImp): GalleryActivityPresenter = presenter
  @ForActivity @Provides fun provideMediaDataSet(): ObservableList<Media> = ObservableList()
}