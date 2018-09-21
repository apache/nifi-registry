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
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var ngMaterial = require('@angular/material');
var nfRegistryAuthGuardService = require('nifi-registry/services/nf-registry.auth-guard.service.js');

/**
 * NfRegistryUserLogin constructor.
 *
 * @param nfRegistryApi                     The api service.
 * @param nfRegistryService                 The nf-registry.service module.
 * @param matDialogRef                      The angular material dialog ref.
 * @param nfRegistryLoginAuthGuard          The login auth guard.
 * @constructor
 */
function NfRegistryUserLogin(nfRegistryApi, nfRegistryService, matDialogRef, nfRegistryLoginAuthGuard) {
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.dialogRef = matDialogRef;
    this.nfRegistryLoginAuthGuard = nfRegistryLoginAuthGuard;
};

NfRegistryUserLogin.prototype = {
    constructor: NfRegistryUserLogin,

    /**
     * Submit login form.
     *
     * @param username  The user name.
     * @param password  The password.
     */
    login: function (username, password) {
        var self = this;
        this.nfRegistryApi.postToLogin(username.value, password.value).subscribe(function(response){
            if(response || response.status === 200) {
                //successful login
                self.dialogRef.close();
                self.nfRegistryService.currentUser.anonymous = false;
                self.nfRegistryLoginAuthGuard.checkLogin(self.nfRegistryService.redirectUrl)
            }
        });
    }
};

NfRegistryUserLogin.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-user-login.html!text')
    })
];

NfRegistryUserLogin.parameters = [
    NfRegistryApi,
    NfRegistryService,
    ngMaterial.MatDialogRef,
    nfRegistryAuthGuardService.NfRegistryLoginAuthGuard
];

module.exports = NfRegistryUserLogin;
