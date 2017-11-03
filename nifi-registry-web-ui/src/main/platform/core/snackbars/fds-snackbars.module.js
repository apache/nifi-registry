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
var fdsSnackBarComponentModule = require('@fluid-design-system/snackbar-component');
var fdsSnackBarServiceModule = require('@fluid-design-system/snackbar-service');
var FdsCoasterComponent = require('@fluid-design-system/coaster-component');

var FDS_SNACKBARS = [
    fdsSnackBarComponentModule.FdsSnackBarComponent,
    fdsSnackBarComponentModule.FdsSnackBarTitleDirective,
    fdsSnackBarComponentModule.FdsSnackBarActionsDirective,
    fdsSnackBarComponentModule.FdsSnackBarContentDirective,
    FdsCoasterComponent
];

var FDS_SNACKBARS_ENTRY_COMPONENTS = [
    FdsCoasterComponent
];

/**
 * FdsSnackBarsModule constructor.
 *
 * @constructor
 */
function FdsSnackBarsModule() {

};

FdsSnackBarsModule.prototype = {
    constructor: FdsSnackBarsModule
};

FdsSnackBarsModule.annotations = [
    new ngCore.NgModule({
        imports: [
            ngFlex.FlexLayoutModule,
            ngForms.FormsModule,
            ngCommon.CommonModule,
            ngMaterial.MatSnackBarModule,
            ngMaterial.MatInputModule,
            ngMaterial.MatButtonModule,
            ngMaterial.MatIconModule
        ],
        declarations: [
            FDS_SNACKBARS
        ],
        exports: [
            FDS_SNACKBARS
        ],
        providers: [
            fdsSnackBarServiceModule.FdsSnackBarService
        ],
        entryComponents: [
            FDS_SNACKBARS_ENTRY_COMPONENTS
        ]
    })
];

module.exports = {
    FdsSnackBarsModule: FdsSnackBarsModule,
    ICoasterConfig: fdsSnackBarServiceModule.ICoasterConfig,
    FdsSnackBarService: fdsSnackBarServiceModule.FdsSnackBarService,
    FdsSnackBarComponent: fdsSnackBarComponentModule.FdsSnackBarComponent,
    FdsSnackBarTitleDirective: fdsSnackBarComponentModule.FdsSnackBarTitleDirective,
    FdsSnackBarContentDirective: fdsSnackBarComponentModule.FdsSnackBarContentDirective,
    FdsSnackBarActionsDirective: fdsSnackBarComponentModule.FdsSnackBarActionsDirective,
    FdsCoasterComponent: FdsCoasterComponent
};
