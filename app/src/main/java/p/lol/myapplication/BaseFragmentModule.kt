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
package p.lol.myapplication

import dagger.Module
import dagger.Provides
import org.fs.architecture.common.scope.ForFragment
import org.fs.component.media.presenter.CapturePhotoFragmentPresenter
import org.fs.component.media.presenter.CapturePhotoFragmentPresenterImp
import org.fs.component.media.presenter.CaptureVideoFragmentPresenter
import org.fs.component.media.presenter.CaptureVideoFragmentPresenterImp
import org.fs.component.media.view.CapturePhotoFragment
import org.fs.component.media.view.CapturePhotoFragmentView
import org.fs.component.media.view.CaptureVideoFragment
import org.fs.component.media.view.CaptureVideoFragmentView

@Module class BaseFragmentModule {

  @ForFragment @Provides fun provideCapturePhotoFragmentView(fragment: CapturePhotoFragment): CapturePhotoFragmentView = fragment
  @ForFragment @Provides fun provideCapturePhotoFragmentPresenter(presenter: CapturePhotoFragmentPresenterImp): CapturePhotoFragmentPresenter = presenter

  @ForFragment @Provides fun provideCaptureVideoFragmentView(fragment: CaptureVideoFragment): CaptureVideoFragmentView = fragment
  @ForFragment @Provides fun provideCaptureVideoFragmentPresenter(presenter: CaptureVideoFragmentPresenterImp): CaptureVideoFragmentPresenter = presenter
}