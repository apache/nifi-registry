/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the 'License'); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var NfRegistryApi = require('nifi-registry/services/nf-registry.api.js');
var NfRegistryService = require('nifi-registry/services/nf-registry.service.js');
var NfRegistryAddSelectedToGroup = require('nifi-registry/components/administration/users/dialogs/add-selected-users-to-group/nf-registry-add-selected-users-to-group.js');
var rxjs = require('rxjs/Rx');
var covalentCore = require('@covalent/core');
var fdsSnackBarsModule = require('@fluid-design-system/snackbars');

describe('NfRegistryAddSelectedToGroup Component isolated unit tests', function () {
    var comp;
    var nfRegistryService;
    var nfRegistryApi;
    var snackBarService;
    var dataTableService;

    beforeEach(function () {
        nfRegistryService = new NfRegistryService();
        // setup the nfRegistryService
        nfRegistryService.groups = [{identifier: 1, identity: 'Group 1'}];
        nfRegistryService.filteredUsers = [{identifier: 2, identity: 'User 1'}];

        nfRegistryApi = new NfRegistryApi();
        snackBarService = new fdsSnackBarsModule.FdsSnackBarService();
        dataTableService = new covalentCore.TdDataTableService();
        comp = new NfRegistryAddSelectedToGroup(nfRegistryApi, dataTableService, nfRegistryService, {
            close: function () {
            }
        }, snackBarService);

        // Spy
        spyOn(nfRegistryApi, 'getUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({identifier: 1, identity: 'Group 1'}));
        spyOn(nfRegistryApi, 'updateUserGroup').and.callFake(function () {
        }).and.returnValue(rxjs.Observable.of({identifier: 1, identity: 'Group 1'}));
        spyOn(comp.dialogRef, 'close');
        spyOn(comp.snackBarService, 'openCoaster');
        spyOn(comp, 'filterGroups').and.callThrough();

        // initialize the component
        comp.ngOnInit();

        //assertions
        expect(comp.filterGroups).toHaveBeenCalled();
        expect(comp.filteredUserGroups[0].identity).toEqual('Group 1');
        expect(comp.filteredUserGroups.length).toBe(1);
        expect(comp).toBeDefined();
    });

    it('should make a call to the api to add selected users to selected groups', function () {
        // select a group
        comp.filteredUserGroups[0].checked = true;

        // the function to test
        comp.addSelectedUsersToSelectedGroups();

        //assertions
        expect(comp.dialogRef.close).toHaveBeenCalled();
        expect(comp.snackBarService.openCoaster).toHaveBeenCalled();
    });

    it('should determine all user groups are selected', function () {
        // select a group
        comp.filteredUserGroups[0].checked = true;

        // the function to test
        comp.determineAllUserGroupsSelectedState();

        //assertions
        expect(comp.allGroupsSelected).toBe(true);
        expect(comp.isAddSelectedUsersToSelectedGroupsDisabled).toBe(false);
    });

    it('should determine all user groups are not selected', function () {
        // the function to test
        comp.determineAllUserGroupsSelectedState();

        //assertions
        expect(comp.allGroupsSelected).toBe(false);
        expect(comp.isAddSelectedUsersToSelectedGroupsDisabled).toBe(true);
    });

    it('should select all groups.', function () {
        // The function to test
        comp.selectAllUserGroups();

        //assertions
        expect(comp.filteredUserGroups[0].checked).toBe(true);
        expect(comp.isAddSelectedUsersToSelectedGroupsDisabled).toBe(false);
        expect(comp.allGroupsSelected).toBe(true);
    });

    it('should deselect all groups.', function () {
        // select a group
        comp.filteredUserGroups[0].checked = true;

        // The function to test
        comp.deselectAllUserGroups();

        //assertions
        expect(comp.filteredUserGroups[0].checked).toBe(false);
        expect(comp.isAddSelectedUsersToSelectedGroupsDisabled).toBe(true);
        expect(comp.allGroupsSelected).toBe(false);
    });

    it('should toggle all groups `checked` properties to true.', function () {
        //Spy
        spyOn(comp, 'selectAllUserGroups').and.callFake(function () {
        });

        comp.allGroupsSelected = true;

        // The function to test
        comp.toggleUserGroupsSelectAll();

        //assertions
        expect(comp.selectAllUserGroups).toHaveBeenCalled();
    });

    it('should toggle all groups `checked` properties to false.', function () {
        //Spy
        spyOn(comp, 'deselectAllUserGroups').and.callFake(function () {
        });

        comp.allGroupsSelected = false;

        // The function to test
        comp.toggleUserGroupsSelectAll();

        //assertions
        expect(comp.deselectAllUserGroups).toHaveBeenCalled();
    });

    it('should sort `groups` by `column`', function () {
        // object to be updated by the test
        var column = {name: 'name', label: 'Group Name', sortable: true};

        // The function to test
        comp.sortUserGroups(column);

        //assertions
        var filterGroupsCall = comp.filterGroups.calls.mostRecent();
        expect(filterGroupsCall.args[0]).toBe('name');
        expect(filterGroupsCall.args[1]).toBe('ASC');
    });

    it('should cancel the creation of a new user', function () {
        // the function to test
        comp.cancel();

        //assertions
        expect(comp.dialogRef.close).toHaveBeenCalled();
    });
});