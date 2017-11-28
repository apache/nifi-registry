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

describe('NfRegistryWorkflowAdministration Component', function () {
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
                ngCommonHttp.HttpClientModule,
                fdsCore,
                NfRegistryRoutes
            ],
            declarations: [
                FdsDemo,
                NfRegistry,
                NfRegistryExplorer,
                NfRegistryAdministration,
                NfRegistryUsersAdministration,
                NfRegistryUserDetails,
                NfRegistryUserPermissions,
                NfRegistryUserGroupPermissions,
                NfRegistryBucketPermissions,
                NfRegistryAddUser,
                NfRegistryWorkflowAdministration,
                NfRegistryCreateBucket,
                NfRegistryGridListViewer,
                NfRegistryBucketGridListViewer,
                NfRegistryDropletGridListViewer,
                NfPageNotFoundComponent
            ],
            entryComponents: [
                NfRegistryCreateBucket
            ],
            providers: [
                NfRegistryService,
                NfRegistryAuthService,
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
                        params: rxjs.Observable.of({})
                    }
                }
            ]
        });

        fixture = ngCoreTesting.TestBed.createComponent(NfRegistryWorkflowAdministration);

        // test instance
        comp = fixture.componentInstance;

        // from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);
        nfRegistryApi = ngCoreTesting.TestBed.get(NfRegistryApi);
        de = fixture.debugElement.query(ngPlatformBrowser.By.css('#nifi-registry-workflow-administration-perspective-buckets-container'));
        el = de.nativeElement;

        // Spy
        spyOn(nfRegistryApi, 'ticketExchange').and.callFake(function () {}).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryService, 'loadCurrentUser').and.callFake(function () {}).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryService.api, 'getDroplets').and.callFake(function () {
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
        spyOn(nfRegistryApi, 'getBuckets').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of([{name: 'Bucket #1'}]));
        spyOn(nfRegistryApi, 'createBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({name: 'Newly Created Bucket'}));
        spyOn(nfRegistryService, 'filterBuckets');
    });

    it('should have a defined component', ngCoreTesting.async(function () {
        fixture.detectChanges();
        fixture.whenStable().then(function () { // wait for async getBuckets
            fixture.detectChanges();

            //assertions
            expect(comp).toBeDefined();
            expect(de).toBeDefined();
            expect(nfRegistryService.adminPerspective).toBe('workflow');
            expect(nfRegistryService.buckets[0].name).toEqual('Bucket #1');
            expect(nfRegistryService.buckets.length).toBe(1);
            expect(nfRegistryService.filterBuckets).toHaveBeenCalled();
        });
    }));

    it('should open a dialog to create a new bucket', function () {
        spyOn(comp.dialog, 'open')
        fixture.detectChanges();

        // the function to test
        comp.createBucket();

        //assertions
        expect(comp.dialog.open).toBeDefined();
    });

    it('should destroy the component', ngCoreTesting.fakeAsync(function () {
        fixture.detectChanges();
        // wait for async getBucket call
        ngCoreTesting.tick();
        fixture.detectChanges();

        // The function to test
        comp.ngOnDestroy();

        //assertions
        expect(nfRegistryService.adminPerspective).toBe('');
        expect(nfRegistryService.buckets.length).toBe(0);
        expect(nfRegistryService.filteredBuckets.length).toBe(0);
        expect(nfRegistryService.allBucketsSelected).toBe(false);
    }));
});