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
var fdsDialogsModule = require('@fluid-design-system/dialogs');
var fdsSnackBarsModule = require('@fluid-design-system/snackbars');
var ngCore = require('@angular/core');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var ngRouter = require('@angular/router');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var ngMaterial = require('@angular/material');
var NfRegistryAddUsersToGroup = require('nifi-registry/components/administration/users/dialogs/add-users-to-group/nf-registry-add-users-to-group.js');

/**
 * NfRegistryManageGroup constructor.
 *
 * @param nfRegistryApi         The api service.
 * @param nfRegistryService     The nf-registry.service module.
 * @param tdDataTableService    The covalent data table service module.
 * @param fdsDialogService      The FDS dialog service.
 * @param fdsSnackBarService    The FDS snack bar service module.
 * @param activatedRoute        The angular route module.
 * @param router                The angular router module.
 * @param matDialog             The angular material dialog module.
 * @constructor
 */
function NfRegistryManageGroup(nfRegistryApi, nfRegistryService, tdDataTableService, fdsDialogService, fdsSnackBarService, activatedRoute, router, matDialog) {
    this.filteredUsers = [];
    this.usersSearchTerms = [];
    this._groupname = '';
    this.manageGroupPerspective = 'membership';

    // Services
    this.nfRegistryService = nfRegistryService;
    this.route = activatedRoute;
    this.router = router;
    this.dialog = matDialog;
    this.nfRegistryApi = nfRegistryApi;
    this.dialogService = fdsDialogService;
    this.snackBarService = fdsSnackBarService;
    this.dataTableService = tdDataTableService;
};

NfRegistryManageGroup.prototype = {
    constructor: NfRegistryManageGroup,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.nfRegistryService.sidenav.open();

        // subscribe to the route params
        this.$subscription = self.route.params
            .switchMap(function (params) {
                return self.nfRegistryApi.getUserGroup(params['groupId']);
            })
            .subscribe(function (response) {
                self.nfRegistryService.group = response;
                self._groupname = response.identity;
                self.filterUsers();
            });
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.sidenav.close();
        this.$subscription.unsubscribe();
    },

    /**
     * Navigate to administer users for current registry.
     */
    closeSideNav: function () {
        this.router.navigateByUrl('/nifi-registry/administration/users');
    },

    /**
     * Toggles the manage bucket privileges for the group.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleGroupManageBucketsPrivileges: function ($event, policyAction) {
        var self = this;
        if ($event.checked) {
            for (var resource in this.nfRegistryService.BUCKETS_PRIVS) {
                if (this.nfRegistryService.BUCKETS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.BUCKETS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, self.nfRegistryService.group.users, []).subscribe(
                                        function (response) {
                                            // can manage buckets privileges created and granted!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.userGroups.push(self.nfRegistryService.group);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage buckets privileges updated!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        } else {
            // Remove the current group from the administrator resources
            for (var resource in this.nfRegistryService.BUCKETS_PRIVS) {
                if (this.nfRegistryService.BUCKETS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.BUCKETS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current group and update it
                                    policy.userGroups = policy.userGroups.filter(function (group) {
                                        return (group.identifier !== self.nfRegistryService.group.identifier) ? true : false;
                                    });
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage buckets privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                self.nfRegistryService.group = response;
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        }
    },

    /**
     * Toggles the manage tenants privileges for the group.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleGroupManageTenantsPrivileges: function ($event, policyAction) {
        var self = this;
        if ($event.checked) {
            for (var resource in this.nfRegistryService.TENANTS_PRIVS) {
                if (this.nfRegistryService.TENANTS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.TENANTS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, self.nfRegistryService.group.users, []).subscribe(
                                        function (response) {
                                            // can manage tenants privileges created and granted!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.userGroups.push(self.nfRegistryService.group);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage tenants privileges updated!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        } else {
            // Remove the current group from the administrator resources
            for (var resource in this.nfRegistryService.TENANTS_PRIVS) {
                if (this.nfRegistryService.TENANTS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.TENANTS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current group and update it
                                    policy.userGroups = policy.userGroups.filter(function (group) {
                                        return (group.identifier !== self.nfRegistryService.group.identifier) ? true : false;
                                    });
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage tenants privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                self.nfRegistryService.group = response;
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        }
    },

    /**
     * Toggles the manage policies privileges for the group.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleGroupManagePoliciesPrivileges: function ($event, policyAction) {
        var self = this;
        if ($event.checked) {
            for (var resource in this.nfRegistryService.POLICIES_PRIVS) {
                if (this.nfRegistryService.POLICIES_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.POLICIES_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, self.nfRegistryService.group.users, []).subscribe(
                                        function (response) {
                                            // can manage policies privileges created and granted!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.userGroups.push(self.nfRegistryService.group);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage policies privileges updated!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        } else {
            // Remove the current group from the administrator resources
            for (var resource in this.nfRegistryService.POLICIES_PRIVS) {
                if (this.nfRegistryService.POLICIES_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.POLICIES_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current group and update it
                                    policy.userGroups = policy.userGroups.filter(function (group) {
                                        return (group.identifier !== self.nfRegistryService.group.identifier) ? true : false;
                                    });
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage policies privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                self.nfRegistryService.group = response;
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        }
    },

    /**
     * Toggles the manage proxy privileges for the group.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleGroupManageProxyPrivileges: function ($event, policyAction) {
        var self = this;
        if ($event.checked) {
            for (var resource in this.nfRegistryService.PROXY_PRIVS) {
                if (this.nfRegistryService.PROXY_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.PROXY_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, self.nfRegistryService.group.users, []).subscribe(
                                        function (response) {
                                            // can manage proxy privileges created and granted!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.userGroups.push(self.nfRegistryService.group);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage proxy privileges updated!!!...now update the view
                                            response.userGroups.forEach(function (group) {
                                                if (group.identifier === self.nfRegistryService.group.identifier) {
                                                    self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.group = response;
                                                    });
                                                }
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        } else {
            // Remove the current group from the administrator resources
            for (var resource in this.nfRegistryService.PROXY_PRIVS) {
                if (this.nfRegistryService.PROXY_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.PROXY_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current group and update it
                                    policy.userGroups = policy.userGroups.filter(function (group) {
                                        return (group.identifier !== self.nfRegistryService.group.identifier) ? true : false;
                                    });
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage proxy privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier).subscribe(function (response) {
                                                self.nfRegistryService.group = response;
                                            });
                                        });
                                }
                            });
                        }
                    });
                }
            }
        }
    },

    /**
     * Opens a modal dialog UX enabling the addition of users to this group.
     */
    addUsersToGroup: function () {
        var self = this;
        this.dialog.open(NfRegistryAddUsersToGroup, {
            data: {
                group: this.nfRegistryService.group,
                disableClose: true
            }
        }).afterClosed().subscribe(function () {
            self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier)
                .subscribe(function (response) {
                    self.nfRegistryService.group = response;
                    self._groupname = response.identity;
                    self.filterUsers();
                });
        });
    },

    /**
     * Filter users.
     *
     * @param {string} [sortBy]       The column name to sort `userGroupsColumns` by.
     * @param {string} [sortOrder]    The order. Either 'ASC' or 'DES'
     */
    filterUsers: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in `userGroupsColumns`
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

        var newUsersData = this.nfRegistryService.group.users || [];

        for (var i = 0; i < this.usersSearchTerms.length; i++) {
            newUsersData = this.filterData(newUsersData, this.usersSearchTerms[i], true);
        }

        newUsersData = this.dataTableService.sortData(newUsersData, sortBy, sortOrder);
        this.filteredUsers = newUsersData;
    },

    /**
     * Sort `users` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortUsers: function (column) {
        if (column.sortable) {
            var sortBy = column.name;
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterUsers(sortBy, sortOrder);

            //only one column can be actively sorted so we reset all to inactive
            this.nfRegistryService.userGroupsColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    /**
     * Remove user from group.
     *
     * @param user
     */
    removeUserFromGroup: function (user) {
        var self = this;
        var users = this.nfRegistryService.group.users.filter(function (u) {
            if (u.identifier !== user.identifier) {
                return u;
            }
        });

        this.nfRegistryApi.updateUserGroup(this.nfRegistryService.group.identifier, this.nfRegistryService.group.identity, users).subscribe(function (response) {
            self.nfRegistryApi.getUserGroup(self.nfRegistryService.group.identifier)
                .subscribe(function (response) {
                    self.nfRegistryService.group = response;
                    self.filterUsers();
                });
            var snackBarRef = self.snackBarService.openCoaster({
                title: 'Success',
                message: 'The user has been removed from the ' + self.nfRegistryService.group.identity + ' group.',
                verticalPosition: 'bottom',
                horizontalPosition: 'right',
                icon: 'fa fa-check-circle-o',
                color: '#1EB475',
                duration: 3000
            });
        });
    },

    /**
     * Update group name.
     *
     * @param groupname
     */
    updateGroupName: function (groupname) {
        var self = this;
        this.nfRegistryApi.updateUserGroup(this.nfRegistryService.group.identifier, groupname, this.nfRegistryService.group.users).subscribe(function (response) {
            if (!response.status || response.status === 200) {
                self.nfRegistryService.group = response;
                self.nfRegistryService.groups.filter(function (group) {
                    if (self.nfRegistryService.group.identifier === group.identifier) {
                        group.identity = response.identity;
                    }
                });
                var snackBarRef = self.snackBarService.openCoaster({
                    title: 'Success',
                    message: 'This group name has been updated.',
                    verticalPosition: 'bottom',
                    horizontalPosition: 'right',
                    icon: 'fa fa-check-circle-o',
                    color: '#1EB475',
                    duration: 3000
                });
            } else if (response.status === 404) {
                self._groupname = self.nfRegistryService.group.identity;
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: 'This group already exists. Please enter a different identity/group name.',
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
            }
        });
    }
};

NfRegistryManageGroup.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-manage-group.html!text')
    })
];

NfRegistryManageGroup.parameters = [
    NfRegistryApi,
    NfRegistryService,
    covalentCore.TdDataTableService,
    fdsDialogsModule.FdsDialogService,
    fdsSnackBarsModule.FdsSnackBarService,
    ngRouter.ActivatedRoute,
    ngRouter.Router,
    ngMaterial.MatDialog
];

module.exports = NfRegistryManageGroup;
