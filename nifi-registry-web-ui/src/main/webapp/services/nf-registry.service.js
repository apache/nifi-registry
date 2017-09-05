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
                if (field.indexOf(".") > -1) {
                    var objArray = field.split(".");
                    var obj = item;
                    var arrayLength = objArray.length;
                    for (var i = 0; i < arrayLength; i++) {
                        try {
                            obj = obj[objArray[i]];
                        } catch (e) {
                            return false;
                        }
                    }
                    var preItemValue = ('' + obj);
                    var itemValue = ignoreCase ? preItemValue.toLowerCase() : preItemValue;
                    return itemValue.indexOf(filter) > -1;
                } else {
                    if (key !== field && field !== '') {
                        return false;
                    }
                    var preItemValue = ('' + item[key]);
                    var itemValue = ignoreCase ? preItemValue.toLowerCase() : preItemValue;
                    return itemValue.indexOf(filter) > -1;
                }
            });
            return !(typeof res === 'undefined');
        });
    }
    return data;
};

function NfRegistryService(TdDataTableService) {
    this.registries = [];
    this.registry = {};
    this.bucket = {};
    this.buckets = [];
    this.droplet = {};
    this.droplets = [];
    this.certifications = [];
    this.user = {};
    this.users = [];
    this.alerts = [];
    this.explorerViewType = '';
    this.perspective = '';
    this.breadCrumbState = 'out';
    this.dataTableService = TdDataTableService;

    this.filteredDroplets = [];

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
    this.bucketsSearchTerms = [];

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
    this.usersFromRow = 1;
    this.usersCurrentPage = 1;
    this.usersPageSize = 5;
    this.usersPageCount = 0;

    this.filteredCertifications = [];

    this.certificationColumns = [
        {name: 'name', label: 'Label Name', sortable: true, tooltip: 'Sort Certifications by name.', width: 40},
        {name: 'usage', label: 'Usage', sortable: true, tooltip: 'Sort Certifications by usage.', width: 30},
        {name: 'badge', label: 'Badge Design', sortable: false, tooltip: 'Certification badge.', width: 30}
    ];

    this.autoCompleteCertifications = [];
    this.certificationsSearchTerms = [];
};

NfRegistryService.prototype = {
    constructor: NfRegistryService,

    deleteDroplet: function (id) {
        //TODO: REST call to API to delete droplet by id.
    },

    deleteBucket: function (id) {
        //TODO: REST call to API to delete bucket by id.
    },

    deleteUser: function (id) {
        //TODO: REST call to API to delete user by id.
    },

    suspendUser: function (id) {
        //TODO: REST call to API to suspend user by id.
    },

    getRegistries: function () {
        //TODO: leverage $http service to make call to nifi registry api. For now just return mock data...
        var self = this;
        var date = new Date();
        return new Promise(
            function (resolve) {
                setTimeout(
                    function () {
                        resolve(self.registries = [{
                            id: '23f6cc59-0156-1000-06b4-2b0810089090',
                            name: "Nifi Registry",
                            users: [{
                                id: '23f6cc59-0156-1000-06b4-2b0810089090',
                                name: 'Scotty 2 Hotty',
                                status: 'authorized',
                                provider: 'Friendly LDAP Provider',
                                type: 'user',
                                activities: [{
                                    id: '25fd6vv87-3249-0001-05g6-4d4767890765',
                                    description: 'Saved something...',
                                    created: date.setDate(date.getDate() - 1),
                                    updated: new Date()
                                }],
                                actions: [{
                                    'name': 'details',
                                    'icon': 'fa fa-info-circle',
                                    'tooltip': 'User Details',
                                    'type': 'sidenav',

                                }, {
                                    'name': 'permissions',
                                    'icon': 'fa fa-key',
                                    'tooltip': 'Manage User Policies',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'Delete',
                                    'icon': 'fa fa-trash',
                                    'tooltip': 'Delete User'
                                }, {
                                    'name': 'Suspend',
                                    'icon': 'fa fa-ban',
                                    'tooltip': 'Suspend User'
                                }]
                            }, {
                                id: '25fd6vv87-3249-0001-05g6-4d4767890765',
                                name: 'Group 1',
                                status: 'suspended',
                                provider: 'IOAT',
                                type: 'group',
                                actions: [{
                                    'name': 'details',
                                    'icon': 'fa fa-info-circle',
                                    'tooltip': 'User Details',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'permissions',
                                    'icon': 'fa fa-key',
                                    'tooltip': 'Manage User Policies',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'Delete',
                                    'icon': 'fa fa-trash',
                                    'tooltip': 'Delete User'
                                }, {
                                    'name': 'Reauthorize',
                                    'icon': 'fa fa-check-circle',
                                    'tooltip': 'Reauthorize User'
                                }]
                            }, {
                                id: '98f6cc59-0156-1000-06b4-2b0810089090',
                                name: 'G$',
                                status: 'authorized',
                                provider: 'Friendly LDAP Provider',
                                type: 'user',
                                actions: [{
                                    'name': 'details',
                                    'icon': 'fa fa-info-circle',
                                    'tooltip': 'User Details',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'permissions',
                                    'icon': 'fa fa-key',
                                    'tooltip': 'Manage User Policies',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'Delete',
                                    'icon': 'fa fa-trash',
                                    'tooltip': 'Delete User'
                                }, {
                                    'name': 'Suspend',
                                    'icon': 'fa fa-ban',
                                    'tooltip': 'Suspend User'
                                }]
                            }, {
                                id: '65fd6vv87-3249-0001-05g6-4d4767890765',
                                name: 'Group 2',
                                status: 'suspended',
                                provider: 'IOAT',
                                type: 'group',
                                actions: [{
                                    'name': 'details',
                                    'icon': 'fa fa-info-circle',
                                    'tooltip': 'User Details',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'permissions',
                                    'icon': 'fa fa-key',
                                    'tooltip': 'Manage User Policies',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'Delete',
                                    'icon': 'fa fa-trash',
                                    'tooltip': 'Delete User'
                                }, {
                                    'name': 'Reauthorize',
                                    'icon': 'fa fa-check-circle',
                                    'tooltip': 'Reauthorize User'
                                }]
                            }],
                            buckets: [{
                                id: '25fd6vv87-3549-0001-05g6-4d4567890765',
                                name: "My Flows",
                                actions: [{
                                    'name': 'permissions',
                                    'icon': 'fa fa-key',
                                    'tooltip': 'Manage Bucket Policies',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'Delete',
                                    'icon': 'fa fa-trash',
                                    'tooltip': 'Delete Bucket'
                                }],
                                droplets: [{
                                    id: '23f6cc59-0156-1000-09b4-2b0610089090',
                                    name: "Security_Dev_Ops",
                                    displayName: 'Security Dev Ops',
                                    type: 'Data Flow',
                                    sublabel: 'Some info',
                                    updated: new Date(),
                                    description: 'This is the most secure flow ever!',
                                    versions: [{
                                        id: '23f6cc59-0156-1000-06b4-2b0810089090',
                                        revision: '1',
                                        dependentFlows: [{
                                            id: '25fd6vv87-3549-0001-05g6-4d4567890765'
                                        }],
                                        author: '2Hot',
                                        comment: 'delete ListenHttp',
                                        created: new Date(date.setDate(date.getDate() - 1)),
                                        updated: new Date()
                                    }, {
                                        id: '25fd6vv87-3549-0001-05g6-4d4567890765',
                                        revision: '2',
                                        dependentFlows: [{
                                            id: '23f6cc59-0156-1000-06b4-2b0810089090'
                                        }],
                                        author: '2Hot',
                                        comment: 'added Labels for better description of groups of processors',
                                        created: new Date(),
                                        updated: new Date()
                                    }],
                                    flows: [],
                                    extensions: [],
                                    assets: [],
                                    actions: [{
                                        'name': 'Delete',
                                        'icon': 'fa fa-close',
                                        'tooltip': 'Delete User'
                                    }]
                                }]
                            }, {
                                id: '23f6cc59-0156-1000-09b4-2b0810089080',
                                name: "Development Flows",
                                droplets: [{
                                    id: '23f6cc59-0156-1000-09b4-2b0610089090',
                                    name: "Fraud Detection Flow",
                                    displayName: 'Fraud Detection Flow',
                                    type: 'Data Flow',
                                    sublabel: 'A sublabel',
                                    updated: new Date(date.setDate(date.getDate() - 2)),
                                    description: 'This flow detects fraud!',
                                    versions: [{
                                        id: '23f6cc59-0156-1000-06b4-2b0810089090',
                                        revision: '1',
                                        dependentFlows: [{
                                            id: '25fd6vv87-3549-0001-05g6-4d4567890765'
                                        }],
                                        author: 'G$',
                                        comment: 'added funnel',
                                        created: new Date(date.setDate(date.getDate() - 1)),
                                        updated: new Date()
                                    }, {
                                        id: '25fd6vv87-3549-0001-05g6-4d4567890765',
                                        revision: '2',
                                        dependentFlows: [{
                                            id: '23f6cc59-0156-1000-06b4-2b0810089090'
                                        }],
                                        author: '2Hot',
                                        comment: 'added Execute script',
                                        created: new Date(date.setDate(date.getDate() - 1)),
                                        updated: new Date()
                                    }, {
                                        id: '77fd6vv87-3549-0001-05g6-4d4567890765',
                                        revision: '3',
                                        dependentFlows: [{
                                            id: '23f6cc59-0156-1000-06b4-2b0810089090'
                                        }],
                                        author: 'Payne',
                                        comment: 'removed Execute script',
                                        created: new Date(date.setDate(date.getDate() - 1)),
                                        updated: new Date()
                                    }, {
                                        id: '96fd6vv87-3549-0001-05g6-4d4567890765',
                                        revision: '4',
                                        dependentFlows: [{
                                            id: '23f6cc59-0156-1000-06b4-2b0810089090'
                                        }],
                                        author: 'G$',
                                        comment: 'add Execute script',
                                        created: new Date(date.setDate(date.getDate() - 1)),
                                        updated: new Date()
                                    }],
                                    flows: [],
                                    extensions: [],
                                    assets: [],
                                    actions: [{
                                        'name': 'Delete',
                                        'icon': 'fa fa-close'
                                    }]
                                }, {
                                    id: '59f6cc23-0156-1000-09b4-2b0610089090',
                                    name: "Cyber Security",
                                    displayName: 'Cyber Security',
                                    type: 'Data Flow',
                                    sublabel: 'A sublabel',
                                    updated: new Date(date.setDate(date.getDate() - 1)),
                                    description: 'This is the most cyber secure flow ever!',
                                    versions: [{
                                        id: '23f6cc59-0156-1000-06b4-2b0810089090',
                                        revision: '1',
                                        dependentFlows: [{
                                            id: '25fd6vv87-3549-0001-05g6-4d4567890765'
                                        }],
                                        author: 'G$',
                                        comment: 'added funnel',
                                        created: new Date(date.setDate(date.getDate() - 1)),
                                        updated: new Date()
                                    }],
                                    flows: [],
                                    extensions: [],
                                    assets: [],
                                    actions: [{
                                        'name': 'Delete',
                                        'icon': 'fa fa-close',
                                        'tooltip': 'Delete User'
                                    }]
                                }],
                                actions: [{
                                    'name': 'permissions',
                                    'icon': 'fa fa-key',
                                    'tooltip': 'Manage Bucket Policies',
                                    'type': 'sidenav'
                                }, {
                                    'name': 'Delete',
                                    'icon': 'fa fa-trash',
                                    'tooltip': 'Delete Bucket'
                                }]
                            }] // some data model for the contents of a registry
                        }])
                    }, 0);
            }
        );
    },

    getRegistry: function (registryId) {
        return this.getRegistries().then(
            function (registries) {
                return registries.find(
                    function (registry) {
                        if (registryId === registry.id) {
                            return registry;
                        }
                    });
            });
    },

    getDroplet: function (registryId, bucketId, dropletId) {
        return this.getDroplets(registryId, bucketId, dropletId).then(
            function (droplets) {
                return droplets[0];
            });
    },

    getDroplets: function (registryIds, bucketIds, dropletIds) {
        var self = this;
        return this.getRegistries().then(
            function (registries) {
                var buckets = [];

                registries.find(
                    function (registry) {
                        if (registryIds === undefined || registryIds.indexOf(registry.id) >= 0) {
                            registry.buckets.find(
                                function (bucket) {
                                    if (bucketIds === undefined || bucketIds.indexOf(bucket.id) >= 0) {
                                        buckets.push(bucket);
                                    }
                                });
                        }
                    });

                var droplets = [];

                buckets.find(
                    function (bucket) {
                        bucket.droplets.find(
                            function (droplet) {
                                if (dropletIds === undefined || dropletIds.indexOf(droplet.id) >= 0) {
                                    droplets.push(droplet);
                                }
                            });
                    });

                return droplets;
            });

    },

    isDropletFilterChecked: function (term) {
        return (this.dropletsSearchTerms.indexOf(term) > -1);
    },

    getDropletTypeCount: function (type) {
        return this.filteredDroplets.filter(function (droplet) {
            return droplet.type === type;
        }).length;
    },

    getDropletCertificationCount: function (certification) {
        return this.filteredDroplets.filter(function (droplet) {
            return Object.keys(droplet).find(function (key) {
                if (key === certification && droplet[certification].type === 'certification') {
                    return droplet;
                }
            });
        }).length;
    },

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

    sortDroplets: function (sortEvent, column) {
        if (column.sortable === true) {
            // toggle column sort order
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterDroplets(column.name, sortOrder);
            this.activeDropletColumn = column;
            //only one column can be actively sorted so we reset all to inactive
            this.dropletColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    dropletsSearchRemove: function (searchTerm) {
        this.filterDroplets(this.activeDropletColumn.name, this.activeDropletColumn.sortOrder);
    },

    dropletsSearchAdd: function (searchTerm) {
        this.filterDroplets(this.activeDropletColumn.name, this.activeDropletColumn.sortOrder);
    },

    toggleDropletsFilter: function (searchTerm) {
        var applySearchTerm = true;
        // check if the search term is already applied and remove it if true
        if (this.dropletsSearchTerms.length > 0) {
            var arrayLength = this.dropletsSearchTerms.length;
            for (var i = 0; i < arrayLength; i++) {
                var index = this.dropletsSearchTerms.indexOf(searchTerm);
                if (index > -1) {
                    this.dropletsSearchTerms.splice(index, 1);
                    applySearchTerm = false;
                }
            }
        }

        // if we just removed the search term do NOT apply it again
        if (applySearchTerm) {
            this.dropletsSearchTerms.push(searchTerm);
        }

        this.filterDroplets(this.activeDropletColumn.name, this.activeDropletColumn.sortOrder);
    },

    filterDroplets: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in this.dropletColumns
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

        var newData = this.droplets;

        for (var i = 0; i < this.dropletsSearchTerms.length; i++) {
            newData = filterData(newData, this.dropletsSearchTerms[i], true, this.activeDropletColumn.name);
        }

        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.filteredDroplets = newData;
        this.getAutoCompleteDroplets();
    },

    getAutoCompleteDroplets: function () {
        var self = this;
        this.autoCompleteDroplets = [];
        this.dropletColumns.forEach(function (c) {
            return self.filteredDroplets.forEach(function (r) {
                return (r[c.name.toLowerCase()]) ? self.autoCompleteDroplets.push(r[c.name.toLowerCase()].toString()) : '';
            })
        });
    },

    getBucket: function (registryId, bucketId) {
        return this.getBuckets(registryId, bucketId).then(
            function (buckets) {
                return buckets[0];
            });
    },

    getBuckets: function (registryIds, bucketIds) {
        var self = this;
        return this.getRegistries().then(
            function (registries) {
                var buckets = [];

                registries.find(
                    function (registry) {
                        if (registryIds === undefined || registryIds.indexOf(registry.id) >= 0) {
                            registry.buckets.find(
                                function (bucket) {
                                    if (bucketIds === undefined || bucketIds.indexOf(bucket.id) >= 0) {
                                        buckets.push(bucket);
                                    }
                                });
                        }
                    });

                return buckets;
            });

    },

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
        this.getAutoCompleteBuckets();
    },

    getAutoCompleteBuckets: function () {
        var self = this;
        this.autoCompleteBuckets = [];
        this.bucketColumns.forEach(function (c) {
            return self.filteredBuckets.forEach(function (r) {
                return (r[c.name.toLowerCase()]) ? self.autoCompleteBuckets.push(r[c.name.toLowerCase()].toString()) : '';
            });
        });
    },

    sortBuckets: function (sortEvent, column) {
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

    bucketsSearchRemove: function (searchTerm) {
        this.filterDroplets(this.activeBucketsColumn.name, this.activeBucketsColumn.sortOrder);
    },

    bucketsSearchAdd: function (searchTerm) {
        this.filterDroplets(this.activeBucketsColumn.name, this.activeBucketsColumn.sortOrder);
    },

    allFilteredBucketsSelected: function () {
        this.filteredBuckets.forEach(function (c) {
            if (c.checked === undefined || c.checked === false) {
                return false;
            }
        });

        return true;
    },

    toggleBucketSelect: function (row) {
        if (this.allFilteredBucketsSelected()) {
            this.allBucketsSelected = true;
        } else {
            this.allBucketsSelected = false;
        }
    },

    getCertification: function (registryId, certificatonId) {
        return this.getCertifications(registryId, certificatonId).then(
            function (certificatons) {
                return certificatons[0];
            });
    },
    getCertifications: function (registryIds, certificatonIds) {
        var self = this;
        return this.getRegistries().then(
            function (registries) {
                var certificatons = [];

                registries.find(
                    function (registry) {
                        if (registryIds === undefined || registryIds.indexOf(registry.id) >= 0) {
                            registry.certifications.find(
                                function (certificaton) {
                                    if (certificatonIds === undefined || certificatonIds.indexOf(certificaton.id) >= 0) {
                                        certificatons.push(certificaton);
                                    }
                                });
                        }
                    });

                return certificatons;
            });

    },

    filterCertifications: function (sortBy, sortOrder) {
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

        var newData = this.certifications;

        for (var i = 0; i < this.certificationsSearchTerms.length; i++) {
            newData = filterData(newData, this.certificationsSearchTerms[i], true, this.activeBucketColumn.name);
        }

        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.filteredCertifications = newData;
        this.getAutoCompleteCertifications();
    },

    getAutoCompleteCertifications: function () {
        var self = this;
        this.autoCompleteCertifications = [];
        this.bucketColumns.forEach(function (c) {
            return self.filteredCertifications.forEach(function (r) {
                return (r[c.name.toLowerCase()]) ? self.autoCompleteCertifications.push(r[c.name.toLowerCase()].toString()) : '';
            });
        });
    },

    sortCertifications: function (sortEvent, column) {
        if (column.sortable === true) {
            // toggle column sort order
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterCertifications(column.name, sortOrder);
            this.activeCertificationsColumn = column;
            //only one column can be actively sorted so we reset all to inactive
            this.bucketColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    certificationsSearchRemove: function (searchTerm) {
        this.filterDroplets(this.activeCertificationsColumn.name, this.activeCertificationsColumn.sortOrder);
    },

    certificationsSearchAdd: function (searchTerm) {
        this.filterDroplets(this.activeCertificationsColumn.name, this.activeCertificationsColumn.sortOrder);
    },

    getUser: function (registryId, userId) {
        return this.getUsers(registryId, userId).then(
            function (users) {
                return users[0];
            });
    },

    getUsers: function (registryIds, userIds) {
        var self = this;
        return this.getRegistries().then(
            function (registries) {
                var users = [];

                registries.find(
                    function (registry) {
                        if (registryIds === undefined || registryIds.indexOf(registry.id) >= 0) {
                            registry.users.find(
                                function (user) {
                                    if (userIds === undefined || userIds.indexOf(user.id) >= 0) {
                                        users.push(user);
                                    }
                                });
                        }
                    });

                return users;
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
        this.usersCurrentPage = 1;
        this.usersFromRow = 1;
        this.usersPageSize = 1;
        this.filterUsers();
    },

    usersSearchAdd: function (searchTerm) {
        this.usersSearchTerms.push(searchTerm);
        this.usersCurrentPage = 1;
        this.usersFromRow = 1;
        this.usersPageSize = 1;
        this.filterUsers();
    },

    pageUsers: function (pagingEvent) {
        this.usersFromRow = pagingEvent.fromRow;
        this.usersCurrentPage = pagingEvent.page;
        this.usersPageSize = pagingEvent.pageSize;
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
        this.usersPageCount = newData.length;
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

    setBreadcrumbState: function (state) {
        this.breadCrumbState = state;
    }
};

NfRegistryService.parameters = [covalentCore.TdDataTableService];

module.exports = NfRegistryService;
