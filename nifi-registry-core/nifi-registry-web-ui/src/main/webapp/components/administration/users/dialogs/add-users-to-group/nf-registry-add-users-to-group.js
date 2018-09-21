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
var fdsSnackBarsModule = require('@flow-design-system/snackbars');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var ngMaterial = require('@angular/material');
var $ = require('jquery');

/**
 * NfRegistryAddUsersToGroup constructor.
 *
 * @param nfRegistryApi         The api service.
 * @param tdDataTableService    The covalent data table service module.
 * @param nfRegistryService     The nf-registry.service module.
 * @param matDialogRef          The angular material dialog ref.
 * @param fdsSnackBarService    The FDS snack bar service module.
 * @param data                  The data passed into this component.
 * @constructor
 */
function NfRegistryAddUsersToGroup(nfRegistryApi, tdDataTableService, nfRegistryService, matDialogRef, fdsSnackBarService, data) {
    //  Services
    this.dataTableService = tdDataTableService;
    this.snackBarService = fdsSnackBarService;
    this.nfRegistryService = nfRegistryService;
    this.nfRegistryApi = nfRegistryApi;
    this.dialogRef = matDialogRef;
    this.data = data;

    // local state
    //make an independent copy of the users for sorting and selecting within the scope of this component
    this.users = $.extend(true, [], this.nfRegistryService.users);
    this.filteredUsers = [];
    this.isAddSelectedUsersToGroupDisabled = true;
    this.usersSearchTerms = [];
    this.allUsersSelected = false;
    this.usersColumns = [
        {
            name: 'identity',
            label: 'Display Name',
            sortable: true,
            tooltip: 'Group name.',
            width: 100
        }
    ];
};

NfRegistryAddUsersToGroup.prototype = {
    constructor: NfRegistryAddUsersToGroup,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;

        this.data.group.users.forEach(function (groupUser) {
            self.users = self.users.filter(function (user) {
                return (user.identifier !== groupUser.identifier) ? true : false
            });
        });

        this.filterUsers();
        this.deselectAllUsers();
        this.determineAllUsersSelectedState();
    },

    /**
     * Filter users.
     *
     * @param {string} [sortBy]       The column name to sort `usersColumns` by.
     * @param {string} [sortOrder]    The order. Either 'ASC' or 'DES'
     */
    filterUsers: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in `dropletColumns`
        if (sortBy === undefined) {
            var arrayLength = this.usersColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.usersColumns[i].sortable === true) {
                    sortBy = this.usersColumns[i].name;
                    //only one column can be actively sorted so we reset all to inactive
                    this.usersColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.usersColumns[i].active = true;
                    this.usersColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newUsersData = this.users;

        for (var i = 0; i < this.usersSearchTerms.length; i++) {
            newUsersData = this.dataTableService.filterData(newUsersData, this.usersSearchTerms[i], true);
        }

        newUsersData = this.dataTableService.sortData(newUsersData, sortBy, sortOrder);
        this.filteredUsers = newUsersData;
    },

    /**
     * Sort `filteredUsers` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortUsers: function (column) {
        if (column.sortable) {
            var sortBy = column.name;
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterUsers(sortBy, sortOrder);
        }
    },

    /**
     * Checks the `allUsersSelected` property state and either selects
     * or deselects each of the `filteredUsers`.
     */
    toggleUsersSelectAll: function () {
        if (this.allUsersSelected) {
            this.selectAllUsers();
        } else {
            this.deselectAllUsers();
        }
    },

    /**
     * Sets the `checked` property of each of the `filteredUsers` to true
     * and sets the `isAddSelectedUsersToGroupDisabled` and the `allUsersSelected`
     * properties accordingly.
     */
    selectAllUsers: function () {
        this.filteredUsers.forEach(function (c) {
            c.checked = true;
        });
        this.isAddSelectedUsersToGroupDisabled = false;
        this.allUsersSelected = true;
    },

    /**
     * Sets the `checked` property of each group to false
     * and sets the `isAddSelectedUsersToGroupDisabled` and the `allUsersSelected`
     * properties accordingly.
     */
    deselectAllUsers: function () {
        this.filteredUsers.forEach(function (c) {
            c.checked = false;
        });
        this.isAddSelectedUsersToGroupDisabled = true;
        this.allUsersSelected = false;
    },

    /**
     * Checks of each of the `filteredUsers`'s checked property state
     * and sets the `allBucketsSelected` and `isAddSelectedUsersToGroupDisabled`
     * property accordingly.
     */
    determineAllUsersSelectedState: function () {
        var selected = 0;
        var allSelected = true;
        this.filteredUsers.forEach(function (c) {
            if (c.checked) {
                selected++;
            }
            if (c.checked === undefined || c.checked === false) {
                allSelected = false;
            }
        });

        if (selected > 0) {
            this.isAddSelectedUsersToGroupDisabled = false;
        } else {
            this.isAddSelectedUsersToGroupDisabled = true;
        }

        this.allUsersSelected = allSelected;
    },

    /**
     * Adds each of the selected users to this group.
     */
    addSelectedUsersToGroup: function () {
        var self = this;
        this.filteredUsers.filter(function (filteredUser) {
            if(filteredUser.checked) {
                self.data.group.users.push(filteredUser);
            }
        });
        this.nfRegistryApi.updateUserGroup(self.data.group.identifier, self.data.group.identity, self.data.group.users).subscribe(function (group) {
            self.dialogRef.close();
            var snackBarRef = self.snackBarService.openCoaster({
                title: 'Success',
                message: 'Selected users have been added to the ' + self.data.group.identity + ' group.',
                verticalPosition: 'bottom',
                horizontalPosition: 'right',
                icon: 'fa fa-check-circle-o',
                color: '#1EB475',
                duration: 3000
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

NfRegistryAddUsersToGroup.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-add-users-to-group.html!text')
    })
];

NfRegistryAddUsersToGroup.parameters = [
    NfRegistryApi,
    covalentCore.TdDataTableService,
    NfRegistryService,
    ngMaterial.MatDialogRef,
    fdsSnackBarsModule.FdsSnackBarService,
    ngMaterial.MAT_DIALOG_DATA
];

module.exports = NfRegistryAddUsersToGroup;
