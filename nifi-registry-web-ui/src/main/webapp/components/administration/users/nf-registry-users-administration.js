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
var ngRouter = require('@angular/router');
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');
var fdsDialogsModule = require('@fluid-design-system/dialogs');

/**
 * NfRegistryUsersAdministration constructor.
 *
 * @param nfRegistryService     The nf-registry.service module.
 * @param ActivatedRoute        The angular activated route module.
 * @param Router                The angular router module.
 * @param FdsDialogService      The FDS dialog service.
 * @constructor
 */
function NfRegistryUsersAdministration(nfRegistryService, ActivatedRoute, Router, FdsDialogService) {
    this.route = ActivatedRoute;
    this.nfRegistryService = nfRegistryService;
    this.router = Router;
    this.dialogService = FdsDialogService;
};

NfRegistryUsersAdministration.prototype = {
    constructor: NfRegistryUsersAdministration,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        var self = this;
        this.route.params
            .switchMap(function (params) {
                self.nfRegistryService.adminPerspective = 'users';
                return self.nfRegistryService.api.getUsers();
            })
            .subscribe(function (users) {
                self.nfRegistryService.users = self.nfRegistryService.filteredUsers = users;
                self.nfRegistryService.filterUsers();
            });
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.nfRegistryService.adminPerspective = '';
        this.nfRegistryService.users = this.nfRegistryService.filteredUsers = [];
        this.nfRegistryService.allUsersSelected = false;
    },

    /**
     * Execute the given user action.
     *
     * @param action        The action object.
     * @param user          The user object the `action` will act upon.
     */
    execute: function (action, user) {
        var self = this;
        if (user) {
            user.checked = !user.checked;
        }
        switch (action.name.toLowerCase()) {
            case 'delete':
                this.dialogService.openConfirm({
                    title: 'Delete User',
                    message: 'User will be deleted.',
                    cancelButton: 'Cancel',
                    acceptButton: 'Delete',
                    acceptButtonColor: 'fds-warn'
                }).afterClosed().subscribe(
                    function (accept) {
                        if (accept) {
                            self.nfRegistryService.api.deleteUser(user.identifier);
                        }
                    });
                break;
            case 'suspend':
                this.dialogService.openConfirm({
                    title: 'Suspend User',
                    message: 'User permissions will be suspended.',
                    cancelButton: 'Cancel',
                    acceptButton: 'Confirm',
                    acceptButtonColor: 'fds-critical'
                }).afterClosed().subscribe(
                    function (accept) {
                        if (accept) {
                            self.nfRegistryService.api.suspendUser(user.identifier);
                        }
                    });
                break;
            case 'add':
                this.router.navigateByUrl('/nifi-registry/administration/users(sidenav:user/add)');
                break;
            default:
                this.router.navigateByUrl('/nifi-registry/administration/users(' + action.type + ':user/' + action.name + '/' + user.id + ')');
                break;
        }
    }
};

NfRegistryUsersAdministration.annotations = [
    new ngCore.Component({
        template: require('./nf-registry-users-administration.html!text'),
        animations: [nfRegistryAnimations.slideInLeftAnimation],
        host: {
            '[@routeAnimation]': 'routeAnimation'
        }
    })
];

NfRegistryUsersAdministration.parameters = [NfRegistryService, ngRouter.ActivatedRoute, ngRouter.Router, fdsDialogsModule.FdsDialogService];

module.exports = NfRegistryUsersAdministration;
