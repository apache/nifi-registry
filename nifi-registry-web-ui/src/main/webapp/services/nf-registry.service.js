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
var ngRouter = require('@angular/router');
var fdsDialogsModule = require('@fluid-design-system/dialogs');
var fdsSnackBarsModule = require('@fluid-design-system/snackbars');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var NfStorage = require('nifi-registry/services/nf-storage.service.js');
var rxjs = require('rxjs/Rx');
require('rxjs/add/operator/catch');
require('rxjs/add/operator/map');

/**
 * NfRegistryService constructor.
 *
 * @param nfRegistryApi         The api service.
 * @param nfStorage             A wrapper for the browser's local storage.
 * @param tdDataTableService    The covalent data table service module.
 * @param router                The angular router module.
 * @param fdsDialogService      The FDS dialog service.
 * @param fdsSnackBarService    The FDS snack bar service module.
 * @constructor
 */
function NfRegistryService(nfRegistryApi, nfStorage, tdDataTableService, router, fdsDialogService, fdsSnackBarService) {
    this.router = router;
    this.api = nfRegistryApi;
    this.nfStorage = nfStorage;
    this.dialogService = fdsDialogService;
    this.snackBarService = fdsSnackBarService;
    this.registry = {
        name: "Nifi Registry"
    };
    this.bucket = {};
    this.buckets = [];
    this.droplet = {};
    this.droplets = [];
    this.currentUser = {};
    this.user = {};
    this.group = {};
    this.users = [];
    this.groups = [];
    this.alerts = [];
    this.explorerViewType = '';
    this.perspective = '';
    this.breadCrumbState = 'out';
    this.dataTableService = tdDataTableService;

    this.filteredDroplets = [];
    this.dropletActions = [{
        name: 'Delete',
        icon: 'fa fa-trash',
        tooltip: 'Delete'
    }];
    this.dropletColumns = [
        {
            name: 'name',
            label: 'Name',
            sortable: true
        },
        {
            name: 'updated',
            label: 'Updated',
            sortable: true
        }
    ];
    this.autoCompleteDroplets = [];
    this.dropletsSearchTerms = [];

    this.filteredBuckets = [];
    this.bucketColumns = [
        {
            name: 'name',
            label: 'Bucket Name',
            sortable: true,
            tooltip: 'Sort Buckets by name.'
        }
    ];
    this.allBucketsSelected = false;
    this.autoCompleteBuckets = [];
    this.selectedBuckets = [];
    this.bucketsSearchTerms = [];
    this.isMultiBucketActionsDisabled = true;

    this.filteredUsers = [];
    this.filteredUserGroups = [];
    this.userColumns = [
        {
            name: 'identity',
            label: 'Display Name',
            sortable: true,
            tooltip: 'User name.',
            width: 100
        }
    ];
    this.allUsersAndGroupsSelected = false;
    this.autoCompleteUsersAndGroups = [];
    this.selectedUsers = [];
    this.usersSearchTerms = [];
    this.isMultiUserActionsDisabled = true;
    this.isMultiUserGroupActionsDisabled = false;
};

NfRegistryService.prototype = {
    constructor: NfRegistryService,

    /**
     * Set the `breadCrumbState` for the breadcrumb animations.
     *
     * @param {string} state    The state. Valid values are 'in' or 'out'.
     */
    setBreadcrumbState: function (state) {
        this.breadCrumbState = state;
    },

    /**
     * Gets the droplet grid-list explorer component's active sorting column display label.
     *
     * @returns {string}
     */
    getSortByLabel: function () {
        var sortByColumn;
        var arrayLength = this.dropletColumns.length;
        for (var i = 0; i < arrayLength; i++) {
            if (this.dropletColumns[i].active === true) {
                sortByColumn = this.dropletColumns[i];
                break;
            }
        }

        if (sortByColumn) {
            var label = '';
            switch (sortByColumn.label) {
                case 'Updated':
                    label = (sortByColumn.sortOrder === 'ASC') ? 'Newest (update)' : 'Oldest (update)';
                    break;
                case 'Name':
                    label = (sortByColumn.sortOrder === 'ASC') ? 'Name (a - z)' : 'Name (z - a)';
                    break;
            }
            return label;
        }
    },

    /**
     * Generates the droplet grid-list explorer component's sorting menu options.
     *
     * @param col           One of the available `dropletColumns`.
     * @returns {string}
     */
    generateSortMenuLabels: function (col) {
        var label = '';
        switch (col.label) {
            case 'Updated':
                label = (col.sortOrder !== 'ASC') ? 'Newest (update)' : 'Oldest (update)';
                break;
            case 'Name':
                label = (col.sortOrder !== 'ASC') ? 'Name (a - z)' : 'Name (z - a)';
                break;
        }
        return label;
    },

    /**
     * Execute the given droplet action.
     *
     * @param action        The action object.
     * @param droplet       The droplet object the `action` will act upon.
     */
    executeDropletAction: function (action, droplet) {
        var self = this;
        if (action.name.toLowerCase() === 'delete') {
            this.dialogService.openConfirm({
                title: 'Delete ' + droplet.type.toLowerCase(),
                message: 'All versions of this ' + droplet.type.toLowerCase() + ' will be deleted.',
                cancelButton: 'Cancel',
                acceptButton: 'Delete',
                acceptButtonColor: 'fds-warn'
            }).afterClosed().subscribe(
                function (accept) {
                    if (accept) {
                        self.api.deleteDroplet(droplet.link.href).subscribe(function (response) {
                            self.droplets = self.droplets.filter(function (d) {
                                return (d.identifier !== droplet.identifier) ? true : false
                            });
                            var snackBarRef = self.snackBarService.openCoaster({
                                title: 'Success',
                                message: 'All versions of this ' + droplet.type.toLowerCase() + ' have been deleted.',
                                verticalPosition: 'bottom',
                                horizontalPosition: 'right',
                                icon: 'fa fa-check-circle-o',
                                color: '#1EB475',
                                duration: 3000
                            });
                            self.droplet = {};
                            self.filterDroplets();
                        });
                    }
                });
        }
    },

    /**
     * Retrieves the snapshot metadata for the given droplet.
     *
     * @param droplet       The droplet.
     */
    getDropletSnapshotMetadata: function (droplet) {
        this.api.getDropletSnapshotMetadata(droplet.link.href, true).subscribe(function (snapshotMetadata) {
            droplet.snapshotMetadata = snapshotMetadata;
        })
    },

    /**
     * Sort `filteredDroplets` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortDroplets: function (column) {
        if (column.sortable === true) {
            // toggle column sort order
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterDroplets(column.name, sortOrder);
            //only one column can be actively sorted so we reset all to inactive
            this.dropletColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
            this.activeDropletColumn = column;
        }
    },

    /**
     * Filter droplets.
     *
     * @param {string} [sortBy]       The column name to sort `dropletColumns` by.
     * @param {string} [sortOrder]    The order. Either 'ASC' or 'DES'
     */
    filterDroplets: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in `dropletColumns`
        if (sortBy === undefined) {
            var arrayLength = this.dropletColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.dropletColumns[i].sortable === true) {
                    sortBy = this.dropletColumns[i].name;
                    //only one column can be actively sorted so we reset all to inactive
                    this.dropletColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.dropletColumns[i].active = true;
                    this.dropletColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newData;

        // if we are viewing a single droplet
        if (this.droplet.identifier) {
            newData = [this.droplet];
        } else {
            newData = this.droplets;
        }

        for (var i = 0; i < this.dropletsSearchTerms.length; i++) {
            newData = this.filterData(newData, this.dropletsSearchTerms[i], true, sortBy);
        }

        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.filteredDroplets = newData;
        this.getAutoCompleteDroplets();
    },

    /**
     * Generates the `autoCompleteDroplets` options for the droplet filter.
     */
    getAutoCompleteDroplets: function () {
        var self = this;
        this.autoCompleteDroplets = [];
        this.dropletColumns.forEach(function (c) {
            return self.filteredDroplets.forEach(function (r) {
                return (r[c.name.toLowerCase()]) ? self.autoCompleteDroplets.push(r[c.name.toLowerCase()].toString()) : '';
            })
        });
    },

    /**
     * Execute the given bucket action.
     *
     * @param action        The action object.
     * @param bucket        The bucket object the `action` will act upon.
     */
    executeBucketAction: function (action, bucket) {
        var self = this;
        switch (action.name.toLowerCase()) {
            case 'delete':
                this.dialogService.openConfirm({
                    title: 'Delete Bucket',
                    message: 'All items stored in this bucket will be deleted as well.',
                    cancelButton: 'Cancel',
                    acceptButton: 'Delete',
                    acceptButtonColor: 'fds-warn'
                }).afterClosed().subscribe(
                    function (accept) {
                        if (accept) {
                            self.api.deleteBucket(bucket.identifier).subscribe(function (response) {
                                self.buckets = self.buckets.filter(function (b) {
                                    return b.identifier !== bucket.identifier;
                                });
                                var snackBarRef = self.snackBarService.openCoaster({
                                    title: 'Success',
                                    message: 'All versions of all items in this bucket, as well as the bucket, have been deleted.',
                                    verticalPosition: 'bottom',
                                    horizontalPosition: 'right',
                                    icon: 'fa fa-check-circle-o',
                                    color: '#1EB475',
                                    duration: 3000
                                });
                                self.bucket = {};
                                self.filterBuckets();
                            });
                        }
                    });
                break;
            case 'permissions':
                this.router.navigateByUrl('/nifi-registry/administration/workflow(' + action.type + ':bucket/' + action.name + '/' + bucket.identifier + ')');
                break;
            default:
                break;
        }
    },

    /**
     * Filter buckets and sets the `isMultiBucketActionsDisabled` property accordingly.
     *
     * @param {string} sortBy       The column name to sort `bucketColumns` by.
     * @param {string} sortOrder    The order. Either 'ASC' or 'DES'
     */
    filterBuckets: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }

        // if `sortBy` is `undefined` then find the first sortable column in this.bucketColumns
        if (sortBy === undefined) {
            var arrayLength = this.bucketColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.bucketColumns[i].sortable === true) {
                    sortBy = this.bucketColumns[i].name;
                    //only one column can be actively sorted so we reset all to inactive
                    this.bucketColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.bucketColumns[i].active = true;
                    this.bucketColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newData = this.buckets;

        for (var i = 0; i < this.bucketsSearchTerms.length; i++) {
            newData = this.filterData(newData, this.bucketsSearchTerms[i], true, sortBy);
        }

        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.filteredBuckets = newData;

        var selected = 0;
        this.filteredBuckets.forEach(function (filteredBucket) {
            if (filteredBucket.checked) {
                selected++;
            }
        });

        this.isMultiBucketActionsDisabled = (selected > 0) ? false : true;

        this.getAutoCompleteBuckets();
    },

    /**
     * Generates the `autoCompleteBuckets` options for the bucket filter.
     */
    getAutoCompleteBuckets: function () {
        var self = this;
        this.autoCompleteBuckets = [];
        this.bucketColumns.forEach(function (c) {
            return self.filteredBuckets.forEach(function (r) {
                return (r[c.name.toLowerCase()]) ? self.autoCompleteBuckets.push(r[c.name.toLowerCase()].toString()) : '';
            });
        });
    },

    /**
     * Sort `filteredBuckets` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortBuckets: function (column) {
        if (column.sortable === true) {
            // toggle column sort order
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterBuckets(column.name, sortOrder);
            //only one column can be actively sorted so we reset all to inactive
            this.bucketColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    /**
     * Returns true if each bucket in the `filteredBuckets` are selected and sets the `isMultiBucketActionsDisabled`
     * property accordingly.
     *
     * @returns {boolean}
     */
    allFilteredBucketsSelected: function () {
        var selected = 0;
        var allSelected = true;
        this.filteredBuckets.forEach(function (c) {
            if (c.checked) {
                selected++;
            }
            if (c.checked === undefined || c.checked === false) {
                allSelected = false;
            }
        });

        this.isMultiBucketActionsDisabled = (selected > 0) ? false : true;
        return allSelected;
    },

    /**
     * Checks each of the `filteredBuckets`'s `checked` property state and sets the `allBucketsSelected`
     * property accordingly.
     */
    determineAllBucketsSelectedState: function () {
        if (this.allFilteredBucketsSelected()) {
            this.allBucketsSelected = true;
        } else {
            this.allBucketsSelected = false;
        }
    },

    /**
     * Checks the `allBucketsSelected` property state and either selects
     * or deselects all filtered buckets.
     */
    toggleBucketsSelectAll: function () {
        if (this.allBucketsSelected) {
            this.selectAllBuckets();
        } else {
            this.deselectAllBuckets();
        }
    },

    /**
     * Sets the `checked` property of each filtered bucket to true and sets
     * the `isMultiBucketActionsDisabled` property accordingly.
     */
    selectAllBuckets: function () {
        this.filteredBuckets.forEach(function (c) {
            c.checked = true;
        });
        this.isMultiBucketActionsDisabled = false;
    },

    /**
     * Sets the `checked` property of each filtered bucket to false and sets
     * the `isMultiBucketActionsDisabled` property accordingly.
     */
    deselectAllBuckets: function () {
        this.filteredBuckets.forEach(function (c) {
            c.checked = false;
        });
        this.isMultiBucketActionsDisabled = true;
    },

    /**
     * Removes a `searchTerm` from the `bucketsSearchTerms` and filters the `buckets`.
     *
     * @param {string} searchTerm The search term to remove.
     */
    bucketsSearchRemove: function (searchTerm) {
        //only remove the first occurrence of the search term
        var index = this.bucketsSearchTerms.indexOf(searchTerm);
        if (index !== -1) {
            this.bucketsSearchTerms.splice(index, 1);
        }
        this.filterBuckets();
        this.determineAllBucketsSelectedState();
    },

    /**
     * Adds a `searchTerm` from the `bucketsSearchTerms` and filters the `buckets`.
     *
     * @param {string} searchTerm The search term to add.
     */
    bucketsSearchAdd: function (searchTerm) {
        this.bucketsSearchTerms.push(searchTerm);
        this.filterBuckets();
        this.determineAllBucketsSelectedState();
    },

    /**
     * Deletes all versions of all flows of each selected bucket
     */
    deleteSelectedBuckets: function () {
        var self = this;
        this.dialogService.openConfirm({
            title: 'Delete Buckets',
            message: 'All versions of all flows of each selected bucket will be deleted.',
            cancelButton: 'Cancel',
            acceptButton: 'Delete',
            acceptButtonColor: 'fds-warn'
        }).afterClosed().subscribe(
            function (accept) {
                if (accept) {
                    self.filteredBuckets.forEach(function (filteredBucket) {
                        if (filteredBucket.checked) {
                            self.api.deleteBucket(filteredBucket.identifier).subscribe(function (response) {
                                self.buckets = self.buckets.filter(function (bucket) {
                                    return bucket.identifier !== filteredBucket.identifier;
                                });
                                var snackBarRef = self.snackBarService.openCoaster({
                                    title: 'Success',
                                    message: 'All versions of all items in ' + filteredBucket.name + ' have been deleted.',
                                    verticalPosition: 'bottom',
                                    horizontalPosition: 'right',
                                    icon: 'fa fa-check-circle-o',
                                    color: '#1EB475',
                                    duration: 3000
                                });
                                self.filterBuckets();
                            });
                        }
                    });
                    self.determineAllBucketsSelectedState();
                }
            });
    },

    /**
     * Sort `users` and `groups` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortUsersAndGroups: function (sortEvent, column) {
        if (column.sortable) {
            var sortBy = column.name;
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterUsersAndGroups(sortBy, sortOrder);

            //only one column can be actively sorted so we reset all to inactive
            this.userColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    /**
     * Loads the current user and updates the current user locally.
     *
     * @returns xhr
     */
    loadCurrentUser: function () {
        var self = this;
        // get the current user
        return rxjs.Observable.of(this.api.loadCurrentUser().subscribe(function (currentUser) {
                // if the user is logged, we want to determine if they were logged in using a certificate
                if (currentUser.status !== "UNKNOWN") {
                    // render the users name
                    self.currentUser = currentUser;

                    // render the logout button if there is a token locally
                    if (self.nfStorage.getItem('jwt') !== null) {
                        self.currentUser.canLogout = true;
                    }
                } else {
                    // set the anonymous user label
                    self.nfRegistryService.currentUser.identity = 'Anonymous';
                }
        }));
    },

    /**
     * Adds a `searchTerm` to the `usersSearchTerms` and filters the `users` amd `groups`.
     *
     * @param {string} searchTerm   The search term to add.
     */
    usersSearchRemove: function (searchTerm) {
        //only remove the first occurrence of the search term
        var index = this.usersSearchTerms.indexOf(searchTerm);
        if (index !== -1) {
            this.usersSearchTerms.splice(index, 1);
        }
        this.filterUsersAndGroups();
        this.determineAllUsersAndGroupsSelectedState();
    },

    /**
     * Removes a `searchTerm` from the `usersSearchTerms` and filters the `users` amd `groups`.
     *
     * @param {string} searchTerm   The search term to remove.
     */
    usersSearchAdd: function (searchTerm) {
        this.usersSearchTerms.push(searchTerm);
        this.filterUsersAndGroups();
        this.determineAllUsersAndGroupsSelectedState();
    },

    /**
     * Filter users and groups.
     *
     * @param {string} [sortBy]       The column name to sort `userGroupsColumns` by.
     * @param {string} [sortOrder]    The order. Either 'ASC' or 'DES'
     */
    filterUsersAndGroups: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in `dropletColumns`
        if (sortBy === undefined) {
            var arrayLength = this.userColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.userColumns[i].sortable === true) {
                    sortBy = this.userColumns[i].name;
                    //only one column can be actively sorted so we reset all to inactive
                    this.userColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.userColumns[i].active = true;
                    this.userColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newUsersData = this.users;
        var newUserGroupsData = this.groups;

        for (var i = 0; i < this.usersSearchTerms.length; i++) {
            newUsersData = this.filterData(newUsersData, this.usersSearchTerms[i], true);
        }

        newUsersData = this.dataTableService.sortData(newUsersData, sortBy, sortOrder);
        this.filteredUsers = newUsersData;

        for (var i = 0; i < this.usersSearchTerms.length; i++) {
            newUserGroupsData = this.filterData(newUserGroupsData, this.usersSearchTerms[i], true);
        }

        newUserGroupsData = this.dataTableService.sortData(newUserGroupsData, sortBy, sortOrder);
        this.filteredUserGroups = newUserGroupsData;

        this.getAutoCompleteUserAndGroups();
    },

    /**
     * Checks each of the `filteredUsers` and each of the `filteredUserGroups` `checked` property state and sets
     * the `allUsersAndGroupsSelected`, `isMultiUserGroupActionsDisabled`, and the `isMultiUserActionsDisabled`
     * properties accordingly.
     */
    determineAllUsersAndGroupsSelectedState: function () {
        var selected = 0;
        var allSelected = true;
        this.isMultiUserGroupActionsDisabled = false;
        this.filteredUserGroups.forEach(function (c) {
            if (c.checked) {
                selected++;
            }
            if (c.checked === undefined || c.checked === false) {
                allSelected = false;
            }
        });

        if (selected > 0) {
            this.isMultiUserGroupActionsDisabled = true;
        }

        this.filteredUsers.forEach(function (c) {
            if (c.checked) {
                selected++;
            }
            if (c.checked === undefined || c.checked === false) {
                allSelected = false;
            }
        });
        this.isMultiUserActionsDisabled = (selected > 0) ? false : true;
        this.allUsersAndGroupsSelected = allSelected;
    },

    /**
     * Checks the `allUsersAndGroupsSelected` property state and either selects
     * or deselects all `filteredUsers` and each `filteredUserGroups`.
     */
    toggleUsersSelectAll: function () {
        if (this.allUsersAndGroupsSelected) {
            this.selectAllUsersAndGroups();
        } else {
            this.deselectAllUsersAndGroups();
        }
    },

    /**
     * Sets the `checked` property of each `filteredUsers` and each `filteredUserGroups` to true and sets
     * the `allUsersAndGroupsSelected`, `isMultiUserGroupActionsDisabled`, and the `isMultiUserActionsDisabled`
     * properties accordingly.
     */
    selectAllUsersAndGroups: function () {
        this.filteredUsers.forEach(function (c) {
            c.checked = true;
        });
        this.filteredUserGroups.forEach(function (c) {
            c.checked = true;
        });
        this.isMultiUserGroupActionsDisabled = (this.filteredUserGroups.length > 0) ? true : false;
        this.allUsersAndGroupsSelected = true;
        this.isMultiUserActionsDisabled = false;
    },

    /**
     * Sets the `checked` property of each `filteredUsers` and each `filteredUserGroups` to false and sets
     * the `allUsersAndGroupsSelected`, `isMultiUserGroupActionsDisabled`, and the `isMultiUserActionsDisabled`
     * properties accordingly.
     */
    deselectAllUsersAndGroups: function () {
        this.filteredUsers.forEach(function (c) {
            c.checked = false;
        });
        this.filteredUserGroups.forEach(function (c) {
            c.checked = false;
        });
        this.allUsersAndGroupsSelected = false;
        this.isMultiUserGroupActionsDisabled = false;
        this.isMultiUserActionsDisabled = true;
    },

    /**
     * Generates the `autoCompleteUsersAndGroups` options for the users and groups data table filter.
     */
    getAutoCompleteUserAndGroups: function () {
        var self = this;
        this.autoCompleteUsersAndGroups = [];
        this.userColumns.forEach(function (c) {
            var usersAndGroups = self.filteredUsers.concat(self.filteredUserGroups);
            usersAndGroups.forEach(function (r) {
                (r[c.name.toLowerCase()]) ? self.autoCompleteUsersAndGroups.push(r[c.name.toLowerCase()].toString()) : '';
            });
        });
    },

    /**
     * Execute the given user action.
     *
     * @param action        The action object.
     * @param user          The user object the `action` will act upon.
     */
    executeUserAction: function (action, user) {
        var self = this;
        switch (action.name.toLowerCase()) {
            case 'delete':
                this.dialogService.openConfirm({
                    title: 'Delete User',
                    message: 'This user will lose all access to the registry.',
                    cancelButton: 'Cancel',
                    acceptButton: 'Delete',
                    acceptButtonColor: 'fds-warn'
                }).afterClosed().subscribe(
                    function (accept) {
                        if (accept) {
                            self.api.deleteUser(user.identifier).subscribe(function (response) {
                                self.users = self.users.filter(function (u) {
                                    return u.identifier !== user.identifier;
                                });
                                var snackBarRef = self.snackBarService.openCoaster({
                                    title: 'Success',
                                    message: 'User: ' + user.identity + ' has been deleted.',
                                    verticalPosition: 'bottom',
                                    horizontalPosition: 'right',
                                    icon: 'fa fa-check-circle-o',
                                    color: '#1EB475',
                                    duration: 3000
                                });
                                self.filterUsersAndGroups();
                            });
                        }
                    });
                break;
            case 'permissions':
                this.router.navigateByUrl('/nifi-registry/administration/users(' + action.type + ':user/' + action.name + '/' + user.identifier + ')');
                break;
        }
    },

    /**
     * Execute the given group action.
     *
     * @param action        The action object.
     * @param group          The group object the `action` will act upon.
     */
    executeGroupAction: function (action, group) {
        var self = this;
        this.group = group;
        switch (action.name.toLowerCase()) {
            case 'delete':
                this.dialogService.openConfirm({
                    title: 'Delete Group',
                    message: 'All policies granted to this group will be deleted as well.',
                    cancelButton: 'Cancel',
                    acceptButton: 'Delete',
                    acceptButtonColor: 'fds-warn'
                }).afterClosed().subscribe(
                    function (accept) {
                        if (accept) {
                            self.api.deleteUserGroup(group.identifier).subscribe(function (response) {
                                self.groups = self.groups.filter(function (u) {
                                    return u.identifier !== group.identifier;
                                });
                                var snackBarRef = self.snackBarService.openCoaster({
                                    title: 'Success',
                                    message: 'Group: ' + group.identity + ' has been deleted.',
                                    verticalPosition: 'bottom',
                                    horizontalPosition: 'right',
                                    icon: 'fa fa-check-circle-o',
                                    color: '#1EB475',
                                    duration: 3000
                                });
                                self.filterUsersAndGroups();
                            });
                        }
                    });
                break;
            case 'permissions':
                this.router.navigateByUrl('/nifi-registry/administration/users(' + action.type + ':group/' + action.name + '/' + group.identifier + ')');
                break;
        }
    },

    /**
     * Deletes all selected `filteredUserGroups` and `filteredUsers` and sets the `allUsersAndGroupsSelected`
     * property accordingly.
     */
    deleteSelectedUsersAndGroups: function () {
        var self = this;
        this.dialogService.openConfirm({
            title: 'Delete Users/Groups',
            message: 'The selected users will lose all access to the registry and all policies granted to the selected groups will be deleted.',
            cancelButton: 'Cancel',
            acceptButton: 'Delete',
            acceptButtonColor: 'fds-warn'
        }).afterClosed().subscribe(
            function (accept) {
                if (accept) {
                    self.filteredUserGroups.forEach(function (filteredUserGroup) {
                        if (filteredUserGroup.checked) {
                            self.api.deleteUserGroup(filteredUserGroup.identifier).subscribe(function (response) {
                                self.groups = self.groups.filter(function (u) {
                                    return u.identifier !== filteredUserGroup.identifier;
                                });
                                var snackBarRef = self.snackBarService.openCoaster({
                                    title: 'Success',
                                    message: 'User group: ' + filteredUserGroup.identity + ' has been deleted.',
                                    verticalPosition: 'bottom',
                                    horizontalPosition: 'right',
                                    icon: 'fa fa-check-circle-o',
                                    color: '#1EB475',
                                    duration: 3000
                                });
                                self.filterUsersAndGroups();
                            });
                        }
                    });
                    self.filteredUsers.forEach(function (filteredUser) {
                        if (filteredUser.checked) {
                            self.api.deleteUser(filteredUser.identifier).subscribe(function (response) {
                                self.users = self.users.filter(function (u) {
                                    return u.identifier !== filteredUser.identifier;
                                });
                                var snackBarRef = self.snackBarService.openCoaster({
                                    title: 'Success',
                                    message: 'User: ' + filteredUser.identity + ' has been deleted.',
                                    verticalPosition: 'bottom',
                                    horizontalPosition: 'right',
                                    icon: 'fa fa-check-circle-o',
                                    color: '#1EB475',
                                    duration: 3000
                                });
                                self.filterUsersAndGroups();
                            });
                        }
                    });
                    self.determineAllUsersAndGroupsSelectedState();
                }
            });
    },

    /**
     * Utility method that performs the custom search capability for data tables.
     *
     * @param data          The data to search.
     * @param searchTerm    The term we are looking for.
     * @param ignoreCase    Ignore case.
     * @returns {*}
     */
    filterData: function(data, searchTerm, ignoreCase) {
        var field = '';
        if (searchTerm.indexOf(":") > -1) {
            field = searchTerm.split(':')[0].trim();
            searchTerm = searchTerm.split(':')[1].trim();
        }
        var filter = searchTerm ? (ignoreCase ? searchTerm.toLowerCase() : searchTerm) : '';

        if (filter) {
            data = data.filter(function (item) {
                var res = Object.keys(item).find(function (key) {
                    if (key !== field && field !== '') {
                        return false;
                    }
                    var preItemValue = ('' + item[key]);
                    var itemValue = ignoreCase ? preItemValue.toLowerCase() : preItemValue;
                    return itemValue.indexOf(filter) > -1;
                });
                return !(typeof res === 'undefined');
            });
        }
        return data;
    }

    //</editor-fold>
};

NfRegistryService.parameters = [
    NfRegistryApi,
    NfStorage,
    covalentCore.TdDataTableService,
    ngRouter.Router,
    fdsDialogsModule.FdsDialogService,
    fdsSnackBarsModule.FdsSnackBarService
];

module.exports = NfRegistryService;
