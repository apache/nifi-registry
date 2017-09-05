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
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var ngRouter = require('@angular/router');

/**
 * NfRegistryUserDetails constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param Router                The angular router module.
 * @constructor
 */
function NfRegistryUserDetails(nfRegistryService, Router) {
    this.nfRegistryService = nfRegistryService;
    this.router = Router;
};

NfRegistryUserDetails.prototype = {
    constructor: NfRegistryUserDetails,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        this.nfRegistryService.sidenav.open();
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.sidenav.close();
    },

    /**
     * Navigate to administer users for current registry.
     */
    closeSideNav: function () {
        this.router.navigateByUrl('/nifi-registry/administration/' + this.nfRegistryService.registry.id + '/users');
    }
};

NfRegistryUserDetails.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-user-details.html!text')
    })
];

NfRegistryUserDetails.parameters = [NfRegistryService, ngRouter.Router];

module.exports = NfRegistryUserDetails;
