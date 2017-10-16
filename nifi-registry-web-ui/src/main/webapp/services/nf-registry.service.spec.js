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
var rxjs = require('rxjs/Rx');
var fdsDialogsModule = require('@fluid-design-system/dialogs');
var ngRouter = require('@angular/router');

describe('NfRegistry Service isolated unit tests', function () {
    var comp;
    var fixture;
    var nfRegistryService;

    beforeEach(function () {
        nfRegistryService = new NfRegistryService({}, {}, {}, {});
    });

    it('should set the breadcrumb animation state', function () {
        // The function to test
        nfRegistryService.setBreadcrumbState('test');

        //assertions
        expect(nfRegistryService.breadCrumbState).toBe('test');
    });

    it('should get the `Name (z - a)` sort by label', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.dropletColumns[0].active = true;

        // The function to test
        var label = nfRegistryService.getSortByLabel();

        //assertions
        expect(label).toBe('Name (z - a)');
    });

    it('should get the `Name (a - z)` sort by label', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.dropletColumns[0].active = true;
        nfRegistryService.dropletColumns[0].sortOrder = 'ASC';

        // The function to test
        var label = nfRegistryService.getSortByLabel();

        //assertions
        expect(label).toBe('Name (a - z)');
    });

    it('should get the `Oldest (update)` sort by label', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.dropletColumns[1].active = true;

        // The function to test
        var label = nfRegistryService.getSortByLabel();

        //assertions
        expect(label).toBe('Oldest (update)');
    });

    it('should get the `Newest (update)` sort by label', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.dropletColumns[1].active = true;
        nfRegistryService.dropletColumns[1].sortOrder = 'ASC';

        // The function to test
        var label = nfRegistryService.getSortByLabel();

        //assertions
        expect(label).toBe('Newest (update)');
    });

    it('should generate the sort menu\'s `Name (a - z)` label', function () {
        // The function to test
        var label = nfRegistryService.generateSortMenuLabels({name: 'name', label: 'Name', sortable: true});

        //assertions
        expect(label).toBe('Name (a - z)');
    });

    it('should generate the sort menu\'s `Name (z - a)` label', function () {
        // The function to test
        var label = nfRegistryService.generateSortMenuLabels({
            name: 'name',
            label: 'Name',
            sortable: true,
            sortOrder: 'ASC'
        });

        //assertions
        expect(label).toBe('Name (z - a)');
    });

    it('should generate the sort menu\'s `Newest (update)` label', function () {
        // The function to test
        var label = nfRegistryService.generateSortMenuLabels({name: 'updated', label: 'Updated', sortable: true});

        //assertions
        expect(label).toBe('Newest (update)');
    });

    it('should generate the sort menu\'s `Oldest (update)` label', function () {
        // The function to test
        var label = nfRegistryService.generateSortMenuLabels({
            name: 'updated',
            label: 'Updated',
            sortable: true,
            sortOrder: 'ASC'
        });

        //assertions
        expect(label).toBe('Oldest (update)');
    });

    it('should sort `droplets` by `column`', function () {
        //Spy
        spyOn(nfRegistryService, 'filterDroplets').and.callFake(function () {
        });

        // object to be updated by the test
        var column = {name: 'name', label: 'Name', sortable: true};

        // The function to test
        nfRegistryService.sortDroplets(column);

        //assertions
        expect(column.active).toBe(true);
        var filterDropletsCall = nfRegistryService.filterDroplets.calls.first();
        expect(filterDropletsCall.args[0]).toBe('name');
        expect(filterDropletsCall.args[1]).toBe('ASC');
        expect(nfRegistryService.activeDropletColumn).toBe(column);
    });

    it('should sort `buckets` by `column`', function () {
        //Spy
        spyOn(nfRegistryService, 'filterBuckets').and.callFake(function () {
        });

        // object to be updated by the test
        var column = {name: 'name', label: 'Bucket Name', sortable: true};

        // The function to test
        nfRegistryService.sortBuckets(column);

        //assertions
        expect(column.active).toBe(true);
        var filterBucketsCall = nfRegistryService.filterBuckets.calls.first();
        expect(filterBucketsCall.args[0]).toBe('name');
        expect(filterBucketsCall.args[1]).toBe('ASC');
        expect(nfRegistryService.activeBucketsColumn).toBe(column);
    });

    it('should generate the auto complete options for the droplet filter.', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.filteredDroplets = [{
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

        // The function to test
        nfRegistryService.getAutoCompleteDroplets();

        //assertions
        expect(nfRegistryService.autoCompleteDroplets[0]).toBe(nfRegistryService.filteredDroplets[0].name);
    });

    it('should generate the auto complete options for the bucket filter.', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.filteredBuckets = [{
            'identifier': '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Bucket #1',
            'description': 'This is bucket #1'
        }];

        // The function to test
        nfRegistryService.getAutoCompleteBuckets();

        //assertions
        expect(nfRegistryService.autoCompleteBuckets[0]).toBe(nfRegistryService.filteredBuckets[0].name);
    });

    it('should check if all buckets are selected and return false.', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.filteredBuckets = [{
            'identifier': '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Bucket #1'
        }, {
            'identifier': '5c04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Bucket #2'
        }];

        // The function to test
        var allSelected = nfRegistryService.allFilteredBucketsSelected();

        //assertions
        expect(allSelected).toBe(false);
        expect(nfRegistryService.disableMultiBucketActions).toBe(true);
    });

    it('should check if all buckets are selected and return true.', function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.filteredBuckets = [{
            'identifier': '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Bucket #1',
            'checked': true
        }, {
            'identifier': '5c04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Bucket #2',
            'checked': true
        }];

        // The function to test
        var allSelected = nfRegistryService.allFilteredBucketsSelected();

        //assertions
        expect(allSelected).toBe(true);
        expect(nfRegistryService.disableMultiBucketActions).toBe(false);
    });

    it('should set the `allBucketsSelected` state to true.', function () {
        //Spy
        spyOn(nfRegistryService, 'allFilteredBucketsSelected').and.callFake(function () {
        }).and.returnValue(true);

        // The function to test
        nfRegistryService.determineAllBucketsSelectedState();

        //assertions
        expect(nfRegistryService.allBucketsSelected).toBe(true);
    });

    it('should set the `allBucketsSelected` state to false.', function () {
        //Spy
        spyOn(nfRegistryService, 'allFilteredBucketsSelected').and.callFake(function () {
        }).and.returnValue(false);

        // The function to test
        nfRegistryService.determineAllBucketsSelectedState();

        //assertions
        expect(nfRegistryService.allBucketsSelected).toBe(false);
    });

    it('should toggle all bucket `checked` properties to true.', function () {
        //Spy
        spyOn(nfRegistryService, 'selectAllBuckets').and.callFake(function () {
        });

        nfRegistryService.allBucketsSelected = true;

        // The function to test
        nfRegistryService.toggleBucketsSelectAll();

        //assertions
        expect(nfRegistryService.selectAllBuckets).toHaveBeenCalled();
    });

    it('should toggle all bucket `checked` properties to false.', function () {
        //Spy
        spyOn(nfRegistryService, 'deselectAllBuckets').and.callFake(function () {
        });

        nfRegistryService.allBucketsSelected = false;

        // The function to test
        nfRegistryService.toggleBucketsSelectAll();

        //assertions
        expect(nfRegistryService.deselectAllBuckets).toHaveBeenCalled();
    });

    it('should select all buckets.', function () {
        nfRegistryService.filteredBuckets = [{identifier: 1}];

        // The function to test
        nfRegistryService.selectAllBuckets();

        //assertions
        expect(nfRegistryService.filteredBuckets[0].checked).toBe(true);
        expect(nfRegistryService.disableMultiBucketActions).toBe(false);
    });

    it('should deselect all buckets.', function () {
        nfRegistryService.filteredBuckets = [{identifier: 1, checked: true}];

        // The function to test
        nfRegistryService.deselectAllBuckets();

        //assertions
        expect(nfRegistryService.filteredBuckets[0].checked).toBe(false);
        expect(nfRegistryService.disableMultiBucketActions).toBe(true);
    });

    it('should add a bucket search term.', function () {
        //Spy
        spyOn(nfRegistryService, 'filterBuckets').and.callFake(function () {
        });

        // The function to test
        nfRegistryService.bucketsSearchAdd('Bucket #1');

        //assertions
        expect(nfRegistryService.bucketsSearchTerms.length).toBe(1);
        expect(nfRegistryService.bucketsSearchTerms[0]).toBe('Bucket #1');
        expect(nfRegistryService.filterBuckets).toHaveBeenCalled();
    });

    it('should add a bucket search term.', function () {
        //Spy
        spyOn(nfRegistryService, 'filterBuckets').and.callFake(function () {
        });

        //set up the state
        nfRegistryService.bucketsSearchTerms = ['Bucket #1'];

        // The function to test
        nfRegistryService.bucketsSearchRemove('Bucket #1');

        //assertions
        expect(nfRegistryService.bucketsSearchTerms.length).toBe(0);
        expect(nfRegistryService.filterBuckets).toHaveBeenCalled();
    });
});

describe('NfRegistry Service w/ Angular testing utils', function () {
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

        fixture = ngCoreTesting.TestBed.createComponent(NfRegistryDropletGridListViewer);

        // test instance
        comp = fixture.componentInstance;

        // from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);
        nfRegistryApi = ngCoreTesting.TestBed.get(NfRegistryApi);

        fixture.detectChanges();

        // Spy
        spyOn(nfRegistryApi.http, 'get').and.callThrough();
    });

    it('should retrieve the snapshot metadata for the given droplet.', ngCoreTesting.fakeAsync(function () {
        //Spy
        spyOn(nfRegistryService.api, 'getDropletSnapshotMetadata').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of([{
            version: 999
        }]));

        // object to be updated by the test
        var droplet = {link: {href: 'test/id'}};

        // The function to test
        nfRegistryService.getDropletSnapshotMetadata(droplet);

        // wait for async getDropletSnapshotMetadata call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        //assertions
        expect(droplet.snapshotMetadata[0].version).toBe(999);
        expect(nfRegistryService.api.getDropletSnapshotMetadata).toHaveBeenCalled();
        expect(nfRegistryService.api.getDropletSnapshotMetadata.calls.count()).toBe(1);
        var getDropletSnapshotMetadataCall = nfRegistryService.api.getDropletSnapshotMetadata.calls.first()
        expect(getDropletSnapshotMetadataCall.args[0]).toBe('test/id');
        expect(getDropletSnapshotMetadataCall.args[1]).toBe(true);
    }));

    it('should execute the `delete` droplet action.', ngCoreTesting.fakeAsync(function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.droplets = [{identifier: '2e04b4fb-9513-47bb-aa74-1ae34616bfdc'}];

        //Spy
        spyOn(nfRegistryService.dialogService, 'openConfirm').and.returnValue({
            afterClosed: function () {
                return rxjs.Observable.of(true);
            }
        });
        spyOn(nfRegistryService.api, 'deleteDroplet').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({identifier: '2e04b4fb-9513-47bb-aa74-1ae34616bfdc'}));
        spyOn(nfRegistryService, 'filterDroplets').and.callFake(function () {
        });

        // The function to test
        nfRegistryService.executeDropletAction({name: 'delete'}, {
            identifier: '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
            type: 'testTYPE',
            link: {href: 'testhref'}
        });

        // wait for async nfRegistryService.api.deleteDroplet call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        //assertions
        expect(nfRegistryService.droplets.length).toBe(0);
        expect(nfRegistryService.filterDroplets).toHaveBeenCalled();
        var openConfirmCall = nfRegistryService.dialogService.openConfirm.calls.first()
        expect(openConfirmCall.args[0].title).toBe('Delete testtype');
        var deleteDropletCall = nfRegistryService.api.deleteDroplet.calls.first()
        expect(deleteDropletCall.args[0]).toBe('testhref');
    }));

    it('should filter droplets by name.', ngCoreTesting.fakeAsync(function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.dropletsSearchTerms = ['Flow #1'];
        nfRegistryService.droplets = [{
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
        }, {
            'identifier': '5d04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Flow #2',
            'description': 'This is flow #2',
            'bucketIdentifier': '3g7f9e54-dc09-4ceb-aa58-9fe581319cdc',
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

        //Spy
        spyOn(nfRegistryService, 'getAutoCompleteDroplets');

        // The function to test
        nfRegistryService.filterDroplets();

        // wait for async nfRegistryService.api.deleteDroplet call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        //assertions
        expect(nfRegistryService.filteredDroplets.length).toBe(1);
        expect(nfRegistryService.filteredDroplets[0].name).toBe('Flow #1');
        expect(nfRegistryService.getAutoCompleteDroplets).toHaveBeenCalled();
    }));

    it('should filter droplets by `type:flow` (demonstrate ability to do advanced searching of a droplet by a property `name:value` pair).', ngCoreTesting.fakeAsync(function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.dropletsSearchTerms = ['type:FLOW'];
        nfRegistryService.droplets = [{
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
        }, {
            'identifier': '5d04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Flow #2',
            'description': 'This is not a flow #2',
            'bucketIdentifier': '3g7f9e54-dc09-4ceb-aa58-9fe581319cdc',
            'createdTimestamp': 1505931890999,
            'modifiedTimestamp': 1505931890999,
            'type': 'something',
            'snapshotMetadata': null,
            'link': {
                'params': {
                    'rel': 'self'
                },
                'href': 'flows/5d04b4fb-9513-47bb-aa74-1ae34616bfdc'
            }
        }];

        //Spy
        spyOn(nfRegistryService, 'getAutoCompleteDroplets');

        // The function to test
        nfRegistryService.filterDroplets();

        // wait for async nfRegistryService.api.deleteDroplet call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        //assertions
        expect(nfRegistryService.filteredDroplets.length).toBe(1);
        expect(nfRegistryService.filteredDroplets[0].name).toBe('Flow #1');
        expect(nfRegistryService.getAutoCompleteDroplets).toHaveBeenCalled();
    }));

    it('should execute a `delete` action on a bucket.', ngCoreTesting.fakeAsync(function () {
        // from the root injector
        var dialogService = ngCoreTesting.TestBed.get(fdsDialogsModule.FdsDialogService);

        //Spy
        spyOn(nfRegistryService, 'filterBuckets').and.callFake(function () {
        });
        spyOn(dialogService, 'openConfirm').and.callFake(function () {
        }).and.returnValue({
            afterClosed: function () {
                return rxjs.Observable.of(true);
            }
        });
        spyOn(nfRegistryService.api, 'deleteBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({identifier: '2e04b4fb-9513-47bb-aa74-1ae34616bfdc'}));

        // object to be updated by the test
        var bucket = {identifier: '999'};

        // set up the bucket to be deleted
        nfRegistryService.buckets = [bucket, {identifier: 1}];

        // The function to test
        nfRegistryService.executeBucketAction({name: 'delete'}, bucket);

        // wait for async openConfirm call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        // wait for async deleteBucket call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        //assertions
        expect(dialogService.openConfirm).toHaveBeenCalled();
        expect(nfRegistryService.api.deleteBucket).toHaveBeenCalled();
        expect(nfRegistryService.filterBuckets).toHaveBeenCalled();
        expect(nfRegistryService.buckets.length).toBe(1);
        expect(nfRegistryService.buckets[0].identifier).toBe(1);
    }));

    it('should execute a `permissions` action on a bucket.', function () {
        // from the root injector
        var router = ngCoreTesting.TestBed.get(ngRouter.Router);

        //Spy
        spyOn(router, 'navigateByUrl').and.callFake(function () {
        });

        // object to be updated by the test
        var bucket = {identifier: '999'};

        // The function to test
        nfRegistryService.executeBucketAction({name: 'permissions', type: 'sidenav'}, bucket);

        //assertions
        var navigateByUrlCall = router.navigateByUrl.calls.first();
        expect(navigateByUrlCall.args[0]).toBe('/nifi-registry/administration/workflow(sidenav:bucket/permissions/999)');
    });

    it('should filter buckets by name.', ngCoreTesting.fakeAsync(function () {
        //Setup the nfRegistryService state for this test
        nfRegistryService.bucketsSearchTerms = ['Bucket #1'];
        nfRegistryService.buckets = [{
            'identifier': '2e04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Bucket #1',
            'description': 'This is bucket #1',
            'checked': true
        }, {
            'identifier': '5d04b4fb-9513-47bb-aa74-1ae34616bfdc',
            'name': 'Bucket #2',
            'description': 'This is bucket #2',
            'checked': true
        }];

        //Spy
        spyOn(nfRegistryService, 'getAutoCompleteBuckets');

        //assertion
        expect(nfRegistryService.disableMultiBucketActions).toBe(true);

        // The function to test
        nfRegistryService.filterBuckets();

        // wait for async nfRegistryService.api.deleteDroplet call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        //assertions
        expect(nfRegistryService.filteredBuckets.length).toBe(1);
        expect(nfRegistryService.filteredBuckets[0].name).toBe('Bucket #1');
        expect(nfRegistryService.getAutoCompleteBuckets).toHaveBeenCalled();
        expect(nfRegistryService.disableMultiBucketActions).toBe(false);
    }));

    it('should delete all selected buckets.', ngCoreTesting.fakeAsync(function () {
        // from the root injector
        var dialogService = ngCoreTesting.TestBed.get(fdsDialogsModule.FdsDialogService);

        //Spy
        spyOn(nfRegistryService, 'filterBuckets').and.callFake(function () {
        });
        spyOn(nfRegistryService, 'determineAllBucketsSelectedState').and.callFake(function () {
        });
        spyOn(dialogService, 'openConfirm').and.callFake(function () {
        }).and.returnValue({
            afterClosed: function () {
                return rxjs.Observable.of(true);
            }
        });
        spyOn(nfRegistryService.api, 'deleteBucket').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({identifier: 999}));

        // object to be updated by the test
        var bucket = {identifier: 999, checked: true};

        // set up the bucket to be deleted
        nfRegistryService.buckets = nfRegistryService.filteredBuckets = [bucket, {identifier: 1}];

        // The function to test
        nfRegistryService.deleteSelectedBuckets();

        // wait for async openConfirm call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        // wait for async deleteBucket call
        ngCoreTesting.tick();

        //inform angular to detect changes
        fixture.detectChanges();

        //assertions
        expect(dialogService.openConfirm).toHaveBeenCalled();
        expect(nfRegistryService.api.deleteBucket).toHaveBeenCalled();
        expect(nfRegistryService.api.deleteBucket.calls.count()).toBe(1);
        expect(nfRegistryService.filterBuckets).toHaveBeenCalled();
        expect(nfRegistryService.filterBuckets.calls.count()).toBe(1);
        expect(nfRegistryService.disableMultiBucketActions).toBe(true);
        expect(nfRegistryService.determineAllBucketsSelectedState).toHaveBeenCalled();
        expect(nfRegistryService.buckets.length).toBe(1);
        expect(nfRegistryService.buckets[0].identifier).toBe(1);
    }));
});