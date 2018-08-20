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

import android.support.v4.app.Fragment
import dagger.Binds
import dagger.Module
import org.fs.architecture.common.scope.ForFragment
import org.fs.component.media.view.CapturePhotoFragment
import org.fs.component.media.view.CaptureVideoFragment

@Module abstract class FragmentModule {

  @ForFragment @Binds abstract fun capturePhotoFragment(fragment: CapturePhotoFragment): Fragment
  @ForFragment @Binds abstract fun captureVideoFragment(fragment: CaptureVideoFragment): Fragment
}