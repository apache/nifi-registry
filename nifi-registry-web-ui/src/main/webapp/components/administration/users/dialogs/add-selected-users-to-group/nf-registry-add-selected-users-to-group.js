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

var covalentCore = require('@covalent/core');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var ngCore = require('@angular/core');
var fdsSnackBarsModule = require('@fluid-design-system/snackbars');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var ngMaterial = require('@angular/material');
var $ = require('jquery');

/**
 * NfRegistryAddSelectedToGroup constructor.
 *
 * @param nfRegistryApi         The api service.
 * @param TdDataTableService    The covalent data table service module.
 * @param nfRegistryService     The nf-registry.service module.
 * @param matDialogRef          The angular material dialog ref.
 * @param FdsSnackBarService    The FDS snack bar service module.
 * @constructor
 */
function NfRegistryAddSelectedToGroup(nfRegistryApi, TdDataTableService, nfRegistryService, matDialogRef, FdsSnackBarService) {
    this.dataTableService = TdDataTableService;
    this.snackBarService = FdsSnackBarService;
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.dialogRef = matDialogRef;
    this.filteredUserGroups = [];
    //make an independent copy of the groups for sorting and selecting within the scope of this component
    this.groups = $.extend(true, [], this.nfRegistryService.groups);
    this.selectedGroups = [];
    this.allUsersAndGroupsSelected = false;
    this.isAddSelectedUsersToSelectedGroupsDisabled = true;
    this.userGroupsSearchTerms = [];
    this.userGroupsColumns = [
        {
            name: 'identity',
            label: 'Display Name',
            sortable: true,
            tooltip: 'User name.',
            width: 100
        }
    ];
};

NfRegistryAddSelectedToGroup.prototype = {
    constructor: NfRegistryAddSelectedToGroup,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        this.filterGroups();
    },

    /**
     * Filter groups.
     *
     * @param {string} [sortBy]       The column name to sort `userGroupsColumns` by.
     * @param {string} [sortOrder]    The order. Either 'ASC' or 'DES'
     */
    filterGroups: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in `dropletColumns`
        if (sortBy === undefined) {
            var arrayLength = this.userGroupsColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.userGroupsColumns[i].sortable === true) {
                    sortBy = this.userGroupsColumns[i].name;
                    //only one column can be actively sorted so we reset all to inactive
                    this.userGroupsColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.userGroupsColumns[i].active = true;
                    this.userGroupsColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newUserGroupsData = this.groups;

        for (var i = 0; i < this.userGroupsSearchTerms.length; i++) {
            newUserGroupsData = this.nfRegistryService.filterData(newUserGroupsData, this.userGroupsSearchTerms[i], true);
        }

        newUserGroupsData = this.dataTableService.sortData(newUserGroupsData, sortBy, sortOrder);
        this.filteredUserGroups = newUserGroupsData;
    },

    /**
     * Sort `filteredUserGroups` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortUserGroups: function (sortEvent, column) {
        if (column.sortable) {
            var sortBy = column.name;
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterGroups(sortBy, sortOrder);
        }
    },

    /**
     * Checks the `allUsersAndGroupsSelected` property state and either selects
     * or deselects each of the `filteredUserGroups`.
     */
    toggleUserGroupsSelectAll: function () {
        if (this.allUsersAndGroupsSelected) {
            this.selectAllUserGroups();
        } else {
            this.deselectAllUserGroups();
        }
    },

    /**
     * Sets the `checked` property of each of the `filteredUserGroups` to true
     * and sets the `isAddSelectedUsersToSelectedGroupsDisabled` and the `allUsersAndGroupsSelected`
     * properties accordingly.
     */
    selectAllUserGroups: function () {
        this.filteredUserGroups.forEach(function (c) {
            c.checked = true;
        });
        this.isAddSelectedUsersToSelectedGroupsDisabled = false;
        this.allUsersAndGroupsSelected = true;
    },

    /**
     * Sets the `checked` property of each group to false
     * and sets the `isAddSelectedUsersToSelectedGroupsDisabled` and the `allUsersAndGroupsSelected`
     * properties accordingly.
     */
    deselectAllUserGroups: function () {
        this.filteredUserGroups.forEach(function (c) {
            c.checked = false;
        });
        this.isAddSelectedUsersToSelectedGroupsDisabled = true;
        this.allUsersAndGroupsSelected = false;
    },

    /**
     * Checks of each of the `filteredUserGroups`'s checked property state
     * and sets the `allBucketsSelected` and `isAddSelectedUsersToSelectedGroupsDisabled`
     * property accordingly.
     */
    determineAllUserGroupsSelectedState: function () {
        var selected = 0;
        var allSelected = true;
        this.filteredUserGroups.forEach(function (c) {
            if (c.checked) {
                selected++;
            }
            if (c.checked === undefined || c.checked === false) {
                allSelected = false;
            }
        });

        if (selected > 0) {
            this.isAddSelectedUsersToSelectedGroupsDisabled = false;
        } else {
            this.isAddSelectedUsersToSelectedGroupsDisabled = true;
        }

        this.allUsersAndGroupsSelected = allSelected;
    },

    /**
     * Adds the selected users to each of the selected groups.
     */
    addSelectedUsersToSelectedGroups: function () {
        var self = this;
        var selectedUsers = this.nfRegistryService.filteredUsers.filter(function (filteredUser) {
            return filteredUser.checked;
        });
        var groupIds = this.filteredUserGroups.filter(function (filteredUserGroup) {
            return filteredUserGroup.checked;
        });
        groupIds.forEach(function (groupId) {
            self.nfRegistryApi.getUserGroup(groupId.identifier).subscribe(function (group) {
                self.nfRegistryApi.updateUserGroup(groupId.identifier, groupId.identity, selectedUsers.concat(group.users)).subscribe(function (group) {
                    self.dialogRef.close();
                    var snackBarRef = self.snackBarService.openCoaster({
                        title: 'Success',
                        message: 'Selected users have been added to the ' + group.identity + ' group.',
                        verticalPosition: 'bottom',
                        horizontalPosition: 'right',
                        icon: 'fa fa-check-circle-o',
                        color: '#1EB475',
                        duration: 3000
                    });
                });
            });
        });
    },

    /**
     * Cancel adding selected users to groups and close the dialog.
     */
    cancel: function () {
        this.dialogRef.close();
    }
};

NfRegistryAddSelectedToGroup.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-add-selected-users-to-group.html!text')
    })
];

NfRegistryAddSelectedToGroup.parameters = [
    NfRegistryApi,
    covalentCore.TdDataTableService,
    NfRegistryService,
    ngMaterial.MatDialogRef,
    fdsSnackBarsModule.FdsSnackBarService
];

module.exports = NfRegistryAddSelectedToGroup;
