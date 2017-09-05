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
 * NfRegistryWorkflowAdministration constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param ActivatedRoute        The angular activated route module.
 * @param Router                The angular router module.
 * @param FdsDialogService      The FDS dialog service.
 * @constructor
 */
function NfRegistryWorkflowAdministration(nfRegistryService, ActivatedRoute, Router, FdsDialogService) {
    this.route = ActivatedRoute;
    this.nfRegistryService = nfRegistryService;
    this.router = Router;
    this.dialogService = FdsDialogService;
};

NfRegistryWorkflowAdministration.prototype = {
    constructor: NfRegistryWorkflowAdministration,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.route.params
            .subscribe(function () {
                self.nfRegistryService.adminPerspective = 'workflow';
                // TODO: implement certifications
                // self.nfRegistryService.getCertifications(self.nfRegistryService.registry.id).then(function(certifications) {
                //     self.nfRegistryService.certifications = self.nfRegistryService.filteredCertifications = certifications;
                //     self.nfRegistryService.filterCertifications();
                // });

                self.nfRegistryService.getBuckets(self.nfRegistryService.registry.id).then(function (buckets) {
                    self.nfRegistryService.buckets = self.nfRegistryService.filteredBuckets = buckets;
                    self.nfRegistryService.filterBuckets();
                });

            });

    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.adminPerspective = '';
        this.nfRegistryService.certifications = this.nfRegistryService.filteredCertifications = [];
        this.nfRegistryService.buckets = [];
        this.nfRegistryService.filteredBuckets = [];
        this.autoCompleteBuckets = [];
    },

    /**
     * Execute the given bucket action.
     *
     * @param action        The action object.
     * @param bucket        The bucket object the `action` will act upon.
     */
    execute: function (action, bucket) {
        var self = this;
        bucket.checked = !bucket.checked;
        switch (action.name.toLowerCase()) {
            case 'delete':
                this.dialogService.openConfirm({
                    title: 'Delete Bucket',
                    message: 'All versions of all flows will be deleted.',
                    cancelButton: 'Cancel',
                    acceptButton: 'Delete',
                    acceptButtonColor: 'fds-warn'
                }).afterClosed().subscribe(
                    function (accept) {
                        if (accept) {
                            self.nfRegistryService.deleteBucket(bucket.id);
                        }
                    });
                break;
            case 'permissions':
                this.router.navigateByUrl('/nifi-registry/administration/' + this.nfRegistryService.registry.id + '/workflow(' + action.type + ':bucket/' + action.name + '/' + bucket.id + ')');
                break;
            default:
                break;
        }
    }
};

NfRegistryWorkflowAdministration.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-workflow-administration.html!text'),
        animations: [nfRegistryAnimations.slideInLeftAnimation],
        host: {
            '[@routeAnimation]': 'routeAnimation'
        }
    })
];

NfRegistryWorkflowAdministration.parameters = [NfRegistryService, ngRouter.ActivatedRoute, ngRouter.Router, fdsDialogsModule.FdsDialogService];

module.exports = NfRegistryWorkflowAdministration;
