/*
 * Copyright 2009 James Abley
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
package com.eternus.ratelimit;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;

/**
 * {@link TokenStore} implementation that uses ehcache as the backing store for {@link StoreEntry}s.
 * 
 * @author jabley
 * 
 */
public class EhcacheTokenStore implements TokenStore {

    /**
     * The {@link BlockingCache} used to back this {@link TokenStore}.
     */
    private BlockingCache cache;

    /**
     * Sets the non-null {@link Ehcache} used to back this {@link TokenStore}.
     * 
     * @param cache
     *            a non-null {@link Ehcache}
     */
    public void setCache(Ehcache cache) {
        BlockingCache ref;

        if (!(cache instanceof BlockingCache)) {
            ref = new BlockingCache(cache);
            cache.getCacheManager().replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));
        } else {
            ref = (BlockingCache) cache;
        }

        this.cache = ref;
    }

    /**
     * {@inheritDoc}
     */
    public StoreEntry create(Key key, int timeToLive) {
        StoreEntryImpl result = new StoreEntryImpl(timeToLive);
        Element element = new Element(key, result);
        element.setTimeToLive(timeToLive);
        cache.put(element);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public StoreEntry get(Key key) {

        Element entry = null;

        try {

            /* This may block. */
            entry = cache.get(key);
        } catch (LockTimeoutException e) {
            throw new RuntimeException();
        } catch (RuntimeException e) {

            /* Release the lock that may have been acquired. */
            cache.put(new Element(key, null));
        }

        StoreEntry result = null;

        if (entry != null) {

            /*
             * We don't need to check isExpired() on the result, since ehcache takes care of expiring entries for us.
             * c.f. the get(Key) implementation in this class.
             */
            result = (StoreEntry) entry.getObjectValue();
        }

        return result;
    }

}
