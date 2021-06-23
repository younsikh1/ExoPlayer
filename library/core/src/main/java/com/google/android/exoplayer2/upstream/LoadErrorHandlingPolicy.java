/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.google.android.exoplayer2.upstream;

import androidx.annotation.IntDef;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.upstream.Loader.Callback;
import com.google.android.exoplayer2.upstream.Loader.Loadable;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines how errors encountered by loaders are handled.
 *
 * <p>A loader that can choose between one of a number of resources can exclude a resource when a
 * load error occurs. In this case, {@link #getExclusionDurationMsFor(int, LoadErrorInfo)} defines
 * whether the resource should be excluded for a given {@link FallbackType fallback type}, and if so
 * for how long. If the policy indicates that a resource should be excluded, the loader will exclude
 * it for the specified amount of time unless all of the alternatives for the given fallback type
 * are already excluded.
 *
 * <p>When exclusion does not take place, {@link #getRetryDelayMsFor(LoadErrorInfo)} defines whether
 * the load is retried. An error that's not retried will always be propagated. An error that is
 * retried will be propagated according to {@link #getMinimumLoadableRetryCount(int)}.
 *
 * <p>Methods are invoked on the playback thread.
 */
public interface LoadErrorHandlingPolicy {

  /** Fallback type. One of {@link #FALLBACK_TYPE_LOCATION} or {@link #FALLBACK_TYPE_TRACK}. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({FALLBACK_TYPE_LOCATION, FALLBACK_TYPE_TRACK})
  @interface FallbackType {}

  /**
   * Fallback type that is using exclusion of locations (i.e., multiple URLs through which the same
   * data is accessible).
   */
  int FALLBACK_TYPE_LOCATION = 1;
  /**
   * Fallback type that is using exclusion of tracks (i.e., multiple URLs through which different
   * representations of the same content are available; for example the same video encoded at
   * different bitrates or resolutions).
   */
  int FALLBACK_TYPE_TRACK = 2;

  /** Holds information about a load task error. */
  final class LoadErrorInfo {

    /** The {@link LoadEventInfo} associated with the load that encountered an error. */
    public final LoadEventInfo loadEventInfo;
    /** {@link MediaLoadData} associated with the load that encountered an error. */
    public final MediaLoadData mediaLoadData;
    /** The exception associated to the load error. */
    public final IOException exception;
    /** The number of errors this load task has encountered, including this one. */
    public final int errorCount;

    /** Creates an instance with the given values. */
    public LoadErrorInfo(
        LoadEventInfo loadEventInfo,
        MediaLoadData mediaLoadData,
        IOException exception,
        int errorCount) {
      this.loadEventInfo = loadEventInfo;
      this.mediaLoadData = mediaLoadData;
      this.exception = exception;
      this.errorCount = errorCount;
    }
  }

  /**
   * Returns the number of milliseconds for which a resource associated to a provided load error
   * should be excluded for a given {@link FallbackType fallback type}, or {@link C#TIME_UNSET} if
   * the resource should not be excluded.
   *
   * @param fallbackType The {@link FallbackType fallback type} used for exclusion.
   * @param loadErrorInfo A {@link LoadErrorInfo} holding information about the load error.
   * @return The exclusion duration in milliseconds, or {@link C#TIME_UNSET} if the resource should
   *     not be excluded.
   */
  long getExclusionDurationMsFor(@FallbackType int fallbackType, LoadErrorInfo loadErrorInfo);

  /**
   * Returns the number of milliseconds to wait before attempting the load again, or {@link
   * C#TIME_UNSET} if the error is fatal and should not be retried.
   *
   * <p>Loaders may ignore the retry delay returned by this method in order to wait for a specific
   * event before retrying. However, the load is retried if and only if this method does not return
   * {@link C#TIME_UNSET}.
   *
   * @param loadErrorInfo A {@link LoadErrorInfo} holding information about the load error.
   * @return The number of milliseconds to wait before attempting the load again, or {@link
   *     C#TIME_UNSET} if the error is fatal and should not be retried.
   */
  long getRetryDelayMsFor(LoadErrorInfo loadErrorInfo);

  /**
   * Called once {@code loadTaskId} will not be associated with any more load errors.
   *
   * <p>Implementations should clean up any resources associated with {@code loadTaskId} when this
   * method is called.
   */
  default void onLoadTaskConcluded(long loadTaskId) {}

  /**
   * Returns the minimum number of times to retry a load in the case of a load error, before
   * propagating the error.
   *
   * @param dataType One of the {@link C C.DATA_TYPE_*} constants indicating the type of data to
   *     load.
   * @return The minimum number of times to retry a load in the case of a load error, before
   *     propagating the error.
   * @see Loader#startLoading(Loadable, Callback, int)
   */
  int getMinimumLoadableRetryCount(int dataType);
}
