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
var NfRegistry = require('nifi-registry/nf-registry.js');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfPageNotFoundComponent = require('nifi-registry/components/page-not-found/nf-registry-page-not-found.js');
var NfRegistryExplorer = require('nifi-registry/components/explorer/nf-registry-explorer.js');
var NfRegistryAdministration = require('nifi-registry/components/administration/nf-registry-administration.js');
var NfRegistryUsersAdministration = require('nifi-registry/components/administration/users/nf-registry-users-administration.js');
var NfRegistryAddUser = require('nifi-registry/components/administration/users/dialogs/add-user/nf-registry-add-user.js');
var NfRegistryManageUser = require('nifi-registry/components/administration/users/sidenav/manage-user/nf-registry-manage-user.js');
var NfRegistryManageGroup = require('nifi-registry/components/administration/users/sidenav/manage-group/nf-registry-manage-group.js');
var NfRegistryManageBucket = require('nifi-registry/components/administration/workflow/sidenav/manage-bucket/nf-registry-manage-bucket.js');
var NfRegistryWorkflowAdministration = require('nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js');
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js');
var fdsCore = require('@flow-design-system/core');
var ngMoment = require('angular2-moment');
var rxjs = require('rxjs/Rx');
var ngCommonHttp = require('@angular/common/http');
var NfRegistryTokenInterceptor = require('nifi-registry/services/nf-registry.token.interceptor.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');
var NfLoginComponent = require('nifi-registry/components/login/nf-registry-login.js');
var NfUserLoginComponent = require('nifi-registry/components/login/dialogs/nf-registry-user-login.js');

describe('NfRegistryDropletGridListViewer Component', function () {
    var comp;
    var fixture;
    var nfRegistryService;
    var nfRegistryApi;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                ngMoment.MomentModule,
                ngCommonHttp.HttpClientModule,
                fdsCore,
                NfRegistryRoutes
            ],
            declarations: [
                NfRegistry,
                NfRegistryExplorer,
                NfRegistryAdministration,
                NfRegistryUsersAdministration,
                NfRegistryManageUser,
                NfRegistryManageGroup,
                NfRegistryManageBucket,
                NfRegistryAddUser,
                NfRegistryWorkflowAdministration,
                NfRegistryGridListViewer,
                NfRegistryBucketGridListViewer,
                NfRegistryDropletGridListViewer,
                NfPageNotFoundComponent,
                NfLoginComponent,
                NfUserLoginComponent
            ],
            providers: [
                NfRegistryService,
                NfRegistryApi,
                NfStorage,
                {
                    provide: ngCommonHttp.HTTP_INTERCEPTORS,
                    useClass: NfRegistryTokenInterceptor,
                    multi: true
                },
                {
                    provide: ngCommon.APP_BASE_HREF,
                    useValue: '/'
                }, {
                    provide: ngRouter.ActivatedRoute,
                    useValue: {
                        params: rxjs.Observable.of({
                            bucketId: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
                            dropletId: '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
                            dropletType: 'flow'
                        })
                    }
                }
            ]
        });

        fixture = ngCoreTesting.TestBed.createComponent(NfRegistryDropletGridListViewer);

        // test instance
        comp = fixture.componentInstance;

        // from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);
        nfRegistryApi = ngCoreTesting.TestBed.get(NfRegistryApi);

        // because the NfRegistryDropletGridListViewer component is a nested route component we need to set up the nfRegistryService service manually
        nfRegistryService.perspective = 'explorer';
        nfRegistryService.explorerViewType = 'grid-list';

        //Spy
        spyOn(nfRegistryApi, 'ticketExchange').and.callFake(function () {}).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryApi, 'loadCurrentUser').and.callFake(function () {}).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryService, 'filterDroplets');
    });

    it('should have a defined component', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getDroplet').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
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
        }));
        spyOn(nfRegistryApi, 'getBuckets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of([{
            identifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            name: 'Bucket #1'
        }]));
        spyOn(nfRegistryApi, 'getBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            name: 'Bucket #1'
        }));
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
        // 1st change detection triggers ngOnInit which makes getBuckets, getBucket, getDroplet, and getDroplets calls
        fixture.detectChanges();
        // wait for async getBuckets, getBucket, getDroplet, and getDroplets calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getBuckets, getBucket, getDroplet, and getDroplets calls
        fixture.detectChanges();

        //assertions
        expect(comp).toBeDefined();
        expect(nfRegistryService.breadCrumbState).toBe('in');
        expect(nfRegistryService.inProgress).toBe(false);
        expect(nfRegistryService.bucket.name).toEqual('Bucket #1');
        expect(nfRegistryService.buckets[0].name).toEqual('Bucket #1');
        expect(nfRegistryService.buckets.length).toBe(1);
        expect(nfRegistryService.droplet.name).toEqual('Flow #1');
        expect(nfRegistryService.droplets[0].name).toEqual('Flow #1');
        expect(nfRegistryService.droplets.length).toBe(1);
        expect(nfRegistryApi.getBuckets).toHaveBeenCalled();
        expect(nfRegistryService.filterDroplets).toHaveBeenCalled();
        expect(nfRegistryService.filterDroplets.calls.count()).toBe(1);

        var getDropletsCall = nfRegistryApi.getDroplets.calls.first();
        expect(getDropletsCall.args[0]).toBe('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');

        var getDropletCall = nfRegistryApi.getDroplet.calls.first();
        expect(getDropletCall.args[0]).toBe('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
        expect(getDropletCall.args[1]).toBe('flow');
        expect(getDropletCall.args[2]).toBe('2e04b4fb-9513-47bb-aa74-1ae34616bfdc');

        var getBucketCall = nfRegistryApi.getBucket.calls.first()
        expect(getBucketCall.args[0]).toBe('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
    }));

    it('should FAIL to get buckets, get bucket, get droplets, and get droplet and then redirect to view all buckets', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getBuckets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404
        }));
        spyOn(nfRegistryApi, 'getBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404
        }));
        spyOn(nfRegistryApi, 'getDroplets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404
        }));
        spyOn(nfRegistryApi, 'getDroplet').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404
        }));
        spyOn(comp.router, 'navigateByUrl').and.callFake(function () {
        });
        // 1st change detection triggers ngOnInit which makes getBuckets, getBucket, and getDroplets calls
        fixture.detectChanges();
        // wait for async getBuckets, getBucket, and getDroplets calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getBuckets, getBucket, and getDroplets calls
        fixture.detectChanges();

        //assertions
        var routerCall = comp.router.navigateByUrl.calls.first();
        expect(routerCall.args[0]).toBe('/nifi-registry/explorer/grid-list');
        expect(comp.router.navigateByUrl.calls.count()).toBe(2);
    }));

    it('should destroy the component', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getDroplet').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
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
        }));
        spyOn(nfRegistryApi, 'getBuckets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of([{
            identifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            name: 'Bucket #1'
        }]));
        spyOn(nfRegistryApi, 'getBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            name: 'Bucket #1'
        }));
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
        // 1st change detection triggers ngOnInit which makes getBuckets, getBucket, getDroplet, and getDroplets calls
        fixture.detectChanges();
        // wait for async getBuckets, getBucket, getDroplet, and getDroplets calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getBuckets, getBucket, getDroplet, and getDroplets calls
        fixture.detectChanges();

        // The function to test
        comp.ngOnDestroy();

        //assertions
        expect(nfRegistryService.explorerViewType).toBe('');
        expect(nfRegistryService.filteredDroplets.length).toBe(0);
        expect(nfRegistryService.breadCrumbState).toBe('out');
    }));
});