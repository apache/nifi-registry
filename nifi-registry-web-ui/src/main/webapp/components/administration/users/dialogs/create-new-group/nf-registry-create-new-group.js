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
 * NfRegistryCreateNewGroup constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param matDialogRef          The angular material dialog ref.
 * @constructor
 */
function NfRegistryCreateNewGroup(nfRegistryService, matDialogRef) {
    this.nfRegistryService = nfRegistryService;
    this.dialogRef = matDialogRef;
    this.keepDialogOpen = false;
};

NfRegistryCreateNewGroup.prototype = {
    constructor: NfRegistryCreateNewGroup,

    /**
     * Create a new group.
     *
     * @param createNewGroupInput     The createNewGroupInput element.
     */
    createNewGroup: function (createNewGroupInput) {
        if(!this.nfRegistryService.isMultiUserGroupActionsDisabled) {
            var self = this;
            var selectedUsers = this.nfRegistryService.filteredUsers.filter(function (filteredUser) {
                return filteredUser.checked;
            });
            var selectedUserGroups = this.nfRegistryService.filteredUserGroups.filter(function (filteredUserGroup) {
                return filteredUserGroup.checked;
            });

            this.nfRegistryService.api.createNewGroup(null, createNewGroupInput.value, selectedUsers.concat(selectedUserGroups)).subscribe(function (group) {
                self.nfRegistryService.groups.push(group);
                self.nfRegistryService.filterUsersAndGroups();
                self.nfRegistryService.allUsersAndGroupsSelected = false;
                if (self.keepDialogOpen !== true) {
                    self.dialogRef.close();
                }
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

NfRegistryCreateNewGroup.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-create-new-group.html!text')
    })
];

NfRegistryCreateNewGroup.parameters = [
    NfRegistryService,
    ngMaterial.MatDialogRef
];

module.exports = NfRegistryCreateNewGroup;
