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

import { Component } from '@angular/core';
import NfRegistryService from 'services/nf-registry.service';
import NfRegistryApi from 'services/nf-registry.api';
import { MatDialogRef } from '@angular/material';
import { NfRegistryLoginAuthGuard } from 'services/nf-registry.auth-guard.service';
import template from './nf-registry-user-login.html';

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
                self.nfRegistryLoginAuthGuard.checkLogin(self.nfRegistryService.redirectUrl);
            }
        });
    }
};

NfRegistryUserLogin.annotations = [
    new Component({
        template: template
    })
];

NfRegistryUserLogin.parameters = [
    NfRegistryApi,
    NfRegistryService,
    MatDialogRef,
    NfRegistryLoginAuthGuard
];

export default NfRegistryUserLogin;
