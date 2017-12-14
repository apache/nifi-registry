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

/**
 * NfRegistryUsersAdministrationAuthGuard constructor.
 *
 * @param nfRegistryService                 The nfRegistryService module.
 * @constructor
 */
function NfRegistryUsersAdministrationAuthGuard(nfRegistryService) {
    this.nfRegistryService = nfRegistryService;
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
        this.nfRegistryService.api.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryService.api.loadCurrentUser().subscribe(function (currentUser) {
                self.nfRegistryService.currentUser = currentUser;
                // if the user is logged, we want to determine if they were logged in using a certificate
                if (currentUser.anonymous === false) {
                    // render the logout button if there is a token locally
                    if (self.nfRegistryService.nfStorage.getItem('jwt') !== null) {
                        self.nfRegistryService.currentUser.canLogout = true;
                    }

                    // redirect to explorer perspective if not admin
                    if (!currentUser.resourcePermissions.anyTopLevelResource.canRead) {
                        self.nfRegistryService.router.navigateByUrl('/nifi-registry/explorer');
                    } else {
                        self.nfRegistryService.router.navigateByUrl(url);
                    }
                } else {
                    // navigate to the login page
                    self.nfRegistryService.router.navigateByUrl('/nifi-registry/login');
                }
            });
        });

        return false;
    }
};

NfRegistryUsersAdministrationAuthGuard.parameters = [
    NfRegistryService
];

/**
 * NfRegistryWorkflowsAdministrationAuthGuard constructor.
 *
 * @param nfRegistryService                 The nfRegistryService module.
 * @constructor
 */
function NfRegistryWorkflowsAdministrationAuthGuard(nfRegistryService) {
    this.nfRegistryService = nfRegistryService;
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
        this.nfRegistryService.api.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryService.api.loadCurrentUser().subscribe(function (currentUser) {
                self.nfRegistryService.currentUser = currentUser;
                // if the user is logged, we want to determine if they were logged in using a certificate
                if (currentUser.anonymous === false) {
                    // render the logout button if there is a token locally
                    if (self.nfRegistryService.nfStorage.getItem('jwt') !== null) {
                        self.nfRegistryService.currentUser.canLogout = true;
                    }

                    // redirect to explorer perspective if not admin
                    if (!currentUser.resourcePermissions.anyTopLevelResource.canRead) {
                        self.nfRegistryService.router.navigateByUrl('/nifi-registry/explorer');
                    } else {
                        if (currentUser.resourcePermissions.buckets) {
                            self.nfRegistryService.router.navigateByUrl(url);
                        } else {
                            self.nfRegistryService.router.navigateByUrl('/nifi-registry/administration/users');
                        }
                    }
                } else {
                    // Navigate to the login page
                    self.nfRegistryService.router.navigateByUrl(url);
                }
            });
        });

        return false;
    }
};

NfRegistryWorkflowsAdministrationAuthGuard.parameters = [
    NfRegistryService
];

/**
 * NfRegistryLoginAuthGuard constructor.
 *
 * @param nfRegistryService                 The nfRegistryService module.
 * @constructor
 */
function NfRegistryLoginAuthGuard(nfRegistryService) {
    this.nfRegistryService = nfRegistryService;
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
        this.nfRegistryService.api.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryService.api.loadCurrentUser().subscribe(function (currentUser) {
                self.nfRegistryService.currentUser = currentUser;
                // if the user is logged, we want to determine if they were logged in using a certificate
                if (currentUser.anonymous === false) {
                    // render the logout button if there is a token locally
                    if (self.nfRegistryService.nfStorage.getItem('jwt') !== null) {
                        self.nfRegistryService.currentUser.canLogout = true;
                    }
                    self.nfRegistryService.router.navigateByUrl(self.nfRegistryService.redirectUrl);
                } else {
                    self.nfRegistryService.router.navigateByUrl('/nifi-registry/login');
                }
            });
        });

        return false;
    }
};

NfRegistryLoginAuthGuard.parameters = [
    NfRegistryService
];

/**
 * NfRegistryResourcesAuthGuard constructor.
 *
 * @param nfRegistryService                 The nfRegistryService module.
 * @constructor
 */
function NfRegistryResourcesAuthGuard(nfRegistryService) {
    this.nfRegistryService = nfRegistryService;
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
        if (this.nfRegistryService.currentUser.resourcePermissions.buckets.canRead) { return true; }

        // Store the attempted URL for redirecting
        this.nfRegistryService.redirectUrl = url;

        // attempt kerberos authentication
        this.nfRegistryService.api.ticketExchange().subscribe(function (jwt) {
            self.nfRegistryService.api.loadCurrentUser().subscribe(function (currentUser) {
                self.nfRegistryService.currentUser = currentUser;
                // if the user is logged, we want to determine if they were logged in using a certificate
                if (currentUser.anonymous === false) {
                    // render the logout button if there is a token locally
                    if (self.nfRegistryService.nfStorage.getItem('jwt') !== null) {
                        self.nfRegistryService.currentUser.canLogout = true;
                    }
                }
                self.nfRegistryService.router.navigateByUrl(url);
            });
        });

        return false;
    }
};

NfRegistryResourcesAuthGuard.parameters = [
    NfRegistryService
];

module.exports = {
    NfRegistryUsersAdministrationAuthGuard: NfRegistryUsersAdministrationAuthGuard,
    NfRegistryWorkflowsAdministrationAuthGuard: NfRegistryWorkflowsAdministrationAuthGuard,
    NfRegistryLoginAuthGuard: NfRegistryLoginAuthGuard,
    NfRegistryResourcesAuthGuard: NfRegistryResourcesAuthGuard
};
