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
var rxjs = require('rxjs/Rx');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var ngRouter = require('@angular/router');
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');

/**
 * NfRegistryBucketGridListViewer constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param ActivatedRoute        The angular activated route module.
 * @constructor
 */
function NfRegistryBucketGridListViewer(nfRegistryService, ActivatedRoute) {
    this.route = ActivatedRoute;
    this.nfRegistryService = nfRegistryService;
};

NfRegistryBucketGridListViewer.prototype = {
    constructor: NfRegistryBucketGridListViewer,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.explorerViewType = 'grid-list';
        this.nfRegistryService.droplet = {};
        this.route.params
            .switchMap(function (params) {
                return new rxjs.Observable.forkJoin(
                    self.nfRegistryService.api.getBuckets(),
                    self.nfRegistryService.api.getDroplets(params['bucketId']),
                    self.nfRegistryService.api.getBucket(params['bucketId'])
                );
            })
            .subscribe(function (response) {
                var buckets = response[0];
                var droplets = response[1];
                var bucket = response[2];
                self.nfRegistryService.bucket = bucket;
                self.nfRegistryService.buckets = buckets;
                self.nfRegistryService.droplets = droplets;
                self.nfRegistryService.filterDroplets();
                self.nfRegistryService.setBreadcrumbState('in');
            });
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.explorerViewType = '';
        this.nfRegistryService.setBreadcrumbState('out');
    }
};

NfRegistryBucketGridListViewer.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-grid-list-viewer.html!text'),
        animations: [nfRegistryAnimations.flyInOutAnimation]
    })
];

NfRegistryBucketGridListViewer.parameters = [
    NfRegistryService,
    ngRouter.ActivatedRoute
];

module.exports = NfRegistryBucketGridListViewer;
