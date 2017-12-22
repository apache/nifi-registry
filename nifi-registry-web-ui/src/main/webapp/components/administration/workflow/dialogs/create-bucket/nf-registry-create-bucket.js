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
var ngMaterial = require('@angular/material');
var fdsSnackBarsModule = require('@fluid-design-system/snackbars');

/**
 * NfRegistryCreateBucket constructor.
 *
 * @param nfRegistryApi         The api service.
 * @param fdsSnackBarService    The FDS snack bar service module.
 * @param nfRegistryService     The nf-registry.service module.
 * @param matDialogRef          The angular material dialog ref.
 * @constructor
 */
function NfRegistryCreateBucket(nfRegistryApi, fdsSnackBarService, nfRegistryService, matDialogRef) {
    // Services
    this.snackBarService = fdsSnackBarService;
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.dialogRef = matDialogRef;
    // local state
    this.keepDialogOpen = false;
};

NfRegistryCreateBucket.prototype = {
    constructor: NfRegistryCreateBucket,

    /**
     * Create a new bucket.
     *
     * @param newBucketInput     The newBucketInput element.
     */
    createBucket: function (newBucketInput) {
        var self = this;
        this.nfRegistryApi.createBucket(newBucketInput.value).subscribe(function (bucket) {
            if (!bucket.error) {
                self.nfRegistryService.buckets.push(bucket);
                self.nfRegistryService.filterBuckets();
                self.nfRegistryService.allBucketsSelected = false;
                if (self.keepDialogOpen !== true) {
                    self.dialogRef.close();
                }
                self.snackBarService.openCoaster({
                    title: 'Success',
                    message: 'Bucket has been added.',
                    verticalPosition: 'bottom',
                    horizontalPosition: 'right',
                    icon: 'fa fa-check-circle-o',
                    color: '#1EB475',
                    duration: 3000
                });
            } else {
                self.dialogRef.close();
            }
        })
    },

    /**
     * Cancel creation of a new bucket and close dialog.
     */
    cancel: function () {
        this.dialogRef.close();
    }
};

NfRegistryCreateBucket.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-create-bucket.html!text')
    })
];

NfRegistryCreateBucket.parameters = [
    NfRegistryApi,
    fdsSnackBarsModule.FdsSnackBarService,
    NfRegistryService,
    ngMaterial.MatDialogRef
];

module.exports = NfRegistryCreateBucket;
