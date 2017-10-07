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
var NfRegistryGeneralAdministration = require('nifi-registry/components/administration/general/nf-registry-general-administration.js');
var NfRegistryUsersAdministration = require('nifi-registry/components/administration/users/nf-registry-users-administration.js');
var NfRegistryAddUser = require('nifi-registry/components/administration/users/add/nf-registry-add-user.js');
var NfRegistryUserDetails = require('nifi-registry/components/administration/users/details/nf-registry-user-details.js');
var NfRegistryUserPermissions = require('nifi-registry/components/administration/users/permissions/nf-registry-user-permissions.js');
var NfRegistryBucketPermissions = require('nifi-registry/components/administration/workflow/buckets/permissions/nf-registry-bucket-permissions.js');
var NfRegistryWorkflowAdministration = require('nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js');
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js');
var fdsCore = require('@fluid-design-system/core');
var ngMoment = require('angular2-moment');
var rxjs = require('rxjs/Rx');
var ngHttp = require('@angular/http');

describe('NfRegistryGridListViewer Component', function () {
    var comp;
    var fixture;
    var de;
    var el;
    var nfRegistryService;
    var nfRegistryApi;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                ngMoment.MomentModule,
                ngHttp.HttpModule,
                ngHttp.JsonpModule,
                fdsCore,
                NfRegistryRoutes
            ],
            declarations: [
                FdsDemo,
                NfRegistry,
                NfRegistryExplorer,
                NfRegistryAdministration,
                NfRegistryGeneralAdministration,
                NfRegistryUsersAdministration,
                NfRegistryUserDetails,
                NfRegistryUserPermissions,
                NfRegistryBucketPermissions,
                NfRegistryAddUser,
                NfRegistryWorkflowAdministration,
                NfRegistryGridListViewer,
                NfRegistryBucketGridListViewer,
                NfRegistryDropletGridListViewer,
                NfPageNotFoundComponent
            ],
            providers: [
                NfRegistryApi,
                NfRegistryService, {
                    provide: ngCommon.APP_BASE_HREF,
                    useValue: '/'
                }, {
                    provide: ngRouter.ActivatedRoute,
                    useValue: {
                        params: rxjs.Observable.of({})
                    }
                }
            ]
        });

        fixture = ngCoreTesting.TestBed.createComponent(NfRegistryGridListViewer);

        // test instance
        comp = fixture.componentInstance;

        // from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);
        nfRegistryApi = ngCoreTesting.TestBed.get(NfRegistryApi);

        // because the NfRegistryGridListViewer component is a nested route component we need to set up the nfRegistryService service manually
        nfRegistryService.perspective = 'explorer';

        // Spy
        spyOn(nfRegistryApi, 'getBuckets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of([{
            identifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            name: 'Bucket #1'
        }]));
        spyOn(nfRegistryService, 'filterDroplets');
    });

    it('should have a defined component', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getDroplets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of([{
            "identifier": "2e04b4fb-9513-47bb-aa74-1ae34616bfdc",
            "name": "Flow #1",
            "description": "This is flow #1",
            "bucketIdentifier": "2f7f9e54-dc09-4ceb-aa58-9fe581319cdc",
            "createdTimestamp": 1505931890999,
            "modifiedTimestamp": 1505931890999,
            "type": "FLOW",
            "snapshotMetadata": null,
            "link": {
                "params": {
                    "rel": "self"
                },
                "href": "flows/2e04b4fb-9513-47bb-aa74-1ae34616bfdc"
            }
        }]));
        // 1st change detection triggers ngOnInit which makes getBuckets and getDroplets calls
        fixture.detectChanges();
        // wait for async getBuckets and getDroplets calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getBuckets and getDroplets calls
        fixture.detectChanges();

        //assertions
        expect(comp).toBeDefined();
        expect(nfRegistryService.explorerViewType).toBe('grid-list');
        expect(nfRegistryService.breadCrumbState).toBe('in');
        expect(nfRegistryService.buckets[0].name).toEqual('Bucket #1');
        expect(nfRegistryService.buckets.length).toBe(1);
        expect(nfRegistryService.droplets[0].name).toEqual('Flow #1');
        expect(nfRegistryService.droplets.length).toBe(1);
        expect(nfRegistryApi.getDroplets).toHaveBeenCalled();
        expect(nfRegistryApi.getBuckets).toHaveBeenCalled();
        expect(nfRegistryService.filterDroplets).toHaveBeenCalled();
    }));

    it('should destroy the component', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getDroplets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of([]));
        // 1st change detection triggers ngOnInit which makes getBuckets and getDroplets calls
        fixture.detectChanges();
        // wait for async getBuckets and getDroplets calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getBuckets and getDroplets calls
        fixture.detectChanges();

        // The function to test
        comp.ngOnDestroy();

        //assertions
        expect(nfRegistryService.explorerViewType).toBe('');
        expect(nfRegistryService.buckets.length).toBe(0);
        expect(nfRegistryService.droplets.length).toBe(0);
        expect(nfRegistryService.filteredDroplets.length).toBe(0);
        expect(nfRegistryService.breadCrumbState).toBe('out');
    }));
});