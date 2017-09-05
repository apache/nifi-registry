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
var FdsConfirmDialogComponent = require('@fluid-design-system/confirm-dialog-component');

var IDialogConfig = new ngCore.Class({
    extends: ngMaterial.MdDialogConfig,
    constructor: function () {
        this.title = '';
        this.message = '';
        this.dialogRef = undefined;
        this.viewContainerRef = undefined;
        this.disableClose = true;
    }
});

var IConfirmConfig = new ngCore.Class({
    extends: IDialogConfig,
    constructor: function () {
        this.acceptButton = 'ACCEPT';
        this.acceptButtonColor = 'fds-primary';
        this.cancelButton = 'CANCEL';
        this.cancelButtonColor = 'fds-secondary';
    }
});

function createConfig(config) {
    var dialogConfig = new IConfirmConfig();
    dialogConfig.viewContainerRef = config.viewContainerRef;
    dialogConfig.disableClose = config.disableClose;
    return dialogConfig;
}

/**
 * FdsDialogService constructor.
 *
 * @param MdDialog      The angular material MdDialog.
 * @constructor
 */
function FdsDialogService(MdDialog) {
    this.dialogService = MdDialog;
}

FdsDialogService.prototype = {
    contstructor: FdsDialogService,

    /**
     * Wrapper function over the open() method in MdDialog.
     * Opens a modal dialog containing the given component.
     *
     * @param component     The angular ComponentType<T>.
     * @param config        The angular material MdDialogConfig.
     *
     */
    open: function (component, config) {
        return this.dialogService.open(component, config);
    },

    /**
     * Wrapper function over the closeAll() method in MdDialog.
     * Closes all of the currently-open dialogs.
     */
    closeAll: function () {
        this.dialogService.closeAll();
    },

    /**
     * Opens a confirm dialog with the provided config.
     * Returns an MdDialogRef<TdConfirmDialogComponent> object.
     *
     * @param config     IConfirmConfig {
     *                                      message: string;
     *                                      title?: string;
     *                                      viewContainerRef?: ViewContainerRef;
     *                                      acceptButton?: string;
     *                                      acceptButtonColor?: string;
     *                                      cancelButton?: string;
     *                                      cancelButtonColor?: string;
     *                                   }
     */
    openConfirm: function (config) {
        var dialogConfig = createConfig(config);
        var dialogRef = this.dialogService.open(FdsConfirmDialogComponent, dialogConfig);
        var confirmDialogComponent = dialogRef.componentInstance;
        confirmDialogComponent.dialogRef = dialogRef;
        if (config.title) {
            confirmDialogComponent.title = config.title;
        }
        if (config.message) {
            confirmDialogComponent.message = config.message;
        }
        if (config.acceptButton) {
            confirmDialogComponent.acceptButton = config.acceptButton;
        }
        if (config.acceptButtonColor) {
            confirmDialogComponent.acceptButtonColor = config.acceptButtonColor;
        }
        if (config.cancelButton) {
            confirmDialogComponent.cancelButton = config.cancelButton;
        }
        if (config.cancelButtonColor) {
            confirmDialogComponent.cancelButtonColor = config.cancelButtonColor;
        }
        return dialogRef;
    },
}

FdsDialogService.parameters = [ngMaterial.MdDialog];

module.exports = {
    IDialogConfig: IDialogConfig,
    IConfirmConfig: IConfirmConfig,
    FdsDialogService: FdsDialogService
};