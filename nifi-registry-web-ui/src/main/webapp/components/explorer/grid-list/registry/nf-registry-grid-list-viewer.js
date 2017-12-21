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
var rxjs = require('rxjs/Observable');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');
var ngRouter = require('@angular/router');
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');

/**
 * NfRegistryGridListViewer constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param nfRegistryApi         The api service.
 * @param activatedRoute        The angular activated route module.
 * @param nfStorage             A wrapper for the browser's local storage.
 * @constructor
 */
function NfRegistryGridListViewer(nfRegistryService, nfRegistryApi, activatedRoute, nfStorage) {
    this.route = activatedRoute;
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.nfStorage = nfStorage;
};

NfRegistryGridListViewer.prototype = {
    constructor: NfRegistryGridListViewer,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.explorerViewType = 'grid-list';
        this.nfRegistryService.inProgress = true;

        // reset the breadcrumb state
        this.nfRegistryService.bucket = {};
        this.nfRegistryService.droplet = {};

        // subscribe to the route params
        this.$subscription = this.route.params
            .switchMap(function (params) {
                return new rxjs.Observable.forkJoin(self.nfRegistryApi.getDroplets(),
                    self.nfRegistryApi.getBuckets());
            })
            .subscribe(function (response) {
                var droplets = response[0];
                var buckets = response[1];
                self.nfRegistryService.buckets = buckets;
                self.nfRegistryService.droplets = droplets;
                self.nfRegistryService.filterDroplets();
                self.nfRegistryService.setBreadcrumbState('in');
                self.nfRegistryService.inProgress = false;
            });
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.explorerViewType = '';
        this.nfRegistryService.setBreadcrumbState('out');
        this.nfRegistryService.filteredDroplets = [];
        this.$subscription.unsubscribe();
    }
};

NfRegistryGridListViewer.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-grid-list-viewer.html!text'),
        animations: [nfRegistryAnimations.flyInOutAnimation]
    })
];

NfRegistryGridListViewer.parameters = [
    NfRegistryService,
    NfRegistryApi,
    ngRouter.ActivatedRoute,
    NfStorage
];

module.exports = NfRegistryGridListViewer;
