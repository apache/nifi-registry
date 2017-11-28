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
var ngCore = require('@angular/core');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var ngRouter = require('@angular/router');

/**
 * NfRegistryBucketPermissions constructor.
 *
 * @param nfRegistryApi         The api service.
 * @param nfRegistryService     The nf-registry.service module.
 * @param ActivatedRoute        The angular activated route module.
 * @param Router                The angular router module.
 * @constructor
 */
function NfRegistryBucketPermissions(nfRegistryApi, nfRegistryService, ActivatedRoute, Router) {
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.route = ActivatedRoute;
    this.router = Router;
};

NfRegistryBucketPermissions.prototype = {
    constructor: NfRegistryBucketPermissions,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.sidenav.open();
        this.route.params
            .switchMap(function (params) {
                return self.nfRegistryApi.getBucket(params['bucketId']);
            })
            .subscribe(function (bucket) {
                self.nfRegistryService.bucket = bucket;
            });
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.sidenav.close();
        this.nfRegistryService.bucket = {};
    },

    /**
     * Navigate to administer the buckets of the current registry.
     */
    closeSideNav: function () {
        this.router.navigateByUrl('/nifi-registry/administration/workflow');
    }
};

NfRegistryBucketPermissions.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-bucket-permissions.html!text')
    })
];

NfRegistryBucketPermissions.parameters = [
    NfRegistryApi,
    NfRegistryService,
    ngRouter.ActivatedRoute,
    ngRouter.Router
];

module.exports = NfRegistryBucketPermissions;
