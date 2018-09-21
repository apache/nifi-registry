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
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');
var fdsDialogsModule = require('@flow-design-system/dialogs');
var ngRouter = require('@angular/router');

/**
 * NfLoginComponent constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param fdsDialogService      The FDS dialog service.
 * @param router                The angular router module.
 */
function NfPageNotFoundComponent(nfRegistryService, fdsDialogService, router) {
    // Services
    this.nfRegistryService = nfRegistryService;
    this.dialogService = fdsDialogService;
    this.router = router;
};

NfPageNotFoundComponent.prototype = {
    constructor: NfPageNotFoundComponent,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.perspective = 'not-found';
        this.dialogService.openConfirm({
            title: 'Page Not Found',
            acceptButton: 'Home',
            acceptButtonColor: 'fds-warn'
        }).afterClosed().subscribe(
            function (accept) {
                if (accept) {
                    self.router.navigateByUrl(self.nfRegistryService.redirectUrl);
                }
            });
    }
};

NfPageNotFoundComponent.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-page-not-found.html!text'),
        animations: [nfRegistryAnimations.slideInLeftAnimation],
        host: {
            '[@routeAnimation]': 'routeAnimation'
        }
    })
];

NfPageNotFoundComponent.parameters = [
    NfRegistryService,
    fdsDialogsModule.FdsDialogService,
    ngRouter.Router
];

module.exports = NfPageNotFoundComponent;
