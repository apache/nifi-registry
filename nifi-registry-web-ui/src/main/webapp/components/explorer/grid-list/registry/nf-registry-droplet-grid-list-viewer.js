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
 * NfRegistryDropletGridListViewer constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param ActivatedRoute        The angular activated route module.
 * @constructor
 */
function NfRegistryDropletGridListViewer(nfRegistryService, ActivatedRoute) {
    this.route = ActivatedRoute;
    this.nfRegistryService = nfRegistryService;
};

NfRegistryDropletGridListViewer.prototype = {
    constructor: NfRegistryDropletGridListViewer,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.explorerViewType = 'grid-list';
        this.route.params
            .switchMap(function (params) {
                return new rxjs.Observable.forkJoin(
                    self.nfRegistryService.api.getDroplet(params['bucketId'], params['dropletType'], params['dropletId']),
                    self.nfRegistryService.api.getBucket(params['bucketId']),
                    self.nfRegistryService.api.getBuckets(),
                    self.nfRegistryService.api.getDroplets(params['bucketId'])
                );
            })
            .subscribe(function (response) {
                var droplet = response[0];
                var bucket = response[1];
                var buckets = response[2];
                var droplets = response[3];
                self.nfRegistryService.bucket = bucket;
                self.nfRegistryService.buckets = buckets;
                self.nfRegistryService.droplet = droplet;
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

NfRegistryDropletGridListViewer.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-grid-list-viewer.html!text'),
        animations: [nfRegistryAnimations.flyInOutAnimation]
    })
];

NfRegistryDropletGridListViewer.parameters = [
    NfRegistryService,
    ngRouter.ActivatedRoute
];

module.exports = NfRegistryDropletGridListViewer;
