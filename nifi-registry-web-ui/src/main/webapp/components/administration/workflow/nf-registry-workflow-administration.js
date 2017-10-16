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
var NfRegistryCreateBucket = require('nifi-registry/components/administration/workflow/dialogs/nf-registry-create-bucket.js');
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');
var ngRouter = require('@angular/router');
var ngMaterial = require('@angular/material');

/**
 * NfRegistryWorkflowAdministration constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param ActivatedRoute        The angular activated route module.
 * @param matDialogRef          The angular material dialog ref.
 * @constructor
 */
function NfRegistryWorkflowAdministration(nfRegistryService, ActivatedRoute, matDialog) {
    this.route = ActivatedRoute;
    this.nfRegistryService = nfRegistryService;
    this.dialog = matDialog;
    this.bucketActions = [{
        'name': 'permissions',
        'icon': 'fa fa-pencil',
        'tooltip': 'Manage Bucket Policies',
        'type': 'sidenav'
    }, {
        'name': 'Delete',
        'icon': 'fa fa-trash',
        'tooltip': 'Delete Bucket'
    }];
};

NfRegistryWorkflowAdministration.prototype = {
    constructor: NfRegistryWorkflowAdministration,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.route.params
            .switchMap(function (params) {
                self.nfRegistryService.adminPerspective = 'workflow';
                return self.nfRegistryService.api.getBuckets();
            })
            .subscribe(function (buckets) {
                self.nfRegistryService.buckets = buckets;
                self.nfRegistryService.filterBuckets();
            });

    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.adminPerspective = '';
        this.nfRegistryService.buckets = this.nfRegistryService.filteredBuckets = [];
        this.nfRegistryService.allBucketsSelected = false;
    },

    /**
     * Opens the create new bucket dialog.
     */
    createBucket: function () {
        this.dialog.open(NfRegistryCreateBucket);
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

NfRegistryWorkflowAdministration.parameters = [NfRegistryService, ngRouter.ActivatedRoute, ngMaterial.MdDialog];

module.exports = NfRegistryWorkflowAdministration;
