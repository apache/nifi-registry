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
var FdsCoasterComponent = require('@fluid-design-system/coaster-component');
var $ = require('jquery');

var ISnackBarConfig = new ngCore.Class({
    extends: ngMaterial.MatSnackBarConfig,
    constructor: function () {
        this.title = '';
        this.message = '';
        this.snackBarRef = undefined;
        this.viewContainerRef = undefined;
    }
});

var ICoasterConfig = new ngCore.Class({
    extends: ISnackBarConfig,
    constructor: function () {
        this.icon = '';
        this.color = '';
    }
});

/**
 * FdsSnackBarService constructor.
 *
 * @param MatSnackBar      The angular material MatSnackBar.
 * @constructor
 */
function FdsSnackBarService(MatSnackBar) {
    this.snackBarService = MatSnackBar;
}

FdsSnackBarService.prototype = {
    contstructor: FdsSnackBarService,

    /**
     * Wrapper function over the open() method in MatSnackBar.
     *
     * @param message               The message to show in the snackbar.
     * @param action                The label for the snackbar action.
     * @param config                Additional configuration options for the snackbar.
     *
     * @returns {MatSnackBarRef}    The reference to the snackbar.
     */
    open: function (message, action, config) {
        return this.snackBarService.open(message, action, config);
    },

    /**
     * Wrapper function over the openFromComponent() method in MatSnackBar.
     * Opens a snackbar containing the given component.
     *
     * @param component     The angular ComponentType<T>.
     * @param config        The angular material MatSnackBarConfig.
     *
     * @returns {MatSnackBarRef}    The reference to the snackbar.
     */
    openFromComponent: function (component, config) {
        return this.snackBarService.openFromComponent(component, config);
    },

    /**
     * Wrapper function over the dismiss() method in MatSnackBar.
     * Dismisses the currently-open snackbar.
     */
    dismiss: function () {
        this.snackBarService.dismiss();
    },

    /**
     * Opens a coaster snackbar with the provided config.
     *
     * @param config     ICoasterConfig {
     *                                      message?: string;
     *                                      title?: string;
     *                                      snackBarRef?: MatSnackBarRef;
     *                                      viewContainerRef?: ViewContainerRef;
     *                                      icon?: string;
     *                                      color?: string;
     *                                   }
     *
     * @returns {MatSnackBarRef}    The reference to the snackbar.
     */
    openCoaster: function (config) {
        var snackBarConfig = new ICoasterConfig();
        snackBarConfig.verticalPosition = config.verticalPosition;
        snackBarConfig.horizontalPosition = config.horizontalPosition;
        snackBarConfig.duration = config.duration;
        var snackBarRef = this.snackBarService.openFromComponent(FdsCoasterComponent, snackBarConfig);
        var coasterComponent = snackBarRef.instance;
        coasterComponent.snackBarRef = snackBarRef;
        if (config.title) {
            coasterComponent.title = config.title;
        }
        if (config.message) {
            coasterComponent.message = config.message;
        }
        if (config.icon) {
            coasterComponent.icon = config.icon;
        }
        if (config.color) {
            coasterComponent.color = config.color;
        }
        return snackBarRef;
    },
}

FdsSnackBarService.parameters = [ngMaterial.MatSnackBar];

module.exports = {
    ISnackBarConfig: ISnackBarConfig,
    ICoasterConfig: ICoasterConfig,
    FdsSnackBarService: FdsSnackBarService
};