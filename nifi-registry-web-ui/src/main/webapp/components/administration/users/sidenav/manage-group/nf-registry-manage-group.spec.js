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
var NfRegistryCreateNewGroup = require('nifi-registry/components/administration/users/dialogs/create-new-group/nf-registry-create-new-group.js');
var NfRegistryEditBucketPolicy = require('nifi-registry/components/administration/workflow/dialogs/edit-bucket-policy/nf-registry-edit-bucket-policy.js');
var NfRegistryAddPolicyToBucket = require('nifi-registry/components/administration/workflow/dialogs/add-policy-to-bucket/nf-registry-add-policy-to-bucket.js');
var NfRegistryAddUserToGroups = require('nifi-registry/components/administration/users/dialogs/add-user-to-groups/nf-registry-add-user-to-groups.js');
var NfRegistryAddUsersToGroup = require('nifi-registry/components/administration/users/dialogs/add-users-to-group/nf-registry-add-users-to-group.js');
var NfRegistryManageUser = require('nifi-registry/components/administration/users/sidenav/manage-user/nf-registry-manage-user.js');
var NfRegistryManageGroup = require('nifi-registry/components/administration/users/sidenav/manage-group/nf-registry-manage-group.js');
var NfRegistryManageBucket = require('nifi-registry/components/administration/workflow/sidenav/manage-bucket/nf-registry-manage-bucket.js');
var NfRegistryWorkflowAdministration = require('nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js');
var NfRegistryCreateBucket = require('nifi-registry/components/administration/workflow/dialogs/create-bucket/nf-registry-create-bucket.js');
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js');
var fdsCore = require('@fluid-design-system/core');
var ngMoment = require('angular2-moment');
var rxjs = require('rxjs/Rx');
var ngCommonHttp = require('@angular/common/http');
var NfRegistryTokenInterceptor = require('nifi-registry/services/nf-registry.token.interceptor.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');
var NfLoginComponent = require('nifi-registry/components/login/nf-registry-login.js');
var NfUserLoginComponent = require('nifi-registry/components/login/dialogs/nf-registry-user-login.js');

describe('NfRegistryManageGroup Component', function () {
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
                NfRegistryWorkflowAdministration,
                NfRegistryAddUser,
                NfRegistryCreateBucket,
                NfRegistryCreateNewGroup,
                NfRegistryAddUserToGroups,
                NfRegistryAddUsersToGroup,
                NfRegistryAddPolicyToBucket,
                NfRegistryEditBucketPolicy,
                NfRegistryGridListViewer,
                NfRegistryBucketGridListViewer,
                NfRegistryDropletGridListViewer,
                NfPageNotFoundComponent,
                NfLoginComponent,
                NfUserLoginComponent
            ],
            entryComponents: [
                NfRegistryAddUser,
                NfRegistryCreateBucket,
                NfRegistryCreateNewGroup,
                NfRegistryAddUserToGroups,
                NfRegistryAddUsersToGroup,
                NfRegistryAddPolicyToBucket,
                NfRegistryEditBucketPolicy,
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
                },
                {
                    provide: ngRouter.ActivatedRoute,
                    useValue: {
                        params: rxjs.Observable.of({groupId: '123'})
                    }
                }
            ]
        });
        fixture = ngCoreTesting.TestBed.createComponent(NfRegistryManageGroup);

        // test instance
        comp = fixture.componentInstance;

        // from the root injector
        nfRegistryService = ngCoreTesting.TestBed.get(NfRegistryService);
        nfRegistryApi = ngCoreTesting.TestBed.get(NfRegistryApi);

        // because the NfRegistryManageGroup component is a nested route component we need to set up the nfRegistryService service manually
        nfRegistryService.sidenav = {
            open: function () {
            },
            close: function () {
            }
        };
        nfRegistryService.group = {
            identifier: 999,
            identity: 'Group #1',
            users: [{
                identifier: '123',
                identity: 'Group #1'
            }],
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            }
        };
        nfRegistryService.groups = [nfRegistryService.group];

        //Spy
        spyOn(nfRegistryApi, 'ticketExchange').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryApi, 'loadCurrentUser').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({}));
    });

    it('should have a defined component', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));
        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        expect(comp).toBeDefined();
        expect(nfRegistryService.group.identifier).toEqual('123');

        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first()
        expect(getUserGroupCall.args[0]).toBe('123');
    }));

    it('should FAIL to get user by id and redirect to admin users perspective', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404
        }));
        spyOn(comp.router, 'navigateByUrl').and.callFake(function () {
        });
        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var routerCall = comp.router.navigateByUrl.calls.first();
        expect(routerCall.args[0]).toBe('/nifi-registry/administration/users');
        expect(comp.router.navigateByUrl.calls.count()).toBe(1);
    }));

    it('should FAIL to get user by id and redirect to workflow perspective', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 409
        }));
        spyOn(comp.router, 'navigateByUrl').and.callFake(function () {
        });
        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var routerCall = comp.router.navigateByUrl.calls.first();
        expect(routerCall.args[0]).toBe('/nifi-registry/administration/workflow');
        expect(comp.router.navigateByUrl.calls.count()).toBe(1);
    }));

    it('should redirect to users perspective', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));
        spyOn(comp.router, 'navigateByUrl').and.callFake(function () {
        });
        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        // the function to test
        comp.closeSideNav();

        //assertions
        var routerCall = comp.router.navigateByUrl.calls.first();
        expect(routerCall.args[0]).toBe('/nifi-registry/administration/users');
        expect(comp.router.navigateByUrl.calls.count()).toBe(1);
    }));

    it('should toggle to create the manage bucket privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404,
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'postPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageBucketsPrivileges({
            checked: true
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.postPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to update the manage bucket privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageBucketsPrivileges({
            checked: true
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to remove the manage bucket privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 400,
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageBucketsPrivileges({
            checked: false
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to create the manage proxy privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404,
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'postPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageProxyPrivileges({
            checked: true
        }, 'write');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.postPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to update the manage proxy privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageProxyPrivileges({
            checked: true
        }, 'write');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to remove the manage proxy privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 400,
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageProxyPrivileges({
            checked: false
        }, 'write');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to create the manage policies privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404,
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'postPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManagePoliciesPrivileges({
            checked: true
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.postPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to update the manage policies privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManagePoliciesPrivileges({
            checked: true
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to remove the manage policies privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 400,
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManagePoliciesPrivileges({
            checked: false
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to create the manage tenants privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 404,
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'postPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageTenantsPrivileges({
            checked: true
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.postPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to update the manage tenants privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: []
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageTenantsPrivileges({
            checked: true
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should toggle to remove the manage tenants privileges for the current group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(nfRegistryApi, 'getPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            status: 400,
            userGroups: [
                {
                    identifier: '123',
                    identity: 'Group #1',
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                }
            ]
        }));
        spyOn(nfRegistryApi, 'putPolicyActionResource').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({}));
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.toggleGroupManageTenantsPrivileges({
            checked: false
        }, 'read');

        //assertions
        expect(nfRegistryApi.getPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.putPolicyActionResource.calls.count()).toBe(1);
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
    }));

    it('should open a modal dialog UX enabling the addition of the current user to a group(s)', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(comp, 'filterUsers').and.callFake(function () {
        });
        spyOn(comp.dialog, 'open').and.callFake(function () {
            return {
                afterClosed: function () {
                    return rxjs.Observable.of({});
                }
            }
        });
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));
        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.addUsersToGroup();

        //assertions
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
        expect(comp.filterUsers).toHaveBeenCalled();
    }));

    it('should remove user from group', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(comp, 'filterUsers').and.callFake(function () {
        });
        spyOn(comp.snackBarService, 'openCoaster');
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));
        spyOn(nfRegistryApi, 'updateUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({}));
        spyOn(comp.router, 'navigateByUrl').and.callFake(function () {
        });
        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        var user = {
            identifier: '123'
        };

        // the function to test
        comp.removeUserFromGroup(user);

        //assertions
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(2);
        expect(nfRegistryApi.updateUserGroup.calls.count()).toBe(1);
        expect(comp.snackBarService.openCoaster.calls.count()).toBe(1);
        expect(comp.filterUsers).toHaveBeenCalled();
    }));

    it('should update group name', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(comp.dialogService, 'openConfirm').and.callFake(function () {
            return {
                afterClosed: function () {
                    return rxjs.Observable.of(true);
                }
            }
        });
        spyOn(comp.snackBarService, 'openCoaster');
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));
        spyOn(nfRegistryApi, 'updateUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'test',
            status: 200
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.updateGroupName('test');

        //assertions
        expect(comp.snackBarService.openCoaster.calls.count()).toBe(1);
        expect(comp.nfRegistryService.group.identity).toBe('test');
    }));

    it('should fail to update group name (409)', ngCoreTesting.fakeAsync(function () {
        // Spy
        spyOn(comp.dialogService, 'openConfirm').and.callFake(function () {
            return {
                afterClosed: function () {
                    return rxjs.Observable.of(true);
                }
            }
        });
        spyOn(comp.snackBarService, 'openCoaster');
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));
        spyOn(nfRegistryApi, 'updateUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'test',
            status: 409
        }));

        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();

        //assertions
        var getUserGroupCall = nfRegistryApi.getUserGroup.calls.first();
        expect(getUserGroupCall.args[0]).toBe('123');
        expect(nfRegistryApi.getUserGroup.calls.count()).toBe(1);

        // the function to test
        comp.updateGroupName('test');

        //assertions
        expect(comp.dialogService.openConfirm.calls.count()).toBe(1);
        expect(comp.nfRegistryService.group.identity).toBe('Group #1');
    }));

    it('should destroy the component', ngCoreTesting.fakeAsync(function () {
        spyOn(nfRegistryService.sidenav, 'close');
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({
            identifier: '123',
            identity: 'Group #1',
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            },
            users: [{
                identifier: '123',
                identity: 'User #1',
                resourcePermissions: {
                    anyTopLevelResource: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    buckets: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    tenants: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    policies: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    },
                    proxy: {
                        canRead: false,
                        canWrite: false,
                        canDelete: false
                    }
                }
            }]
        }));
        // 1st change detection triggers ngOnInit
        fixture.detectChanges();
        // wait for async calls
        ngCoreTesting.tick();
        // 2nd change detection completes after the getUserGroup calls
        fixture.detectChanges();
        spyOn(comp.$subscription, 'unsubscribe');

        // The function to test
        comp.ngOnDestroy();

        //assertions
        expect(nfRegistryService.sidenav.close).toHaveBeenCalled();
        expect(nfRegistryService.group.identity).toBe('Group #1');
        expect(comp.$subscription.unsubscribe).toHaveBeenCalled();
    }));
});
