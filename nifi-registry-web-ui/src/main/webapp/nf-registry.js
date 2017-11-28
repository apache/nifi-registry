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

var ngCore = require('@angular/core');
var ngCommonHttp = require('@angular/common/http');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var ngRouter = require('@angular/router');
var MILLIS_PER_SECOND = 1000;

/**
 * NfRegistry constructor.
 *
 * @param http                  The angular http module.
 * @param nfStorage             A wrapper for the browser's local storage.
 * @param nfRegistryService     The registry service.
 * @param nfRegistryApi     The api service.
 * @param changeDetectorRef     The change detector ref.
 * @param router                The angular router module.
 * @constructor
 */
function NfRegistry(http, nfStorage, nfRegistryService, nfRegistryApi, changeDetectorRef, router) {
    this.http = http;
    this.nfStorage = nfStorage;
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.cd = changeDetectorRef;
    this.router = router;
};

NfRegistry.prototype = {
    constructor: NfRegistry,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.sidenav = this.sidenav; //ngCore.ViewChild
        //
        // // attempt kerberos authentication
        // return this.nfRegistryApi.ticketExchange().subscribe(function (jwt) {
        //     // get the payload and store the token with the appropriate expiration
        //     var token = self.nfStorage.getJwtPayload(jwt);
        //     if(token) {
        //         var expiration = parseInt(token['exp'], 10) * MILLIS_PER_SECOND;
        //         self.nfStorage.setItem('jwt', jwt, expiration);
        //     }
        //     self.nfRegistryApi.loadCurrentUser().subscribe(function (currentUser) {
        //         // if the user is logged, we want to determine if they were logged in using a certificate
        //         if (currentUser.status !== "UNKNOWN") {
        //             // render the users name
        //             self.nfRegistryService.currentUser = currentUser;
        //
        //             // render the logout button if there is a token locally
        //             if (self.nfStorage.getItem('jwt') !== null) {
        //                 self.nfRegistryService.currentUser.canLogout = true;
        //             }
        //         } else {
        //             // set the anonymous user label
        //             self.nfRegistryService.currentUser.identity = 'Anonymous';
        //         }
        //     });
        // });
    },

    /**
     * since the child views are updating the nfRegistryService values that are used to display
     * the breadcrumbs in this component's view we need to manually detect changes at the correct
     * point in the lifecycle.
     */
    ngAfterViewChecked: function () {
        this.cd.detectChanges();
    },

    /**
     * Invalidate old tokens and route to login page
     */
    logout: function() {
        this.nfRegistryService.currentUser = {};
        this.nfStorage.removeItem('jwt');
        this.router.navigateByUrl('/nifi-registry/login');
    }
};

NfRegistry.annotations = [
    new ngCore.Component({
        selector: 'nf-registry-app',
        template: require('./nf-registry.html!text'),
        queries: {
            sidenav: new ngCore.ViewChild('sidenav')
        },
        animations: [nfRegistryAnimations.flyInOutAnimation]
    })
];

NfRegistry.parameters = [
    ngCommonHttp.HttpClient,
    NfStorage,
    NfRegistryService,
    NfRegistryApi,
    ngCore.ChangeDetectorRef,
    ngRouter.Router
];

module.exports = NfRegistry;
