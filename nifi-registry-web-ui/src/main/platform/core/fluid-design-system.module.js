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

var $ = require('jquery');
var ngCore = require('@angular/core');
var ngFlex = require('@angular/flex-layout');
var ngMaterial = require('@angular/material');
var ngCommon = require('@angular/common');
var ngPlatformBrowser = require('@angular/platform-browser');
var ngAnimations = require('@angular/platform-browser/animations');
var covalentCore = require('@covalent/core');
var fdsDialogsModule = require('@fluid-design-system/dialogs');
var fdsSnackBarsModule = require('@fluid-design-system/snackbars');

/**
 * FluidDesignSystemModule constructor.
 *
 * @constructor
 */
function FluidDesignSystemModule() {
    $(document).ready(function () {
        //add fds attr to body tag to allow fine grain style overrides
        document.body.setAttribute('fds', '');

        //override the hover styles for checkbox borders
        $(document.body).on('mouseenter', '.mat-checkbox-inner-container', function () {
            $(this).find('.mat-checkbox-frame').css('border-color', '#1491C1');
        });
        $(document.body).on('mouseleave', '.mat-checkbox-inner-container', function () {
            $(this).find('.mat-checkbox-frame').css('border-color', '#DDDDDD');
        });
    });
};

FluidDesignSystemModule.prototype = {
    constructor: FluidDesignSystemModule
};

FluidDesignSystemModule.annotations = [
    new ngCore.NgModule({
        imports: [
            ngFlex.FlexLayoutModule,
            ngAnimations.BrowserAnimationsModule,
            ngCommon.CommonModule,
            ngPlatformBrowser.BrowserModule,
            ngMaterial.MatAutocompleteModule,
            ngMaterial.MatButtonModule,
            ngMaterial.MatButtonToggleModule,
            ngMaterial.MatCardModule,
            ngMaterial.MatCheckboxModule,
            ngMaterial.MatChipsModule,
            ngMaterial.MatDatepickerModule,
            ngMaterial.MatDialogModule,
            ngMaterial.MatExpansionModule,
            ngMaterial.MatFormFieldModule,
            ngMaterial.MatGridListModule,
            ngMaterial.MatIconModule,
            ngMaterial.MatInputModule,
            ngMaterial.MatListModule,
            ngMaterial.MatMenuModule,
            ngMaterial.MatProgressBarModule,
            ngMaterial.MatProgressSpinnerModule,
            ngMaterial.MatRadioModule,
            ngMaterial.MatSelectModule,
            ngMaterial.MatSlideToggleModule,
            ngMaterial.MatSliderModule,
            ngMaterial.MatSidenavModule,
            ngMaterial.MatSnackBarModule,
            ngMaterial.MatStepperModule,
            ngMaterial.MatTabsModule,
            ngMaterial.MatToolbarModule,
            ngMaterial.MatTooltipModule,
            ngMaterial.MatPaginatorModule,
            ngMaterial.MatSortModule,
            ngMaterial.MatTableModule,
            covalentCore.CovalentCommonModule,
            covalentCore.CovalentChipsModule,
            covalentCore.CovalentDataTableModule,
            covalentCore.CovalentDialogsModule,
            fdsDialogsModule.FdsDialogsModule,
            fdsSnackBarsModule.FdsSnackBarsModule,
            covalentCore.CovalentExpansionPanelModule,
            covalentCore.CovalentLoadingModule,
            covalentCore.CovalentMenuModule,
            covalentCore.CovalentNotificationsModule,
            covalentCore.CovalentPagingModule,
            covalentCore.CovalentSearchModule,
            covalentCore.CovalentStepsModule
        ],
        exports: [
            ngFlex.FlexLayoutModule,
            ngAnimations.BrowserAnimationsModule,
            ngCommon.CommonModule,
            ngPlatformBrowser.BrowserModule,
            ngMaterial.MatAutocompleteModule,
            ngMaterial.MatButtonModule,
            ngMaterial.MatButtonToggleModule,
            ngMaterial.MatCardModule,
            ngMaterial.MatCheckboxModule,
            ngMaterial.MatChipsModule,
            ngMaterial.MatDatepickerModule,
            ngMaterial.MatDialogModule,
            ngMaterial.MatExpansionModule,
            ngMaterial.MatFormFieldModule,
            ngMaterial.MatGridListModule,
            ngMaterial.MatIconModule,
            ngMaterial.MatInputModule,
            ngMaterial.MatListModule,
            ngMaterial.MatMenuModule,
            ngMaterial.MatProgressBarModule,
            ngMaterial.MatProgressSpinnerModule,
            ngMaterial.MatRadioModule,
            ngMaterial.MatSelectModule,
            ngMaterial.MatSlideToggleModule,
            ngMaterial.MatSliderModule,
            ngMaterial.MatSidenavModule,
            ngMaterial.MatSnackBarModule,
            ngMaterial.MatStepperModule,
            ngMaterial.MatTabsModule,
            ngMaterial.MatToolbarModule,
            ngMaterial.MatTooltipModule,
            ngMaterial.MatPaginatorModule,
            ngMaterial.MatSortModule,
            ngMaterial.MatTableModule,
            covalentCore.CovalentCommonModule,
            covalentCore.CovalentChipsModule,
            covalentCore.CovalentDataTableModule,
            covalentCore.CovalentDialogsModule,
            fdsDialogsModule.FdsDialogsModule,
            fdsSnackBarsModule.FdsSnackBarsModule,
            covalentCore.CovalentExpansionPanelModule,
            covalentCore.CovalentLoadingModule,
            covalentCore.CovalentMenuModule,
            covalentCore.CovalentNotificationsModule,
            covalentCore.CovalentPagingModule,
            covalentCore.CovalentSearchModule,
            covalentCore.CovalentStepsModule
        ]
    })
];
module.exports = FluidDesignSystemModule;
