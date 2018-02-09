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

var NfStorage = require('nifi-registry/services/nf-storage.service.js');

/**
 * NfRegistryTokenInterceptor constructor.
 *
 * @param nfStorage             The NfStorage module.
 * @constructor
 */
function NfRegistryTokenInterceptor(nfStorage) {
    this.nfStorage = nfStorage;
};

NfRegistryTokenInterceptor.prototype = {
    constructor: NfRegistryTokenInterceptor,

    /**
     * Generates the droplet grid-list explorer component's sorting menu options.
     *
     * @param request       angular HttpRequest.
     * @param next          angular HttpHandler.
     * @returns {Observable HTTPEvent}
     */
    intercept: function(request, next) {
        var token = this.nfStorage.getItem('jwt');
        if(token) {
            request = request.clone({headers: request.headers.set('Authorization', 'Bearer ' + token)});
        }
        return next.handle(request);
    }
};

NfRegistryTokenInterceptor.parameters = [
    NfStorage
];

module.exports = NfRegistryTokenInterceptor;