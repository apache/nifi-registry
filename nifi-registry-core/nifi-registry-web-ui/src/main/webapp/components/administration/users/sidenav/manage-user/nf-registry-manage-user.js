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
var fdsDialogsModule = require('@flow-design-system/dialogs');
var fdsSnackBarsModule = require('@flow-design-system/snackbars');
var ngCore = require('@angular/core');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var ngRouter = require('@angular/router');
var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var ngMaterial = require('@angular/material');
var NfRegistryAddUserToGroups = require('nifi-registry/components/administration/users/dialogs/add-user-to-groups/nf-registry-add-user-to-groups.js');

/**
 * NfRegistryManageUser constructor.
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
function NfRegistryManageUser(nfRegistryApi, nfRegistryService, tdDataTableService, fdsDialogService, fdsSnackBarService, activatedRoute, router, matDialog) {
    // local state
    this.sortBy;
    this.sortOrder;
    this.filteredUserGroups = [];
    this.userGroupsSearchTerms = [];
    this._username = '';
    this.manageUserPerspective = 'membership';
    this.userGroupsColumns = [
        {
            name: 'identity',
            label: 'Display Name',
            sortable: true,
            tooltip: 'Group name.',
            width: 100
        }
    ];

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

NfRegistryManageUser.prototype = {
    constructor: NfRegistryManageUser,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        // subscribe to the route params
        this.$subscription = self.route.params
            .switchMap(function (params) {
                return self.nfRegistryApi.getUser(params['userId']);
            })
            .subscribe(function (response) {
                if (!response.status || response.status === 200) {
                    self.nfRegistryService.sidenav.open();
                    self.nfRegistryService.user = response;
                    self._username = response.identity;
                    self.filterGroups(this.sortBy, this.sortOrder);
                } else if (response.status === 404) {
                    self.router.navigateByUrl('/nifi-registry/administration/users');
                } else if (response.status === 409) {
                    self.router.navigateByUrl('/nifi-registry/administration/workflow');
                }
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
     * Toggles the manage bucket privileges for the user.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleUserManageBucketsPrivileges: function ($event, policyAction) {
        var self = this;
        if($event.checked) {
            for (var resource in this.nfRegistryService.BUCKETS_PRIVS) {
                if (this.nfRegistryService.BUCKETS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.BUCKETS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, [self.nfRegistryService.user], []).subscribe(
                                        function (response) {
                                            // can manage buckets privileges created and granted!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function(response) {
                                                        self.nfRegistryService.user = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.users.push(self.nfRegistryService.user);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage buckets privileges updated!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function(response) {
                                                        self.nfRegistryService.user = response;
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
            // Remove the current user from the /buckets resources
            for (var resource in this.nfRegistryService.BUCKETS_PRIVS) {
                if (this.nfRegistryService.BUCKETS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.BUCKETS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current user and update it
                                    policy.users = policy.users.filter(function (user) {
                                        return (user.identifier !== self.nfRegistryService.user.identifier) ? true : false;
                                    });
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage buckets privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function(response) {
                                                self.nfRegistryService.user = response;
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
     * Toggles the manage tenants privileges for the user.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleUserManageTenantsPrivileges: function ($event, policyAction) {
        var self = this;
        if($event.checked) {
            for (var resource in this.nfRegistryService.TENANTS_PRIVS) {
                if (this.nfRegistryService.TENANTS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.TENANTS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, [self.nfRegistryService.user], []).subscribe(
                                        function (response) {
                                            // can manage tenants privileges created and granted!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.user = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.users.push(self.nfRegistryService.user);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage tenants privileges updated!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.user = response;
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
            // Remove the current user from the administrator resources
            for (var resource in this.nfRegistryService.TENANTS_PRIVS) {
                if (this.nfRegistryService.TENANTS_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.TENANTS_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current user and update it
                                    policy.users = policy.users.filter(function (user) {
                                        return (user.identifier !== self.nfRegistryService.user.identifier) ? true : false;
                                    });
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage tenants privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                self.nfRegistryService.user = response;
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
     * Toggles the manage policies privileges for the user.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleUserManagePoliciesPrivileges: function ($event, policyAction) {
        var self = this;
        if($event.checked) {
            for (var resource in this.nfRegistryService.POLICIES_PRIVS) {
                if (this.nfRegistryService.POLICIES_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.POLICIES_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, [self.nfRegistryService.user], []).subscribe(
                                        function (response) {
                                            // can manage policies privileges created and granted!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.user = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.users.push(self.nfRegistryService.user);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage policies privileges updated!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.user = response;
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
            // Remove the current user from the administrator resources
            for (var resource in this.nfRegistryService.POLICIES_PRIVS) {
                if (this.nfRegistryService.POLICIES_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.POLICIES_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current user and update it
                                    policy.users = policy.users.filter(function (user) {
                                        return (user.identifier !== self.nfRegistryService.user.identifier) ? true : false;
                                    });
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage policies privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                self.nfRegistryService.user = response;
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
     * Toggles the manage proxy privileges for the user.
     *
     * @param $event
     * @param policyAction      The action to be toggled
     */
    toggleUserManageProxyPrivileges: function ($event, policyAction) {
        var self = this;
        if($event.checked) {
            for (var resource in this.nfRegistryService.PROXY_PRIVS) {
                if (this.nfRegistryService.PROXY_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.PROXY_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist, let's create it
                                    self.nfRegistryApi.postPolicyActionResource(action, resource, [self.nfRegistryService.user], []).subscribe(
                                        function (response) {
                                            // can manage proxy privileges created and granted!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.user = response;
                                                    });
                                                }
                                            });
                                        });
                                } else {
                                    // resource exists, let's update it
                                    policy.users.push(self.nfRegistryService.user);
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // can manage proxy privileges updated!!!...now update the view
                                            response.users.forEach(function (user) {
                                                if (user.identifier === self.nfRegistryService.user.identifier) {
                                                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                        self.nfRegistryService.user = response;
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
            // Remove the current user from the administrator resources
            for (var resource in this.nfRegistryService.PROXY_PRIVS) {
                if (this.nfRegistryService.PROXY_PRIVS.hasOwnProperty(resource)) {
                    this.nfRegistryService.PROXY_PRIVS[resource].forEach(function (action) {
                        if (!policyAction || (action === policyAction)) {
                            self.nfRegistryApi.getPolicyActionResource(action, resource).subscribe(function (policy) {
                                if (policy.status && policy.status === 404) {
                                    // resource does NOT exist
                                } else {
                                    // resource exists, let's filter out the current user and update it
                                    policy.users = policy.users.filter(function (user) {
                                        return (user.identifier !== self.nfRegistryService.user.identifier) ? true : false;
                                    })
                                    self.nfRegistryApi.putPolicyActionResource(policy.identifier, policy.action,
                                        policy.resource, policy.users, policy.userGroups).subscribe(
                                        function (response) {
                                            // administrator privileges updated!!!...now update the view
                                            self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier).subscribe(function (response) {
                                                self.nfRegistryService.user = response;
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
     * Opens a modal dialog UX enabling the addition of this user to multiple groups.
     */
    addUserToGroups: function () {
        var self = this;
        this.dialog.open(NfRegistryAddUserToGroups, {
            data: {
                user: this.nfRegistryService.user,
                disableClose: true
            }
        }).afterClosed().subscribe(function () {
            self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier)
                .subscribe(function (response) {
                    self.nfRegistryService.user = response;
                    self._username = response.identity;
                    self.filterGroups(this.sortBy, this.sortOrder);
                });
        });
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
            if (this.sortOrder === undefined) {
                sortOrder = 'ASC'
            } else {
                sortOrder = this.sortOrder
            }
        }
        // if `sortBy` is `undefined` then find the first sortable column in `userGroupsColumns`
        if (sortBy === undefined) {
            if (this.sortBy === undefined) {
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
            } else {
                sortBy = this.sortBy
            }
        }

        var newUserGroupsData = this.nfRegistryService.user.userGroups || [];

        for (var i = 0; i < this.userGroupsSearchTerms.length; i++) {
            newUserGroupsData = this.dataTableService.filterData(newUserGroupsData, this.userGroupsSearchTerms[i], true);
        }

        newUserGroupsData = this.dataTableService.sortData(newUserGroupsData, sortBy, sortOrder);
        this.filteredUserGroups = newUserGroupsData;
    },

    /**
     * Sort `groups` by `column`.
     *
     * @param column    The column to sort by.
     */
    sortGroups: function (column) {
        if (column.sortable) {
            this.sortBy = column.name;
            this.sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterGroups(this.sortBy, this.sortOrder);

            //only one column can be actively sorted so we reset all to inactive
            this.userGroupsColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    /**
     * Remove user from group.
     *
     * @param group
     */
    removeUserFromGroup: function (group) {
        var self = this;
        this.nfRegistryApi.getUserGroup(group.identifier).subscribe(function (response) {
            if (!response.error) {
                var fullGroup = response;
                var users = fullGroup.users.filter(function (user) {
                    if (self.nfRegistryService.user.identifier !== user.identifier) {
                        return user;
                    }
                })
                self.nfRegistryApi.updateUserGroup(fullGroup.identifier, fullGroup.identity, users).subscribe(function (response) {
                    self.nfRegistryApi.getUser(self.nfRegistryService.user.identifier)
                        .subscribe(function (response) {
                            self.nfRegistryService.user = response;
                            self.filterGroups(this.sortBy, this.sortOrder);
                        });
                    var snackBarRef = self.snackBarService.openCoaster({
                        title: 'Success',
                        message: 'This user has been removed from the ' + group.identity + ' group.',
                        verticalPosition: 'bottom',
                        horizontalPosition: 'right',
                        icon: 'fa fa-check-circle-o',
                        color: '#1EB475',
                        duration: 3000
                    });
                });
            }
        });
    },

    /**
     * Update user name.
     *
     * @param username
     */
    updateUserName: function (username) {
        var self = this;
        this.nfRegistryApi.updateUser(this.nfRegistryService.user.identifier, username).subscribe(function (response) {
            if(!response.status || response.status === 200) {
                self.nfRegistryService.user = response;
                self.nfRegistryService.users.filter(function(user) {
                    if (self.nfRegistryService.user.identifier === user.identifier){
                        user.identity = response.identity;
                    }
                });
                var snackBarRef = self.snackBarService.openCoaster({
                    title: 'Success',
                    message: 'This user name has been updated.',
                    verticalPosition: 'bottom',
                    horizontalPosition: 'right',
                    icon: 'fa fa-check-circle-o',
                    color: '#1EB475',
                    duration: 3000
                });
            } else if (response.status === 409) {
                self._username = self.nfRegistryService.user.identity;
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: 'This user already exists. Please enter a different identity/user name.',
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
            }
        });
    },

    /**
     * Determine disabled state of 'Add to Groups' button
     * @returns {boolean}
     */
    canAddNonConfigurableUserToGroup: function() {
        var disabled = true;
        this.nfRegistryService.groups.forEach(function (userGroup) {
            if(userGroup.configurable === true){
                disabled = false;
            }
        });
        return disabled;
    },

    /**
     * Determine if 'Special Privileges' can be edited.
     * @returns {boolean}
     */
    canEditSpecialPrivileges: function() {
        return this.nfRegistryService.currentUser.resourcePermissions.policies.canWrite
                && !(this.nfRegistryService.currentUser.identity === this.nfRegistryService.user.identity)
                && this.nfRegistryService.registry.config.supportsConfigurableAuthorizer;
    }

};

NfRegistryManageUser.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-manage-user.html!text')
    })
];

NfRegistryManageUser.parameters = [
    NfRegistryApi,
    NfRegistryService,
    covalentCore.TdDataTableService,
    fdsDialogsModule.FdsDialogService,
    fdsSnackBarsModule.FdsSnackBarService,
    ngRouter.ActivatedRoute,
    ngRouter.Router,
    ngMaterial.MatDialog
];

module.exports = NfRegistryManageUser;
