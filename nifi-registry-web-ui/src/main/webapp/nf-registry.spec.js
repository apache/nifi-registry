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
var FdsDemo = require('nifi-registry/components/fluid-design-system/fds-demo.js');
var NfRegistry = require('nifi-registry/nf-registry.js');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfPageNotFoundComponent = require('nifi-registry/components/page-not-found/nf-registry-page-not-found.js');
var NfRegistryExplorer = require('nifi-registry/components/explorer/nf-registry-explorer.js');
var NfRegistryExplorerGridListViewer = require('nifi-registry/components/explorer/grid-list/nf-registry-explorer-grid-list-viewer.js');
var NfRegistryAdministration = require('nifi-registry/components/administration/nf-registry-administration.js');
var NfRegistryGeneralAdministration = require('nifi-registry/components/administration/general/nf-registry-general-administration.js');
var NfRegistryUsersAdministration = require('nifi-registry/components/administration/users/nf-registry-users-administration.js');
var NfRegistryAddUser = require('nifi-registry/components/administration/users/add/nf-registry-add-user.js');
var NfRegistryUserDetails = require('nifi-registry/components/administration/users/details/nf-registry-user-details.js');
var NfRegistryUserPermissions = require('nifi-registry/components/administration/users/permissions/nf-registry-user-permissions.js');
var NfRegistryBucketDetails = require('nifi-registry/components/administration/workflow/buckets/details/nf-registry-bucket-details.js');
var NfRegistryBucketPermissions = require('nifi-registry/components/administration/workflow/buckets/permissions/nf-registry-bucket-permissions.js');
var NfRegistryWorkflowAdministration = require('nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js');
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/bucket/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/bucket/droplet/nf-registry-droplet-grid-list-viewer.js');
var fdsCore = require('@fluid-design-system/core');

describe('NfRegistry Component', function () {
    var comp;
    var fixture;

    beforeEach(ngCoreTesting.async(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                fdsCore,
                NfRegistryRoutes
            ],
            declarations: [FdsDemo, NfRegistry, NfRegistryExplorer, NfRegistryExplorerGridListViewer, NfRegistryAdministration, NfRegistryGeneralAdministration, NfRegistryUsersAdministration, NfRegistryUserDetails, NfRegistryUserPermissions, NfRegistryBucketDetails, NfRegistryBucketPermissions, NfRegistryAddUser, NfRegistryWorkflowAdministration, NfRegistryGridListViewer, NfRegistryBucketGridListViewer, NfRegistryDropletGridListViewer, NfPageNotFoundComponent],
            providers: [NfRegistryService, {provide: ngCommon.APP_BASE_HREF, useValue: '/'}],
            bootstrap: [NfRegistry]
        });
    }));

    it('should create component', function () {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;
        expect(comp).toBeDefined()
    });
});