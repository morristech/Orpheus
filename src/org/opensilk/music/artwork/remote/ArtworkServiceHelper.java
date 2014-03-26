/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.artwork.remote;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;

import org.opensilk.music.artwork.cache.BitmapLruCache;

import java.io.File;

import hugo.weaving.DebugLog;

/**
 * Wrapper for IArtworkService to simplify logic needed in the remote service
 *
 * Created by drew on 3/23/14.
 */
public class ArtworkServiceHelper {
    private static final String TAG = ArtworkServiceHelper.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float THUMB_MEM_CACHE_DIVIDER = 0.10f;

    private Context mContext;

    /**
     * Binder
     */
    private IArtworkService mService = null;

    /**
     * Services private image cache
     */
    private BitmapLruCache mL1Cache;

    public ArtworkServiceHelper(Context context) {
        mContext = context;
        initCache();
    }

    private void initCache() {
        final ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = activityManager.getMemoryClass();
        final int lruThumbCacheSize = Math.round(THUMB_MEM_CACHE_DIVIDER * memClass * 1024 * 1024);
        if (D) Log.d(TAG, "thumbcache=" + ((float) lruThumbCacheSize / 1024 / 1024) + "MB");
        mL1Cache = new BitmapLruCache(lruThumbCacheSize);
    }

    @DebugLog
    public void bind() {
        mContext.bindService(new Intent(mContext, ArtworkService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @DebugLog
    public void unbind() {
        mContext.unbindService(mConnection);
    }

    /**
     * @return IBinder
     */
    public IArtworkService getService() {
        return mService;
    }

    /**
     * Wrapper method for getting artwork for the service
     * Checks its local cache first, then calls to the ArtService (which checks
     * its cache), and as last resort loads the default art.
     */
    @DebugLog
    public Bitmap getArtwork(String artistName, String albumName, long albumId) {
        String cacheKey = makeCacheKey(artistName, albumName, albumId);
        Bitmap bitmap = mL1Cache.getBitmap(cacheKey);
        if (bitmap == null && mService != null) {
            try {
                final String path = mService.getCurrentArtwork();
                if (!TextUtils.isEmpty(path)) {
                    // Parse the file
                    bitmap = BitmapFactory.decodeFile(path);
                    // Remove the file in the background;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                new File(path).delete();
                            } catch (Exception ignored) {
                            }
                        }
                    }).start();
                    if (bitmap != null) {
                        mL1Cache.putBitmap(cacheKey, bitmap);
                    }
                }
            } catch (RemoteException ignored) { }
        }
        if (bitmap == null) {
            //TODO mediastore
            // Couldn't get it from ArtService, load the default
            if (mContext.getResources() != null) {
                BitmapDrawable drawable =
                        (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.default_artwork);
                if (drawable != null) {
                    bitmap = drawable.getBitmap();
                }
            }
        }
        return bitmap;
    }

    /**
     * Generates a cache key for local  L1Cache
     */
    private String makeCacheKey(String artistName, String albumName, long albumId) {
        return new StringBuilder((artistName != null ? artistName.length() : 4)
                + (albumName != null ? albumName.length() : 4) + 1)
                .append(artistName)
                .append(albumName)
                .append(albumId)
                .toString();
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IArtworkServiceImpl.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
}