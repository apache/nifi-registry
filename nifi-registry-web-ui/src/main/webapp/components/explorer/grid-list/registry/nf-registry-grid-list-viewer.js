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
var ngRouter = require('@angular/router');
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');
var fdsDialogsModule = require('@fluid-design-system/dialogs');

/**
 * NfRegistryGridListViewer constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param ActivatedRoute        The angular activated route module.
 * @param TdDialogService       The covalent dialog service module.
 * @constructor
 */
function NfRegistryGridListViewer(nfRegistryService, ActivatedRoute, FdsDialogService) {
    this.route = ActivatedRoute;
    this.nfRegistryService = nfRegistryService;
    this.dialogService = FdsDialogService;
};

NfRegistryGridListViewer.prototype = {
    constructor: NfRegistryGridListViewer,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.explorerViewType = 'grid-list';
        this.route.params
            .switchMap(function (params) {
                return self.nfRegistryService.getRegistry(params['registryId']);
            })
            .subscribe(function (registry) {
                self.nfRegistryService.registry = registry;
                self.nfRegistryService.getBuckets(self.nfRegistryService.registry.id).then(function (buckets) {
                    self.nfRegistryService.buckets = buckets;
                    self.nfRegistryService.getDroplets(self.nfRegistryService.registry.id, self.nfRegistryService.bucket.id, self.nfRegistryService.droplet.id).then(function (droplets) {
                        self.nfRegistryService.droplets = self.nfRegistryService.filteredDroplets = droplets;
                        self.nfRegistryService.filterDroplets();
                    });
                })
            });
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.registry = {};
        this.nfRegistryService.buckets = [];
        this.nfRegistryService.droplets = [];
        this.nfRegistryService.filteredDroplets = [];
    },

    /**
     * Execute the given droplet action.
     *
     * @param action        The action object.
     * @param droplet       The droplet object the `action` will act upon.
     */
    execute: function (action, droplet) {
        var self = this;
        if (action.name.toLowerCase() === 'delete') {
            this.dialogService.openConfirm({
                title: 'Delete Data Flow',
                message: 'All versions of this data flow will be deleted.',
                cancelButton: 'Cancel',
                acceptButton: 'Delete',
                acceptButtonColor: 'fds-warn'
            }).afterClosed().subscribe(
                function (accept) {
                    if (accept) {
                        self.nfRegistryService.deleteDroplet(droplet.id);
                    }
                });
        }
    }
};

NfRegistryGridListViewer.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-grid-list-viewer.html!text'),
        animations: [nfRegistryAnimations.flyInOutAnimation]
    })
];

NfRegistryGridListViewer.parameters = [NfRegistryService, ngRouter.ActivatedRoute, fdsDialogsModule.FdsDialogService];

module.exports = NfRegistryGridListViewer;
