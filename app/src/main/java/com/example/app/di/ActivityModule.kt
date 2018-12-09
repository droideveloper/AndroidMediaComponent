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
import dagger.android.ContributesAndroidInjector
import org.fs.architecture.common.scope.ForFragment
import org.fs.component.media.view.CapturePhotoFragment
import org.fs.component.media.view.CaptureVideoFragment
import org.fs.component.media.view.GalleryFragment

@Module
abstract class ActivityModule {

  @ForFragment @ContributesAndroidInjector(modules = [FragmentModule::class, BaseFragmentModule::class])
  abstract fun captureVideoFragment(): CaptureVideoFragment

  @ForFragment @ContributesAndroidInjector(modules = [FragmentModule::class, BaseFragmentModule::class])
  abstract fun capturePhotoFragment(): CapturePhotoFragment

  @ForFragment @ContributesAndroidInjector(modules = [FragmentModule::class, BaseFragmentModule::class])
  abstract fun galleryFragment(): GalleryFragment
}