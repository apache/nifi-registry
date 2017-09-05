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
var rxjs = require('rxjs/Rx');

describe('NfRegistryAdministration Component', function () {
    var comp;
    var fixture;
    var de;
    var el;
    var nfRegistryService;
    var originalTimeout;

    function ActivatedRouteStub() {
        this._testParamMap = ngRouter.ParamMap;
        this.subject = new rxjs.BehaviorSubject(ngRouter.convertToParamMap(this.testParamMap));
        this.paramMap = this.subject.asObservable();

        this.params = {
            switchMap: function () {
                return Observable.of({
                    id: '1234',
                    name: "Test Registry",
                    certifications: [],
                    users: [],
                    buckets: []
                });
            }
        };
    };

    ActivatedRouteStub.prototype = {
        constructor: ActivatedRouteStub,
        navigateByUrl: function (url) {
            return url;
        }
    };

    Object.defineProperty(ActivatedRouteStub.prototype, "testParamMap", {
        get: function () {
            return this._testParamMap;
        },
        set: function (params) {
            this._testParamMap = ngRouter.convertToParamMap(params);
            this.subject.next(this._testParamMap);
        }
    });

    beforeEach(function () {
        originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
        jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
    });

    afterEach(function () {
        jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
    });

    beforeEach(ngCoreTesting.async(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                fdsCore,
                NfRegistryRoutes
            ],
            declarations: [FdsDemo, NfRegistry, NfRegistryExplorer, NfRegistryExplorerGridListViewer, NfRegistryAdministration, NfRegistryGeneralAdministration, NfRegistryUsersAdministration, NfRegistryUserDetails, NfRegistryUserPermissions, NfRegistryBucketDetails, NfRegistryBucketPermissions, NfRegistryAddUser, NfRegistryWorkflowAdministration, NfRegistryGridListViewer, NfRegistryBucketGridListViewer, NfRegistryDropletGridListViewer, NfPageNotFoundComponent],
            providers: [NfRegistryService, {
                provide: ngCommon.APP_BASE_HREF,
                useValue: '/'
            }, {provide: ngRouter.ActivatedRoute, useClass: ActivatedRouteStub}]
        });
    }));

    beforeEach(function () {
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistryAdministration);

        // NfRegistryAdministration test instance
        comp = fixture.componentInstance;

        // NfRegistryService from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);
        // spyOn(nfRegistryService, 'getRegistries').and.returnValue(Promise.resolve([{
        //     id: '1234',
        //     name: "Test Registry",
        //     certifications: [],
        //     users: [],
        //     buckets: []
        // }]));

        de = fixture.debugElement.query(ngPlatformBrowser.By.css('#nifi-registry-administration-perspective'));
        el = de.nativeElement;
    });

    it('should have a defined component', function () {
        fixture.detectChanges();
        expect(comp).toBeDefined();
        expect(de).toBeDefined();
    });

    it('should call Router.navigateByUrl("nifi-registry/administration/:registryId") with the ID of the registry', ngCoreTesting.inject([ngRouter.ActivatedRoute], function (router) {
        fixture.detectChanges();
        var spy = spyOn(router, 'navigateByUrl');
        comp.navigateToAdministration('23f6cc59-0156-1000-06b4-2b0810089090');
        var url = spy.calls.first().args[0];
        expect(url).toBe('nifi-registry/administration/23f6cc59-0156-1000-06b4-2b0810089090');
    }));

    xit('should call `NfRegistryService.getRegistry` when the route ID changes', ngCoreTesting.inject([ngRouter.ActivatedRoute], function (activeRoute) {
        spyOn(nfRegistryService, 'getRegistry');
        activeRoute.testParamMap = {registryId: 1234};
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistryAdministration);
        comp = fixture.componentInstance; // NfRegistryAdministration test instance
        fixture.detectChanges();
        expect(nfRegistryService.getRegistry).toHaveBeenCalledWith(1234);
    }));
});