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
var ngMaterial = require('@angular/material');
var ngFlex = require('@angular/flex-layout');
var ngCommon = require('@angular/common');
var ngForms = require('@angular/forms');
var fdsDialogComponentModule = require('@fluid-design-system/dialog-component');
var fdsDialogServiceModule = require('@fluid-design-system/dialog-service');
var FdsConfirmDialogComponent = require('@fluid-design-system/confirm-dialog-component');

var FDS_DIALOGS = [
    fdsDialogComponentModule.FdsDialogComponent,
    fdsDialogComponentModule.FdsDialogTitleDirective,
    fdsDialogComponentModule.FdsDialogActionsDirective,
    fdsDialogComponentModule.FdsDialogContentDirective,
    FdsConfirmDialogComponent
];

var FDS_DIALOGS_ENTRY_COMPONENTS = [
    FdsConfirmDialogComponent
];

/**
 * FdsDialogsModule constructor.
 *
 * @constructor
 */
function FdsDialogsModule() {

};

FdsDialogsModule.prototype = {
    constructor: FdsDialogsModule
};

FdsDialogsModule.annotations = [
    new ngCore.NgModule({
        imports: [
            ngFlex.FlexLayoutModule,
            ngForms.FormsModule,
            ngCommon.CommonModule,
            ngMaterial.MdDialogModule,
            ngMaterial.MdInputModule,
            ngMaterial.MdButtonModule,
            ngMaterial.MdIconModule
        ],
        declarations: [
            FDS_DIALOGS
        ],
        exports: [
            FDS_DIALOGS
        ],
        providers: [
            fdsDialogServiceModule.FdsDialogService
        ],
        entryComponents: [
            FDS_DIALOGS_ENTRY_COMPONENTS
        ]
    })
];

module.exports = {
    FdsDialogsModule: FdsDialogsModule,
    IConfirmConfig: fdsDialogServiceModule.IConfirmConfig,
    FdsDialogService: fdsDialogServiceModule.FdsDialogService,
    FdsDialogComponent: fdsDialogComponentModule.FdsDialogComponent,
    FdsDialogTitleDirective: fdsDialogComponentModule.FdsDialogTitleDirective,
    FdsDialogContentDirective: fdsDialogComponentModule.FdsDialogContentDirective,
    FdsDialogActionsDirective: fdsDialogComponentModule.FdsDialogActionsDirective,
    FdsConfirmDialogComponent: FdsConfirmDialogComponent
};
