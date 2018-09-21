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

var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');
var ngRouter = require('@angular/router');
var fdsDialogsModule = require('@flow-design-system/dialogs');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');

/**
 * NfRegistryUsersAdministrationAuthGuard constructor.
 *
 * @param nfRegistryService         The nfRegistryService module.
 * @param nfRegistryApi             The nfRegistryApi module.
 * @param nfStorage                 The NfStorage module.
 * @param router                    The angular router module.
 * @param fdsDialogService          The FDS dialog service.
 * @constructor
 */
function NfRegistryUsersAdministrationAuthGuard(nfRegistryService, nfRegistryApi, nfStorage, router, fdsDialogService) {
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.nfStorage = nfStorage;
    this.router = router;
    this.dialogService = fdsDialogService;
};

NfRegistryUsersAdministrationAuthGuard.prototype = {
    constructor: NfRegistryUsersAdministrationAuthGuard,

    /**
     * Can activate guard.
     * @returns {*}
     */
    canActivate: function (route, state) {
        var url = state.url;

        return this.checkLogin(url);
    },

    checkLogin: function (url) {
        var self = this;
        if (this.nfRegistryService.currentUser.resourcePermissions.tenants.canRead) { return true; }

        // Store the attempted URL for redirecting
        this.nfRegistryService.redirectUrl = url;

        // attempt kerberos authentication
        this.nfRegistryApi.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryApi.loadCurrentUser().subscribe(function (currentUser) {
                // there is no anonymous access and we don't know this user - open the login page which handles login/registration/etc
                if (currentUser.error) {
                    if (currentUser.error.status === 401) {
                        self.nfStorage.removeItem('jwt');
                        self.router.navigateByUrl('/nifi-registry/login');
                    }
                } else {
                    self.nfRegistryService.currentUser = currentUser;
                    if (currentUser.anonymous === false) {
                        // render the logout button if there is a token locally
                        if (self.nfStorage.getItem('jwt') !== null) {
                            self.nfRegistryService.currentUser.canLogout = true;
                        }

                        // redirect to explorer perspective if not admin
                        if (!currentUser.resourcePermissions.anyTopLevelResource.canRead) {
                            self.dialogService.openConfirm({
                                title: 'Access denied',
                                message: 'Please contact your system administrator.',
                                acceptButton: 'Ok',
                                acceptButtonColor: 'fds-warn'
                            });
                            self.router.navigateByUrl('/nifi-registry/explorer');
                        } else {
                            if (currentUser.resourcePermissions.tenants.canRead) {
                                self.router.navigateByUrl(url);
                            } else {
                                self.dialogService.openConfirm({
                                    title: 'Access denied',
                                    message: 'Please contact your system administrator.',
                                    acceptButton: 'Ok',
                                    acceptButtonColor: 'fds-warn'
                                });
                                self.router.navigateByUrl('/nifi-registry/explorer');
                            }
                        }
                    } else {
                        // registry security not configured, redirect to workflow perspective
                        self.dialogService.openConfirm({
                            title: 'Not Applicable',
                            message: 'User administration is not configured for this registry.',
                            acceptButton: 'Ok',
                            acceptButtonColor: 'fds-warn'
                        });
                        self.router.navigateByUrl('/nifi-registry/administration/workflow');
                    }
                }
            });
        });

        return false;
    }
};

NfRegistryUsersAdministrationAuthGuard.parameters = [
    NfRegistryService,
    NfRegistryApi,
    NfStorage,
    ngRouter.Router,
    fdsDialogsModule.FdsDialogService
];

/**
 * NfRegistryWorkflowsAdministrationAuthGuard constructor.
 *
 * @param nfRegistryService         The nfRegistryService module.
 * @param nfRegistryApi             The nfRegistryApi module.
 * @param nfStorage                 The NfStorage module.
 * @param router                    The angular router module.
 * @param fdsDialogService          The FDS dialog service.
 * @constructor
 */
function NfRegistryWorkflowsAdministrationAuthGuard(nfRegistryService, nfRegistryApi, nfStorage, router, fdsDialogService) {
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.nfStorage = nfStorage;
    this.router = router;
    this.dialogService = fdsDialogService;
};

NfRegistryWorkflowsAdministrationAuthGuard.prototype = {
    constructor: NfRegistryWorkflowsAdministrationAuthGuard,

    /**
     * Can activate guard.
     * @returns {*}
     */
    canActivate: function (route, state) {
        var url = state.url;

        return this.checkLogin(url);
    },

    checkLogin: function (url) {
        var self = this;
        if (this.nfRegistryService.currentUser.resourcePermissions.buckets.canRead || this.nfRegistryService.currentUser.anonymous) { return true; }

        // Store the attempted URL for redirecting
        this.nfRegistryService.redirectUrl = url;

        // attempt kerberos authentication
        this.nfRegistryApi.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryApi.loadCurrentUser().subscribe(function (currentUser) {
                // there is no anonymous access and we don't know this user - open the login page which handles login/registration/etc
                if (currentUser.error) {
                    if (currentUser.error.status === 401) {
                        self.nfStorage.removeItem('jwt');
                        self.router.navigateByUrl('/nifi-registry/login');
                    }
                } else {
                    self.nfRegistryService.currentUser = currentUser;
                    if (currentUser.anonymous === false) {
                        // render the logout button if there is a token locally
                        if (self.nfStorage.getItem('jwt') !== null) {
                            self.nfRegistryService.currentUser.canLogout = true;
                        }

                        // redirect to explorer perspective if not admin
                        if (!currentUser.resourcePermissions.anyTopLevelResource.canRead) {
                            self.dialogService.openConfirm({
                                title: 'Access denied',
                                message: 'Please contact your system administrator.',
                                acceptButton: 'Ok',
                                acceptButtonColor: 'fds-warn'
                            });
                            self.router.navigateByUrl('/nifi-registry/explorer');
                        } else {
                            if (currentUser.resourcePermissions.buckets.canRead) {
                                self.router.navigateByUrl(url);
                            } else {
                                self.dialogService.openConfirm({
                                    title: 'Access denied',
                                    message: 'Please contact your system administrator.',
                                    acceptButton: 'Ok',
                                    acceptButtonColor: 'fds-warn'
                                });
                                self.router.navigateByUrl('/nifi-registry/administration/users');
                            }
                        }
                    } else {
                        // registry security not configured, allow access to workflow perspective
                        self.router.navigateByUrl(url);
                    }
                }
            });
        });

        return false;
    }
};

NfRegistryWorkflowsAdministrationAuthGuard.parameters = [
    NfRegistryService,
    NfRegistryApi,
    NfStorage,
    ngRouter.Router,
    fdsDialogsModule.FdsDialogService
];

/**
 * NfRegistryLoginAuthGuard constructor.
 *
 * @param nfRegistryService         The nfRegistryService module.
 * @param nfRegistryApi             The nfRegistryApi module.
 * @param nfStorage                 The NfStorage module.
 * @param router                    The angular router module.
 * @constructor
 */
function NfRegistryLoginAuthGuard(nfRegistryService, nfRegistryApi, nfStorage, router) {
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.nfStorage = nfStorage;
    this.router = router;
};

NfRegistryLoginAuthGuard.prototype = {
    constructor: NfRegistryLoginAuthGuard,

    /**
     * Can activate guard.
     * @returns {*}
     */
    canActivate: function (route, state) {
        var url = state.url;

        return this.checkLogin(url);
    },

    checkLogin: function (url) {
        var self = this;
        if (this.nfRegistryService.currentUser.anonymous) { return true; }
        // attempt kerberos authentication
        this.nfRegistryApi.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryApi.loadCurrentUser().subscribe(function (currentUser) {
                self.nfRegistryService.currentUser = currentUser;
                if (currentUser.anonymous === false) {
                    // render the logout button if there is a token locally
                    if (self.nfStorage.getItem('jwt') !== null) {
                        self.nfRegistryService.currentUser.canLogout = true;
                    }
                    self.nfRegistryService.currentUser.canActivateResourcesAuthGuard = true;
                    self.router.navigateByUrl(self.nfRegistryService.redirectUrl);
                } else {
                    if(self.nfRegistryService.currentUser.anonymous){
                        self.router.navigateByUrl('/nifi-registry');
                    } else {
                        self.nfRegistryService.currentUser.anonymous = true;
                        self.router.navigateByUrl(url);
                    }
                }
            });
        });

        return false;
    }
};

NfRegistryLoginAuthGuard.parameters = [
    NfRegistryService,
    NfRegistryApi,
    NfStorage,
    ngRouter.Router
];

/**
 * NfRegistryResourcesAuthGuard constructor.
 *
 * @param nfRegistryService         The nfRegistryService module.
 * @param nfRegistryApi             The nfRegistryApi module.
 * @param nfStorage                 The NfStorage module.
 * @param router                    The angular router module.
 * @constructor
 */
function NfRegistryResourcesAuthGuard(nfRegistryService, nfRegistryApi, nfStorage, router) {
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.nfStorage = nfStorage;
    this.router = router;
};

NfRegistryResourcesAuthGuard.prototype = {
    constructor: NfRegistryResourcesAuthGuard,

    /**
     * Can activate guard.
     * @returns {*}
     */
    canActivate: function (route, state) {
        var url = state.url;

        return this.checkLogin(url);
    },

    checkLogin: function (url) {
        var self = this;
        if (this.nfRegistryService.currentUser.canActivateResourcesAuthGuard === true) { return true; }

        // Store the attempted URL for redirecting
        this.nfRegistryService.redirectUrl = url;

        // attempt kerberos authentication
        this.nfRegistryApi.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryApi.loadCurrentUser().subscribe(function (currentUser) {
                // there is no anonymous access and we don't know this user - open the login page which handles login/registration/etc
                if (currentUser.error) {
                    if (currentUser.error.status === 401) {
                        self.nfStorage.removeItem('jwt');
                        self.router.navigateByUrl('/nifi-registry/login');
                    }
                } else {
                    self.nfRegistryService.currentUser = currentUser;
                    if (!currentUser || currentUser.anonymous === false) {
                        if(self.nfStorage.hasItem('jwt')){
                            self.nfRegistryService.currentUser.canLogout = true;
                            self.nfRegistryService.currentUser.canActivateResourcesAuthGuard = true;
                            self.router.navigateByUrl(url);
                        } else {
                            self.router.navigateByUrl('/nifi-registry/login');
                        }
                    } else if (currentUser.anonymous === true) {
                        // render the logout button if there is a token locally
                        if (self.nfStorage.getItem('jwt') !== null) {
                            self.nfRegistryService.currentUser.canLogout = true;
                        }
                        self.nfRegistryService.currentUser.canActivateResourcesAuthGuard = true;
                        self.router.navigateByUrl(url);
                    }
                }
            });
        });

        return false;
    }
};

NfRegistryResourcesAuthGuard.parameters = [
    NfRegistryService,
    NfRegistryApi,
    NfStorage,
    ngRouter.Router
];

module.exports = {
    NfRegistryUsersAdministrationAuthGuard: NfRegistryUsersAdministrationAuthGuard,
    NfRegistryWorkflowsAdministrationAuthGuard: NfRegistryWorkflowsAdministrationAuthGuard,
    NfRegistryLoginAuthGuard: NfRegistryLoginAuthGuard,
    NfRegistryResourcesAuthGuard: NfRegistryResourcesAuthGuard
};
