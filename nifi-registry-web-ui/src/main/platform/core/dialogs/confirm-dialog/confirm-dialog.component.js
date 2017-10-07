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

/**
 * FdsConfirmDialogComponent constructor.
 *
 * @constructor
 */
function FdsConfirmDialogComponent() {
    this.title = '';
    this.message = '';
    this.acceptButton = '';
    this.acceptButtonColor = 'fds-primary';
    this.cancelButton = '';
    this.cancelButtonColor = 'fds-regular';
    this.dialogRef = undefined;
    this.viewContainerRef = undefined;
    this.disableClose = true;
};

FdsConfirmDialogComponent.prototype = {
    constructor: FdsConfirmDialogComponent,

    /**
     * Close the dialog and send a cancel response to any subscribers.
     */
    cancel: function () {
        this.dialogRef.close(false);
    },

    /**
     * Close the dialog and send an accept response to any subscribers.
     */
    accept: function () {
        this.dialogRef.close(true);
    }
};

FdsConfirmDialogComponent.annotations = [
    new ngCore.Component({
        selector: 'fds-confirm-dialog',
        template: require('./confirm-dialog.component.html!text')
    })
];

FdsConfirmDialogComponent.parameters = [];

module.exports = FdsConfirmDialogComponent;
