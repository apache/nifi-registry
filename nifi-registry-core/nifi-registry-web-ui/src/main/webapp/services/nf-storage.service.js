/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Store items for two days before being eligible for removal.
var MILLIS_PER_DAY = 86400000;
var TWO_DAYS = MILLIS_PER_DAY * 2;

var isUndefined = function (obj) {
    return typeof obj === 'undefined';
};

var isNull = function (obj) {
    return obj === null;
};

var isDefinedAndNotNull = function (obj) {
    return !isUndefined(obj) && !isNull(obj);
};

/**
 * Checks the expiration for the specified entry.
 *
 * @param {object} entry
 * @returns {boolean}
 */
var checkExpiration = function (entry) {
    if (isDefinedAndNotNull(entry.expires)) {
        // get the expiration
        var expires = new Date(entry.expires);
        var now = new Date();

        // return whether the expiration date has passed
        return expires.valueOf() < now.valueOf();
    }
    return false;
};

/**
 * Gets an enty for the key. The entry expiration is not checked.
 *
 * @param {string} key
 */
var getEntry = function (key) {
    try {
        // parse the entry
        var entry = JSON.parse(localStorage.getItem(key));

        // ensure the entry and item are present
        if (isDefinedAndNotNull(entry)) {
            return entry;
        }
        return null;
    } catch (e) {
        return null;
    }
};

/**
 * NfRegistryStorage constructor.
 * @constructor
 */
function NfRegistryStorage() {
}

NfRegistryStorage.prototype = {
    constructor: NfRegistryStorage,
    /**
     * Initializes the storage. Items will be persisted for two days. Once the scripts runs
     * thereafter, all eligible items will be removed. This strategy does not support persistence.
     */
    init: function () {
        for (var i = 0; i < localStorage.length; i++) {
            try {
                // get the next item
                var key = localStorage.key(i);

                // attempt to get the item which will expire if necessary
                this.getItem(key);
            } catch (e) {
                // ignore
            }
        }
    },

    /**
     * Stores the specified item.
     *
     * @param {string} key
     * @param {object} item
     * @param {integer} expires
     */
    setItem: function (key, item, expires) {
        // calculate the expiration
        expires = isDefinedAndNotNull(expires) ? expires : new Date().valueOf() + TWO_DAYS;

        // create the entry
        var entry = {
            expires: expires,
            item: item
        };

        // store the item
        localStorage.setItem(key, JSON.stringify(entry));
    },

    /**
     * Returns whether there is an entry for this key. This will not check the expiration. If
     * the entry is expired, it will return null on a subsequent getItem invocation.
     *
     * @param {string} key
     * @returns {boolean}
     */
    hasItem: function (key) {
        return getEntry(key) !== null;
    },

    /**
     * Gets the item with the specified key. If an item with this key does
     * not exist, null is returned. If an item exists but cannot be parsed
     * or is malformed/unrecognized, null is returned.
     *
     * @param {type} key
     */
    getItem: function (key) {
        var entry = getEntry(key);
        if (entry === null) {
            return null;
        }

        // if the entry is expired, drop it and return null
        if (checkExpiration(entry)) {
            this.removeItem(key);
            return null;
        }

        // if the entry has the specified field return its value
        if (isDefinedAndNotNull(entry['item'])) {
            return entry['item'];
        }
        return null;
    },

    /**
     * Gets the expiration for the specified item. This will not check the expiration. If
     * the entry is expired, it will return null on a subsequent getItem invocation.
     *
     * @param {string} key
     * @returns {integer}
     */
    getItemExpiration: function (key) {
        var entry = getEntry(key);
        if (entry === null) {
            return null;
        }

        // if the entry has the specified field return its value
        if (isDefinedAndNotNull(entry['expires'])) {
            return entry['expires'];
        }
        return null;
    },

    /**
     * Extracts the subject from the specified jwt. If the jwt is not as expected
     * an empty string is returned.
     *
     * @param {string} jwt
     * @returns {string}
     */
    getJwtPayload: function (jwt) {
        if (isDefinedAndNotNull(jwt)) {
            var segments = jwt.split(/\./);
            if (segments.length !== 3) {
                return '';
            }

            var rawPayload = window.atob(segments[1]);
            var payload = JSON.parse(rawPayload);

            if (isDefinedAndNotNull(payload)) {
                return payload;
            }
            return null;
        }

        return null;
    },

    /**
     * Removes the item with the specified key.
     *
     * @param {type} key
     */
    removeItem: function (key) {
        localStorage.removeItem(key);
    }
};

NfRegistryStorage.parameters = [];

export default NfRegistryStorage;
