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

var FdsDialogTitleDirective = new ngCore.Class({
    extends: ngCore.Directive({selector: 'fds-dialog-title'}),
    constructor: function () {
    }
});
var FdsDialogContentDirective = new ngCore.Class({
    extends: ngCore.Directive({selector: 'fds-dialog-content'}),
    constructor: function () {
    }
});
var FdsDialogActionsDirective = new ngCore.Class({
    extends: ngCore.Directive({selector: 'fds-dialog-actions'}),
    constructor: function () {
    }
});

/**
 * FdsDialogComponent constructor
 *
 * @constructor
 */
function FdsDialogComponent() {
};

FdsDialogComponent.prototype = {
    constructor: FdsDialogComponent,

    /**
     * Respond after Angular projects external content into the component's view.
     */
    ngAfterContentInit: function () {
        if (this.dialogTitle.length > 1) {
            throw new Error('Duplicate fds-dialog-title component at in fds-dialog.');
        }
        if (this.dialogContent.length > 1) {
            throw new Error('Duplicate fds-dialog-content component at in fds-dialog.');
        }
        if (this.dialogActions.length > 1) {
            throw new Error('Duplicate fds-dialog-actions component at in fds-dialog.');
        }
    }
}

FdsDialogComponent.annotations = [
    new ngCore.Component({
        selector: 'fds-dialog',
        template: require('./fds-dialog.component.html!text'),
        queries: {
            dialogTitle: new ngCore.ContentChildren(FdsDialogTitleDirective),
            dialogContent: new ngCore.ContentChildren(FdsDialogContentDirective),
            dialogActions: new ngCore.ContentChildren(FdsDialogActionsDirective)
        }
    })
];

FdsDialogComponent.parameters = [];

module.exports = {
    FdsDialogTitleDirective: FdsDialogTitleDirective,
    FdsDialogContentDirective: FdsDialogContentDirective,
    FdsDialogActionsDirective: FdsDialogActionsDirective,
    FdsDialogComponent: FdsDialogComponent
};