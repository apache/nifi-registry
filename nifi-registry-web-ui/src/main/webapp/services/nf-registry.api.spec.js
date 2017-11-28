/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the 'License'); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var NfRegistryRoutes = require('nifi-registry/nf-registry.routes.js');
var ngCoreTesting = require('@angular/core/testing');
var ngCommonHttpTesting = require('@angular/common/http/testing');
var ngCommon = require('@angular/common');
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
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js');
var fdsCore = require('@fluid-design-system/core');
var ngMoment = require('angular2-moment');
var ngHttp = require('@angular/http');
var rxjs = require('rxjs/Rx');
var ngCommonHttp = require('@angular/common/http');
var NfRegistryTokenInterceptor = require('nifi-registry/services/nf-registry.token.interceptor.js');
var NfRegistryAuthService = require('nifi-registry/services/nf-registry.auth.service.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');

describe('NfRegistry Service API w/ Angular testing utils', function () {
    var comp;
    var fixture;
    var nfRegistryApi;
    var nfRegistryService;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                ngMoment.MomentModule,
                ngHttp.HttpModule,
                ngHttp.JsonpModule,
                ngCommonHttp.HttpClientModule,
                fdsCore,
                NfRegistryRoutes,
                ngCommonHttpTesting.HttpClientTestingModule
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
                NfRegistryGridListViewer,
                NfRegistryBucketGridListViewer,
                NfRegistryDropletGridListViewer,
                NfPageNotFoundComponent
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
                }
            ],
            bootstrap: [NfRegistry]
        });
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);
        nfRegistryApi = ngCoreTesting.TestBed.get(NfRegistryApi);
        spyOn(nfRegistryApi, 'ticketExchange').and.callFake(function () {}).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryService, 'loadCurrentUser').and.callFake(function () {}).and.returnValue(rxjs.Observable.of({}));
    });

    it('should GET droplet snapshot metadata.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        // The function to test
        nfRegistryApi.getDropletSnapshotMetadata('flow/test').subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/flow/test/versions');
            expect(req.request.method).toEqual('GET');

            // Next, fulfill the request by transmitting a response.
            req.flush({
                snapshotMetadata: [
                    {bucketIdentifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', version: 999}
                ]
            });
            httpMock.verify();
        });
    }));

    it('should GET droplet by type and ID.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        // The function to test
        nfRegistryApi.getDroplet('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', 'flows', '2e04b4fb-9513-47bb-aa74-1ae34616bfdc').subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/buckets/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc/flows/2e04b4fb-9513-47bb-aa74-1ae34616bfdc');
            expect(req.request.method).toEqual('GET');

            // Next, fulfill the request by transmitting a response.
            req.flush({
                'identifier': '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
                'name': 'Flow #1',
                'description': 'This is flow #1',
                'bucketIdentifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
                'createdTimestamp': 1505931890999,
                'modifiedTimestamp': 1505931890999,
                'type': 'FLOW',
                'snapshotMetadata': null,
                'link': {
                    'params': {
                        'rel': 'self'
                    },
                    'href': 'flows/2e04b4fb-9513-47bb-aa74-1ae34616bfdc'
                }
            });
            httpMock.verify();
            expect(droplet.identifier).toEqual('2e04b4fb-9513-47bb-aa74-1ae34616bfdc');
            expect(droplet.type).toEqual('FLOW');
            expect(droplet.name).toEqual('Flow #1');
        });
    }));

    it('should GET all droplets across all buckets.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        // The function to test
        nfRegistryApi.getDroplets().subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/items');
            expect(req.request.method).toEqual('GET');

            // Next, fulfill the request by transmitting a response.
            req.flush([{
                'identifier': '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
                'name': 'Flow #1',
                'description': 'This is flow #1',
                'bucketIdentifier': '9q7f9e54-dc09-4ceb-aa58-9fe581319cdc',
                'createdTimestamp': 1505931890999,
                'modifiedTimestamp': 1505931890999,
                'type': 'FLOW',
                'snapshotMetadata': null,
                'link': {
                    'params': {
                        'rel': 'self'
                    },
                    'href': 'flows/2e04b4fb-9513-47bb-aa74-1ae34616bfdc'
                }
            }, {
                'identifier': '5d04b4fb-9513-47bb-aa74-1ae34616bfdc',
                'name': 'Flow #2',
                'description': 'This is flow #2',
                'bucketIdentifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
                'createdTimestamp': 1505931890999,
                'modifiedTimestamp': 1505931890999,
                'type': 'FLOW',
                'snapshotMetadata': null,
                'link': {
                    'params': {
                        'rel': 'self'
                    },
                    'href': 'flows/5d04b4fb-9513-47bb-aa74-1ae34616bfdc'
                }
            }]);
            httpMock.verify();
            expect(droplets.length).toBe(2);
            expect(droplets[0].bucketIdentifier).toEqual('9q7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(droplets[1].bucketIdentifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(droplets[0].name).toEqual('Flow #1');
            expect(droplets[1].name).toEqual('Flow #2');
        });
    }));

    it('should GET all droplets across a single bucket.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        // The function to test
        nfRegistryApi.getDroplets('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc').subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/items/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(req.request.method).toEqual('GET');

            // Next, fulfill the request by transmitting a response.
            req.flush([{
                'identifier': '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
                'name': 'Flow #1',
                'description': 'This is flow #1',
                'bucketIdentifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
                'createdTimestamp': 1505931890999,
                'modifiedTimestamp': 1505931890999,
                'type': 'FLOW',
                'snapshotMetadata': null,
                'link': {
                    'params': {
                        'rel': 'self'
                    },
                    'href': 'flows/2e04b4fb-9513-47bb-aa74-1ae34616bfdc'
                }
            }]);
            httpMock.verify();
            expect(droplets.length).toBe(1);
            expect(droplets[0].bucketIdentifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(droplets[0].name).toEqual('Flow #1');
        });
    }));

    it('should DELETE a droplet.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        // The function to test
        nfRegistryApi.deleteDroplet('flows/1234').subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/flows/1234');
            expect(req.request.method).toEqual('DELETE');

            // Next, fulfill the request by transmitting a response.
            req.flush({});
            httpMock.verify();
        });
    }));

    it('should POST to create a new bucket.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        //Spy
        spyOn(nfRegistryApi.http, 'post').and.callThrough();

        // The function to test
        nfRegistryApi.createBucket('test').subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/buckets');
            expect(req.request.method).toEqual('POST');

            // Next, fulfill the request by transmitting a response.
            req.flush({
                identifier: '1234'
            });
            httpMock.verify();
            expect(createDropletCall.args[1].name).toBe('test');
            expect(bucket.identifier).toBe('1234');
        });
    }));

    it('should DELETE a bucket.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        //Spy
        spyOn(nfRegistryApi.http, 'post').and.callThrough();

        // The function to test
        nfRegistryApi.deleteBucket('1234').subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/buckets/1234');
            expect(req.request.method).toEqual('DELETE');

            // Next, fulfill the request by transmitting a response.
            req.flush({});
            httpMock.verify();
        });
    }));

    it('should GET bucket by ID.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {

        // The function to test
        nfRegistryApi.getBucket('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc').subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/buckets/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(req.request.method).toEqual('GET');

            // Next, fulfill the request by transmitting a response.
            req.flush({
                'identifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
                'name': 'Bucket #1'
            });
            httpMock.verify();
            expect(response.identifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(response.name).toEqual('Bucket #1');
        });
    }));

    it('should GET metadata for all buckets in the registry for which the client is authorized.', ngCoreTesting.inject([ngCommonHttpTesting.HttpTestingController], function (httpMock) {
        // The function to test
        nfRegistryApi.getBuckets().subscribe(function(response) {
            var req = httpMock.expectOne('/nifi-registry-api/access/token/kerberos');
            req.flush({});
            req = httpMock.expectOne('/nifi-registry-api/access');
            req.flush({});
            httpMock.verify();
            // the request it made
            req = httpMock.expectOne('/nifi-registry-api/buckets');
            expect(req.request.method).toEqual('GET');

            // Next, fulfill the request by transmitting a response.
            req.flush([{
                'identifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
                'name': 'Bucket #1'
            }]);
            httpMock.verify();
            expect(response[0].identifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(response[0].name).toEqual('Bucket #1');
        });
    }));
});