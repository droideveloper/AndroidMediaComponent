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
import org.fs.architecture.common.scope.ForFragment
import org.fs.architecture.util.ObservableList
import org.fs.component.media.model.entity.Media
import org.fs.component.media.presenter.*
import org.fs.component.media.view.*

@Module
class BaseFragmentModule {

  @ForFragment @Provides fun providesCaptureVideoFragmentView(fragment: CaptureVideoFragment): CaptureVideoFragmentView = fragment
  @ForFragment @Provides fun providesCaptureVideoFragmentPresenter(presenter: CaptureVideoFragmentPresenterImp): CaptureVideoFragmentPresenter = presenter

  @ForFragment @Provides fun providesCapturePhotoFragmentView(fragment: CapturePhotoFragment): CapturePhotoFragmentView = fragment
  @ForFragment @Provides fun providesCapturePhotoFragmentPresenter(presenter: CapturePhotoFragmentPresenterImp): CapturePhotoFragmentPresenter = presenter

  @ForFragment @Provides fun providesGalleryFragmentView(fragment: GalleryFragment): GalleryFragmentView = fragment
  @ForFragment @Provides fun providesGalleryFragmentPresenter(presenter: GalleryFragmentPresenterImp): GalleryFragmentPresenter = presenter
  @ForFragment @Provides fun providesGalleryDateSet(): ObservableList<Media> = ObservableList()
}