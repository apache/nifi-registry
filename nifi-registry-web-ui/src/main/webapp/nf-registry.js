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

function NfRegistry(nfRegistryService, changeDetectorRef) {
    this.nfRegistryService = nfRegistryService;
    this.cd = changeDetectorRef;
};

NfRegistry.prototype = {
    constructor: NfRegistry,

    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.sidenav = this.sidenav;
        this.nfRegistryService.getRegistries().then(function (registries) {
            self.nfRegistryService.registries = registries;
        });
    },

    ngAfterViewChecked: function () {
        // since the child views are updating the nfRegistryService values that are used to display
        // the breadcrumbs in this component's view we need to manually detect changes at the correct
        // point in the lifecycle.
        this.cd.detectChanges();
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

NfRegistry.parameters = [NfRegistryService, ngCore.ChangeDetectorRef];

module.exports = NfRegistry;
