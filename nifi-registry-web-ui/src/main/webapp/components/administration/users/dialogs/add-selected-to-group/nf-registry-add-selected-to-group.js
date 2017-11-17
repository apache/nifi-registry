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
var ngMaterial = require('@angular/material');

/**
 * NfRegistryAddSelectedToGroup constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param matDialogRef          The angular material dialog ref.
 * @constructor
 */
function NfRegistryAddSelectedToGroup(nfRegistryService, matDialogRef) {
    this.nfRegistryService = nfRegistryService;
    this.dialogRef = matDialogRef;
    this.selectedGroups = [];
};

NfRegistryAddSelectedToGroup.prototype = {
    constructor: NfRegistryAddSelectedToGroup,

    /**
     * Add selected users and groups to an existing group.
     *
     * @param groupIds     The group ids .
     */
    addSelectedToGroup: function (groupIds) {
        if(!this.nfRegistryService.isMultiUserGroupActionsDisabled) {
            var self = this;
            var selectedUsers = this.nfRegistryService.filteredUsers.filter(function (filteredUser) {
                return filteredUser.checked;
            });

            groupIds.forEach(function (groupId) {
                this.nfRegistryService.api.getUserGroup(groupId).subscribe(function (group) {
                    this.nfRegistryService.api.updateUserGroup(groupId, selectedUsers.concat(group.users));
                });
            });
        }
    },

    /**
     * Cancel creation of a new bucket and close dialog.
     */
    cancel: function () {
        this.dialogRef.close();
    }
};

NfRegistryAddSelectedToGroup.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-add-selected-to-group.html!text')
    })
];

NfRegistryAddSelectedToGroup.parameters = [
    NfRegistryService,
    ngMaterial.MatDialogRef
];

module.exports = NfRegistryAddSelectedToGroup;
