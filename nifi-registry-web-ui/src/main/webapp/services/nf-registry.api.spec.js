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
var ngHttpTesting = require('@angular/http/testing');
var ngCommon = require('@angular/common');
var FdsDemo = require('nifi-registry/components/fluid-design-system/fds-demo.js');
var NfRegistry = require('nifi-registry/nf-registry.js');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfPageNotFoundComponent = require('nifi-registry/components/page-not-found/nf-registry-page-not-found.js');
var NfRegistryExplorer = require('nifi-registry/components/explorer/nf-registry-explorer.js');
var NfRegistryAdministration = require('nifi-registry/components/administration/nf-registry-administration.js');
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
var ngHttp = require('@angular/http');

describe('NfRegistry Service API w/ Angular testing utils', function () {
    var comp;
    var fixture;
    var nfRegistryService;

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
                NfRegistryService,
                NfRegistryApi,
                {
                    provide: ngCommon.APP_BASE_HREF,
                    useValue: '/'
                },
                {
                    provide: ngHttp.XHRBackend,
                    useClass: ngHttpTesting.MockBackend
                }
            ],
            bootstrap: [NfRegistry]
        });
    });

    it('should GET droplet snapshot metadata (verbose = true).', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {
            snapshotMetadata: [
                {bucketIdentifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', version: 999}
            ]
        };
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getDropletSnapshotMetadata('flow/test', true).subscribe(function (snapshotMetadata) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/flow/test?verbose=true');
            expect(snapshotMetadata.length).toBe(1);
            expect(snapshotMetadata[0].bucketIdentifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(snapshotMetadata[0].version).toEqual(999);
        });
    }));

    it('should GET droplet snapshot metadata (verbose = false).', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {
            snapshotMetadata: [
                {bucketIdentifier: '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', version: 999}
            ]
        };
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getDropletSnapshotMetadata('flow/test', false).subscribe(function (snapshotMetadata) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/flow/test');
            expect(snapshotMetadata.length).toBe(1);
            expect(snapshotMetadata[0].bucketIdentifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(snapshotMetadata[0].version).toEqual(999);
        });
    }));

    it('should GET droplet by type and ID (verbose = false).', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {
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
        };
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getDroplet('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', 'flows', '2e04b4fb-9513-47bb-aa74-1ae34616bfdc', false).subscribe(function (droplet) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/buckets/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc/flows/2e04b4fb-9513-47bb-aa74-1ae34616bfdc');
            expect(droplet.identifier).toEqual('2e04b4fb-9513-47bb-aa74-1ae34616bfdc');
            expect(droplet.type).toEqual('FLOW');
            expect(droplet.name).toEqual('Flow #1');
        });
    }));

    it('should GET droplet by type and ID (verbose = true).', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {
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
        };
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getDroplet('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', 'flows', '2e04b4fb-9513-47bb-aa74-1ae34616bfdc', true).subscribe(function (droplet) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/buckets/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc/flows/2e04b4fb-9513-47bb-aa74-1ae34616bfdc?verbose=true');
            expect(droplet.identifier).toEqual('2e04b4fb-9513-47bb-aa74-1ae34616bfdc');
            expect(droplet.type).toEqual('FLOW');
            expect(droplet.name).toEqual('Flow #1');
        });
    }));

    it('should GET all droplets across all buckets.', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = [{
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
        }];
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getDroplets().subscribe(function (droplets) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/items');
            expect(droplets.length).toBe(2);
            expect(droplets[0].bucketIdentifier).toEqual('9q7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(droplets[1].bucketIdentifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(droplets[0].name).toEqual('Flow #1');
            expect(droplets[1].name).toEqual('Flow #2');
        });
    }));

    it('should GET all droplets across a single bucket.', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = [{
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
        }];
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getDroplets('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc').subscribe(function (droplets) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/items/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(droplets.length).toBe(1);
            expect(droplets[0].bucketIdentifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(droplets[0].name).toEqual('Flow #1');
        });
    }));

    it('should DELETE a droplet.', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'delete').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {};
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.deleteDroplet('flows/1234').subscribe(function () {
            //assertions
            var deleteDropletCall = nfRegistryService.api.http.delete.calls.first()
            expect(deleteDropletCall.args[0]).toBe('/nifi-registry-api/flows/1234');
        });
    }));

    it('should POST to create a new bucket.', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'post').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {
            identifier: '1234'
        };
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.createBucket('test').subscribe(function (bucket) {
            //assertions
            var createDropletCall = nfRegistryService.api.http.post.calls.first();
            expect(createDropletCall.args[0]).toBe('/nifi-registry-api/buckets');
            expect(createDropletCall.args[1].name).toBe('test');
            expect(bucket.identifier).toBe('1234');
        });
    }));

    it('should DELETE a bucket.', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'delete').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {};
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.deleteBucket('1234').subscribe(function () {
            //assertions
            var deleteBucketCall = nfRegistryService.api.http.delete.calls.first()
            expect(deleteBucketCall.args[0]).toBe('/nifi-registry-api/buckets/1234');
        });
    }));

    it('should GET bucket by ID (verbose = false).', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {
            'identifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            'name': 'Bucket #1'
        };
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getBucket('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', false).subscribe(function (bucket) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/buckets/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(bucket.identifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(bucket.name).toEqual('Bucket #1');
        });
    }));

    it('should GET bucket by ID (verbose = true).', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = {
            'identifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            'name': 'Bucket #1'
        };
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getBucket('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc', true).subscribe(function (bucket) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/buckets/2f7f9e54-dc09-4ceb-aa58-9fe581319cdc?verbose=true');
            expect(bucket.identifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(bucket.name).toEqual('Bucket #1');
        });
    }));

    it('should GET metadata for all buckets in the registry for which the client is authorized.', ngCoreTesting.inject([ngHttp.XHRBackend], function (mockBackend) {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistry);
        fixture.detectChanges();
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);

        //Spy
        spyOn(nfRegistryService.api.http, 'get').and.callThrough();

        //Setup the mock backend to return mock data
        var mockResponse = [{
            'identifier': '2f7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            'name': 'Bucket #1'
        }];
        mockBackend.connections.subscribe(function (connection) {
            // This is called every time someone subscribes to an http call
            connection.mockRespond(new ngHttp.Response(new ngHttp.ResponseOptions({
                body: JSON.stringify(mockResponse)
            })));
        });

        // The function to test
        nfRegistryService.api.getBuckets().subscribe(function (buckets) {
            //assertions
            expect(nfRegistryService.api.http.get).toHaveBeenCalledWith('/nifi-registry-api/buckets');
            expect(buckets[0].identifier).toEqual('2f7f9e54-dc09-4ceb-aa58-9fe581319cdc');
            expect(buckets[0].name).toEqual('Bucket #1');
        });
    }));
});