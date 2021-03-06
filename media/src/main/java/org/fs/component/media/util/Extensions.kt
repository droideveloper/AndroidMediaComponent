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
package org.fs.component.media.util

import android.support.annotation.IntDef
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.fs.architecture.common.ViewType
import org.fs.architecture.util.NEW_LINE
import org.fs.component.media.BuildConfig

operator fun Size.component1() = width
operator fun Size.component2() = height

operator fun CompositeDisposable.plusAssign(disposable: Disposable) { add(disposable) }

fun Completable.async(): Completable = subscribeOn(Schedulers.io())
  .observeOn(AndroidSchedulers.mainThread())

fun Completable.async(view: ViewType?) = async()
  .doOnSubscribe { view?.showProgress() }
  .doOnComplete { view?.hideProgress() }

fun <T> Observable<T>.async(): Observable<T> = subscribeOn(Schedulers.io())
  .observeOn(AndroidSchedulers.mainThread())

fun <T> Single<T>.async(): Single<T> = subscribeOn(Schedulers.io())
  .observeOn(AndroidSchedulers.mainThread())

fun <T> Single<T>.async(view: ViewType?): Single<T> = async()
  .doOnSubscribe { view?.showProgress() }
  .doFinally { view?.hideProgress() }


// log level limitations
@Target(AnnotationTarget.VALUE_PARAMETER)
@IntDef(value = [Log.ASSERT, Log.DEBUG, Log.ERROR, Log.INFO, Log.VERBOSE, Log.WARN])
annotation class LogLevel

// log levels will be here
inline fun <reified T> T.isLogEnabled(): Boolean = BuildConfig.DEBUG
inline fun <reified T> T.getClassTag(): String = T::class.java.simpleName

// log printers
inline fun <reified T> T.log(msg: String) = log(Log.DEBUG, msg)
inline fun <reified T> T.log(@LogLevel level: Int, msg: String) {
  if (isLogEnabled()) {
    val minStacksSize = 1
    val msgStack = msg.split(String.NEW_LINE)
    if (msgStack.size > minStacksSize) {
      msgStack.forEach { stack -> Log.println(level, getClassTag(), stack) }
    } else {
      Log.println(level, getClassTag(), msg)
    }
  }
}

fun <T> Collection<T>.min(comp: Comparator<T>): T? {
  val i = iterator()
  if (i.hasNext()) {
    var candidate = i.next()
    while (i.hasNext()) {
      val next = i.next()
      if (comp.compare(candidate, next) < 0) {
        candidate = next
      }
    }
    return candidate
  }
  return null
}