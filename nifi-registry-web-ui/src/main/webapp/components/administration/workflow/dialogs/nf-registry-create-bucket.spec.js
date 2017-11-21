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

var NfRegistryRoutes = require('nifi-registry/nf-registry.routes.js');
var ngCoreTesting = require('@angular/core/testing');
var ngCommon = require('@angular/common');
var ngRouter = require('@angular/router');
var ngPlatformBrowser = require('@angular/platform-browser');
var FdsDemo = require('nifi-registry/components/fluid-design-system/fds-demo.js');
var NfRegistry = require('nifi-registry/nf-registry.js');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfPageNotFoundComponent = require('nifi-registry/components/page-not-found/nf-registry-page-not-found.js');
var NfRegistryExplorer = require('nifi-registry/components/explorer/nf-registry-explorer.js');
var NfRegistryAdministration = require('nifi-registry/components/administration/nf-registry-administration.js');
var NfRegistryUsersAdministration = require('nifi-registry/components/administration/users/nf-registry-users-administration.js');
var NfRegistryAddUser = require('nifi-registry/components/administration/users/dialogs/add-user/nf-registry-add-user.js');
var NfRegistryUserDetails = require('nifi-registry/components/administration/users/details/nf-registry-user-details.js');
var NfRegistryUserPermissions = require('nifi-registry/components/administration/users/permissions/nf-registry-user-permissions.js');
var NfRegistryUserGroupPermissions = require('nifi-registry/components/administration/user-group/permissions/nf-registry-user-group-permissions.js');
var NfRegistryBucketPermissions = require('nifi-registry/components/administration/workflow/buckets/permissions/nf-registry-bucket-permissions.js');
var NfRegistryWorkflowAdministration = require('nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js');
var NfRegistryCreateBucket = require('nifi-registry/components/administration/workflow/dialogs/nf-registry-create-bucket.js');
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js');
var fdsCore = require('@fluid-design-system/core');
var ngMoment = require('angular2-moment');
var rxjs = require('rxjs/Rx');
var ngHttp = require('@angular/http');
var ngCommonHttp = require('@angular/common/http');
var NfRegistryTokenInterceptor = require('nifi-registry/services/nf-registry.token.interceptor.js');
var NfRegistryAuthService = require('nifi-registry/services/nf-registry.auth.service.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');

describe('NfRegistryCreateBucket Component', function () {
    var comp;
    var fixture;
    var de;
    var el;
    var nfRegistryService;

    beforeEach(function () {
        nfRegistryService = new NfRegistryService({}, {
            createBucket: function() {}
        }, {}, {});
        comp = new NfRegistryCreateBucket(nfRegistryService, {
            close: function() {}
        })
    });

    it('should create a new bucket and close the dialog', function () {
        // Spy
        spyOn(nfRegistryService.api, 'createBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({name: 'NewBucket'}));
        spyOn(nfRegistryService, 'filterBuckets');
        spyOn(comp.dialogRef, 'close');

        // The function to test
        comp.createBucket({value: 'NewBucket'});

        //assertions
        expect(comp).toBeDefined();
        expect(nfRegistryService.buckets.length).toBe(1);
        expect(nfRegistryService.buckets[0].name).toBe('NewBucket');
        expect(nfRegistryService.filterBuckets).toHaveBeenCalled();
        expect(comp.dialogRef.close).toHaveBeenCalled();
    });

    it('should create a new bucket and keep the dialog open', function () {
        // Spy
        spyOn(nfRegistryService.api, 'createBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({name: 'NewBucket'}));
        spyOn(nfRegistryService, 'filterBuckets');
        spyOn(comp.dialogRef, 'close');

        // setup the component
        comp.keepDialogOpen = true;

        // The function to test
        comp.createBucket({value: 'NewBucket'});

        //assertions
        expect(comp).toBeDefined();
        expect(nfRegistryService.buckets.length).toBe(1);
        expect(nfRegistryService.buckets[0].name).toBe('NewBucket');
        expect(nfRegistryService.filterBuckets).toHaveBeenCalled();
        expect(comp.dialogRef.close.calls.count()).toEqual(0);
    });

    it('should close the dialog', function () {
        // Spy
        spyOn(comp.dialogRef, 'close');

        // The function to test
        comp.cancel();

        //assertions
        expect(comp.dialogRef.close).toHaveBeenCalled();
    });
});