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
 * NfRegistryAddUserToGroups constructor.
 *
 * @param nfRegistryApi         The api service.
 * @param tdDataTableService    The covalent data table service module.
 * @param nfRegistryService     The nf-registry.service module.
 * @param matDialogRef          The angular material dialog ref.
 * @param fdsSnackBarService    The FDS snack bar service module.
 * @param data                  The data passed into this component.
 * @constructor
 */
function NfRegistryAddUserToGroups(nfRegistryApi, tdDataTableService, nfRegistryService, matDialogRef, fdsSnackBarService, data) {
    //Services
    this.dataTableService = tdDataTableService;
    this.snackBarService = fdsSnackBarService;
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.dialogRef = matDialogRef;
    this.data = data;
    // local state
    //make an independent copy of the groups for sorting and selecting within the scope of this component
    this.groups = $.extend(true, [], this.nfRegistryService.groups);
    this.filteredUserGroups = [];
    this.isAddToSelectedGroupsDisabled = true;
    this.userGroupsSearchTerms = [];
    this.allGroupsSelected = false;
};

NfRegistryAddUserToGroups.prototype = {
    constructor: NfRegistryAddUserToGroups,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;

        // filter out any groups that
        // 1) that are not configurable
        self.groups = self.groups.filter(function (group) {
            return (group.configurable) ? true : false
        });
        // 2) the user already belongs to
        this.data.user.userGroups.forEach(function (userGroup) {
            self.groups = self.groups.filter(function (group) {
                return (group.identifier !== userGroup.identifier) ? true : false
            });
        });

        this.filterGroups();
        this.deselectAllUserGroups();
        this.determineAllUserGroupsSelectedState();
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
            var arrayLength = this.nfRegistryService.userGroupsColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.nfRegistryService.userGroupsColumns[i].sortable === true) {
                    sortBy = this.nfRegistryService.userGroupsColumns[i].name;
                    //only one column can be actively sorted so we reset all to inactive
                    this.nfRegistryService.userGroupsColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.nfRegistryService.userGroupsColumns[i].active = true;
                    this.nfRegistryService.userGroupsColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newUserGroupsData = this.groups;

        for (var i = 0; i < this.userGroupsSearchTerms.length; i++) {
            newUserGroupsData = this.dataTableService.filterData(newUserGroupsData, this.userGroupsSearchTerms[i], true);
        }

        newUserGroupsData = this.dataTableService.sortData(newUserGroupsData, sortBy, sortOrder);
        this.filteredUserGroups = newUserGroupsData;
    },

    /**
     * Sort `filteredUserGroups` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortUserGroups: function (column) {
        if (column.sortable) {
            var sortBy = column.name;
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterGroups(sortBy, sortOrder);
        }
    },

    /**
     * Checks the `allGroupsSelected` property state and either selects
     * or deselects each of the `filteredUserGroups`.
     */
    toggleUserGroupsSelectAll: function () {
        if (this.allGroupsSelected) {
            this.selectAllUserGroups();
        } else {
            this.deselectAllUserGroups();
        }
    },

    /**
     * Sets the `checked` property of each of the `filteredUserGroups` to true
     * and sets the `isAddToSelectedGroupsDisabled` and the `allGroupsSelected`
     * properties accordingly.
     */
    selectAllUserGroups: function () {
        this.filteredUserGroups.forEach(function (c) {
            c.checked = true;
        });
        this.isAddToSelectedGroupsDisabled = false;
        this.allGroupsSelected = true;
    },

    /**
     * Sets the `checked` property of each group to false
     * and sets the `isAddToSelectedGroupsDisabled` and the `allGroupsSelected`
     * properties accordingly.
     */
    deselectAllUserGroups: function () {
        this.filteredUserGroups.forEach(function (c) {
            c.checked = false;
        });
        this.isAddToSelectedGroupsDisabled = true;
        this.allGroupsSelected = false;
    },

    /**
     * Checks of each of the `filteredUserGroups`'s checked property state
     * and sets the `allBucketsSelected` and `isAddToSelectedGroupsDisabled`
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
            this.isAddToSelectedGroupsDisabled = false;
        } else {
            this.isAddToSelectedGroupsDisabled = true;
        }

        this.allGroupsSelected = allSelected;
    },

    /**
     * Adds users to each of the selected groups.
     */
    addToSelectedGroups: function () {
        var self = this;
        var selectedGroups = this.filteredUserGroups.filter(function (filteredUserGroup) {
            return filteredUserGroup.checked;
        });
        selectedGroups.forEach(function (selectedGroup) {
            selectedGroup.users.push(self.data.user);
            self.nfRegistryApi.updateUserGroup(selectedGroup.identifier, selectedGroup.identity, selectedGroup.users).subscribe(function (group) {
                self.dialogRef.close();
                var snackBarRef = self.snackBarService.openCoaster({
                    title: 'Success',
                    message: 'User has been added to the ' + group.identity + ' group.',
                    verticalPosition: 'bottom',
                    horizontalPosition: 'right',
                    icon: 'fa fa-check-circle-o',
                    color: '#1EB475',
                    duration: 3000
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

NfRegistryAddUserToGroups.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-add-user-to-groups.html!text')
    })
];

NfRegistryAddUserToGroups.parameters = [
    NfRegistryApi,
    covalentCore.TdDataTableService,
    NfRegistryService,
    ngMaterial.MatDialogRef,
    fdsSnackBarsModule.FdsSnackBarService,
    ngMaterial.MAT_DIALOG_DATA
];

module.exports = NfRegistryAddUserToGroups;
