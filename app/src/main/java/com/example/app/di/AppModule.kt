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

import android.app.Application
import android.content.Context
import com.example.app.App
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.fs.architecture.common.scope.ForActivity
import org.fs.component.media.view.ComponentActivity
import org.fs.component.media.view.NextActivity
import javax.inject.Singleton

@Module
abstract class AppModule {

  @Singleton @Binds abstract fun bindApp(app: App): Application
  @Singleton @Binds abstract fun bindContext(app: Application): Context

  @ForActivity @ContributesAndroidInjector(modules = [ActivityModule::class, BaseActivityModule::class])
  abstract fun componentActivity(): ComponentActivity

  @ForActivity @ContributesAndroidInjector(modules = [ActivityModule::class, BaseActivityModule::class])
  abstract fun nextActivity(): NextActivity
}