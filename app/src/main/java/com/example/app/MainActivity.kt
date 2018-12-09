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

package com.example.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.main_activity.*
import org.fs.component.media.presenter.ComponentActivityPresenterImp
import org.fs.component.media.view.ComponentActivity

class MainActivity: AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)

    button.setOnClickListener {
      startActivity(Intent(this, ComponentActivity::class.java).apply {
        putExtra(ComponentActivityPresenterImp.BUNDLE_ARGS_COMPONENT, 0x01)
      })
    }
  }
}