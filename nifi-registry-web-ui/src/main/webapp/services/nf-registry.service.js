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
require('rxjs/add/operator/catch');
require('rxjs/add/operator/map');

function filterData(data, searchTerm, ignoreCase) {
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
};

/**
 * NfRegistryService constructor.
 *
 * @param TdDataTableService    The covalent data table service module.
 * @param NfRegistryApi         The registry API module.
 * @param Router                The angular router module.
 * @param FdsDialogService      The FDS dialog service.
 * @param FdsSnackBarService    The FDS snack bar service module.
 * @constructor
 */
function NfRegistryService(TdDataTableService, NfRegistryApi, Router, FdsDialogService, FdsSnackBarService) {
    this.router = Router;
    this.dialogService = FdsDialogService;
    this.snackBarService = FdsSnackBarService;
    this.registry = {
        name: "Nifi Registry"
    };
    this.api = NfRegistryApi;
    this.bucket = {};
    this.buckets = [];
    this.droplet = {};
    this.droplets = [];
    this.user = {};
    this.users = [];
    this.alerts = [];
    this.explorerViewType = '';
    this.perspective = '';
    this.breadCrumbState = 'out';
    this.dataTableService = TdDataTableService;

    this.filteredDroplets = [];
    this.dropletActions = [{
        'name': 'Delete',
        'icon': 'fa fa-trash',
        'tooltip': 'Delete'
    }];
    this.dropletColumns = [
        {name: 'name', label: 'Name', sortable: true},
        {name: 'updated', label: 'Updated', sortable: true}
    ];
    this.autoCompleteDroplets = [];
    this.dropletsSearchTerms = [];

    this.filteredBuckets = [];
    this.bucketColumns = [
        {name: 'name', label: 'Bucket Name', sortable: true, tooltip: 'Sort Buckets by name.'}
    ];
    this.allBucketsSelected = false;
    this.autoCompleteBuckets = [];
    this.selectedBuckets = [];
    this.bucketsSearchTerms = [];
    this.disableMultiBucketActions = true;

    this.filteredUsers = [];
    this.userColumns = [
        {name: 'status', label: 'Status', sortable: true, tooltip: 'User Status.', width: 18},
        {name: 'name', label: 'Name', sortable: true, tooltip: 'User name.', width: 30},
        {name: 'provider', label: 'Provider', sortable: true, tooltip: 'Authentication provider.', width: 30}
    ];
    this.allUsersSelected = false;
    this.autoCompleteUsers = [];
    this.selectedUsers = [];
    this.usersSearchTerms = [];
};

NfRegistryService.prototype = {
    constructor: NfRegistryService,

    /**
     * Set the state for the breadcrumb animations.
     *
     * @param {string} state The state. Valid values are 'in' or 'out'.
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
     * @param col   One of the available `dropletColumns`.
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
                            if (response.link === null) {
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
                            } else {
                                var snackBarRef = self.snackBarService.openCoaster({
                                    title: 'Error',
                                    message: 'Error deleting ' + droplet.name + '.',
                                    verticalPosition: 'bottom',
                                    horizontalPosition: 'right',
                                    icon: 'fa fa-times-circle-o',
                                    color: '#EF6162'
                                });
                            }
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
     * Sort `droplets` by `column`.
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
                    this.activeDropletColumn = this.dropletColumns[i];
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
            newData = filterData(newData, this.dropletsSearchTerms[i], true, this.activeDropletColumn.name);
        }

        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.filteredDroplets = newData;
        this.getAutoCompleteDroplets();
    },

    /**
     * Generates the auto complete options for the droplet filter.
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
                    message: 'All versions of all items will be deleted.',
                    cancelButton: 'Cancel',
                    acceptButton: 'Delete',
                    acceptButtonColor: 'fds-warn'
                }).afterClosed().subscribe(
                    function (accept) {
                        if (accept) {
                            self.api.deleteBucket(bucket.identifier).subscribe(function (response) {
                                if (response.link === null) {
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
                                } else {
                                    var snackBarRef = self.snackBarService.openCoaster({
                                        title: 'Error',
                                        message: 'Error deleting ' + bucket.name + '.',
                                        verticalPosition: 'bottom',
                                        horizontalPosition: 'right',
                                        icon: 'fa fa-times-circle-o',
                                        color: '#EF6162'
                                    });
                                }
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
     * Filter buckets.
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
                    this.activeBucketColumn = this.bucketColumns[i];
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
            newData = filterData(newData, this.bucketsSearchTerms[i], true, this.activeBucketColumn.name);
        }

        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.filteredBuckets = newData;

        var selected = 0;
        this.filteredBuckets.forEach(function (filteredBucket) {
            if (filteredBucket.checked) {
                selected++;
            }
        });

        this.disableMultiBucketActions = (selected > 0) ? false : true;

        this.getAutoCompleteBuckets();
    },

    /**
     * Generates the auto complete options for the bucket filter.
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
     * Sort `buckets` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortBuckets: function (column) {
        if (column.sortable === true) {
            // toggle column sort order
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterBuckets(column.name, sortOrder);
            this.activeBucketsColumn = column;
            //only one column can be actively sorted so we reset all to inactive
            this.bucketColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    /**
     * Returns true if each bucket in the `filteredBuckets` are selected.
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

        this.disableMultiBucketActions = (selected > 0) ? false : true;
        return allSelected;
    },

    /**
     * Checks all filtered buckets checked state and sets the `allBucketsSelected`
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
     * Sets the `checked` property of each filtered bucket to true.
     */
    selectAllBuckets: function () {
        this.filteredBuckets.forEach(function (c) {
            c.checked = true;
        });
        this.disableMultiBucketActions = false;
    },

    /**
     * Sets the `checked` property of each filtered bucket to false.
     */
    deselectAllBuckets: function () {
        this.filteredBuckets.forEach(function (c) {
            c.checked = false;
        });
        this.disableMultiBucketActions = true;
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
    },

    /**
     * Adds a `searchTerm` from the `bucketsSearchTerms` and filters the `buckets`.
     *
     * @param {string} searchTerm The search term to add.
     */
    bucketsSearchAdd: function (searchTerm) {
        this.bucketsSearchTerms.push(searchTerm);
        this.filterBuckets();
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
                                if (response.link === null) {
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
                                    self.allBucketsSelected = false;
                                } else {
                                    var snackBarRef = self.snackBarService.openCoaster({
                                        title: 'Error',
                                        message: 'Error deleting ' + filteredBucket.name + '.',
                                        verticalPosition: 'bottom',
                                        horizontalPosition: 'right',
                                        icon: 'fa fa-times-circle-o',
                                        color: '#EF6162'
                                    });
                                }
                            });
                        }
                    });
                }
            });
    },

    sortUsers: function (sortEvent, column) {
        if (column.sortable) {
            var sortBy = column.name;
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterUsers(sortBy, sortOrder);

            //only one column can be actively sorted so we reset all to inactive
            this.userColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    usersSearchRemove: function (searchTerm) {
        //only remove the first occurrence of the search term
        var index = this.usersSearchTerms.indexOf(searchTerm);
        if (index !== -1) {
            this.usersSearchTerms.splice(index, 1);
        }
        this.filterUsers();
    },

    usersSearchAdd: function (searchTerm) {
        this.usersSearchTerms.push(searchTerm);
        this.filterUsers();
    },

    filterUsers: function (sortBy, sortOrder) {
        if (this.allUsersSelected) {
            this.toggleUsersSelectAll();
        }
        this.deselectAllUsers();
        var newData = this.users;

        for (var i = 0; i < this.usersSearchTerms.length; i++) {
            newData = filterData(newData, this.usersSearchTerms[i], true);
        }
        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        newData = this.dataTableService.pageData(newData, this.usersFromRow, this.usersCurrentPage * this.usersPageSize);
        this.filteredUsers = newData;
        this.getAutoCompleteUsers();
    },

    toggleUserSelect: function (row) {
        if (this.allFilteredUsersSelected()) {
            this.allUsersSelected = true;
        } else {
            this.allUsersSelected = false;
        }
    },

    toggleUsersSelectAll: function () {
        if (this.allUsersSelected) {
            this.selectAllUsers();
        } else {
            this.deselectAllUsers();
        }
    },

    selectAllUsers: function () {
        this.filteredUsers.forEach(function (c) {
            c.checked = true;
        });
    },

    deselectAllUsers: function () {
        this.filteredUsers.forEach(function (c) {
            c.checked = false;
        });
    },
    allFilteredUsersSelected: function () {
        var allFilteredUsersSelected = true;
        this.filteredUsers.forEach(function (c) {
            if (c.checked === undefined || c.checked === false) {
                allFilteredUsersSelected = false;
            }
        });

        return allFilteredUsersSelected;
    },

    getAutoCompleteUsers: function () {
        var self = this;
        this.autoCompleteUsers = [];
        this.userColumns.forEach(function (c) {
            self.filteredUsers.forEach(function (r) {
                (r[c.name.toLowerCase()]) ? self.autoCompleteUsers.push(r[c.name.toLowerCase()].toString()) : '';
            });
        });
    },

    //</editor-fold>
};

NfRegistryService.parameters = [
    covalentCore.TdDataTableService,
    NfRegistryApi,
    ngRouter.Router,
    fdsDialogsModule.FdsDialogService,
    fdsSnackBarsModule.FdsSnackBarService
];

module.exports = NfRegistryService;
