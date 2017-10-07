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

var ngHttp = require('@angular/http');
var fdsDialogsModule = require('@fluid-design-system/dialogs');
var rxjs = require('rxjs/Rx');

var headers = new Headers({'Content-Type': 'application/json'});

/**
 * NfRegistryApi constructor.
 *
 * @param Http                  The angular http module.
 * @param FdsDialogService      The FDS dialog service.
 * @constructor
 */
function NfRegistryApi(Http, FdsDialogService) {
    this.http = Http;
    this.dialogService = FdsDialogService;
};

NfRegistryApi.prototype = {
    constructor: NfRegistryApi,

    /**
     * Retrieves the snapshot metadata for an existing droplet the registry has stored.
     *
     * If verbose is true, then the metadata about all snapshots for the droplet will also be returned.
     *
     * @param {string}  dropletType     The type of the droplet to request.
     * @param {boolean} [verbose]       Flag to determine whether or not version children should be included in the response.
     * @returns {*}
     */
    getDropletSnapshotMetadata: function (dropletUri, verbose) {
        var self = this;
        var url = '/nifi-registry-api/' + dropletUri;
        if (verbose) {
            url += '?verbose=true';
        }
        return this.http.get(url)
            .map(function (response) {
                return response.json().snapshotMetadata || [];
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    /**
     * Retrieves the given droplet with or without snapshot metadata.
     *
     * @param {string}  bucketId        The id of the bucket to request.
     * @param {string}  dropletType     The type of the droplet to request.
     * @param {string}  dropletId       The id of the droplet to request.
     * @param {boolean} [verbose]       Flag to determine whether or not version children should be included in the response.
     * @returns {*}
     */
    getDroplet: function (bucketId, dropletType, dropletId, verbose) {
        var self = this;
        var url = '/nifi-registry-api/buckets/' + bucketId + '/' + dropletType + '/' + dropletId;
        if (verbose) {
            url += '?verbose=true';
        }
        return this.http.get(url)
            .map(function (response) {
                return response.json() || {};
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    /**
     * Retrieves all droplets across all buckets (unless the `bucketId` is set then this will
     * retrieve all droplets across that specific bucket). Droplets could include flows, extensions, etc.
     *
     * No snapshot metadata ever returned.
     *
     * @param {string} [bucketId] Defines a bucket id for filtering results.
     * @returns {*}
     */
    getDroplets: function (bucketId) {
        var self = this;
        var url = '/nifi-registry-api/items';
        if (bucketId) {
            url += '/' + bucketId;
        }
        return this.http.get(url)
            .map(function (response) {
                return response.json() || [];
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    /**
     * Delete an existing droplet the registry has stored.
     *
     * @param {string} dropletUri    The portion of the URI describing the type of the droplet and its identifier
     *
     *  Ex:
     *      'flows/1234'
     *      'extension/5678'
     *
     * @returns {*}
     */
    deleteDroplet: function (dropletUri) {
        var self = this;
        return this.http.delete('/nifi-registry-api/' + dropletUri, headers)
            .map(function (response) {
                var body = response.json();
                return response.json() || {};
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    /**
     * Create a named bucket capable of storing NiFi bucket objects (aka droplets) such as flows and extension bundles.
     *
     * @param {string} name  The name of the bucket.
     * @returns {*}
     */
    createBucket: function (name) {
        var self = this;
        return this.http.post('/nifi-registry-api/buckets', {'name': name}, headers)
            .map(function (response) {
                var body = response.json();
                return response.json() || {};
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    /**
     * Delete an existing bucket in the registry, along with all the objects it is storing.
     *
     * @param {string} bucketId     The identifier of the bucket to be deleted.
     * @returns {*}
     */
    deleteBucket: function (bucketId) {
        var self = this;
        return this.http.delete('/nifi-registry-api/buckets/' + bucketId, headers)
            .map(function (response) {
                var body = response.json();
                return response.json() || {};
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    /**
     * Get metadata for an existing bucket in the registry.
     *
     * If verbose is set to true, then each bucket will be returned with the
     * set of items in the bucket, but any further children (version snapshot metadata)
     * of those items will not be included.
     *
     * @param {string} bucketId     The identifier of the bucket to retrieve.
     * @param {bool} [verbose]      Flag indicating whether to include the set of items
     *                              (NiFi bucket objects such as flows and extension
     *                              bundles...otherwise known as droplets) in the bucket.
     * @returns {*}
     */
    getBucket: function (bucketId, verbose) {
        var self = this;
        var url = '/nifi-registry-api/buckets/' + bucketId;
        if (verbose) {
            url += '?verbose=true';
        }
        return this.http.get(url)
            .map(function (response) {
                return response.json() || {};
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    /**
     * Get metadata for all buckets in the registry for which the client is authorized.
     *
     * NOTE: Information about the items stored in each bucket should be obtained by
     * requesting and individual bucket by id.
     *
     * @returns {*}
     */
    getBuckets: function () {
        var self = this;
        var url = '/nifi-registry-api/buckets';
        return this.http.get(url)
            .map(function (response) {
                var body = response.json();
                return response.json() || [];
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    //TODO: REST call to API to get user by id.
    getUser: function (userId) {
        var self = this;
        return this.http.get('/nifi-registry-api/users/' + userId)
            .map(function (response) {
                return response.json() || {};
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error._body,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.throw(error._body);
            });
    },

    //TODO: REST call to API to get users.
    getUsers: function (userIds, bucketId) {
        var self = this;
        return new Promise(
            function (resolve) {
                resolve([{
                    id: '23f6cc59-0156-1000-06b4-2b0810089090',
                    name: 'Scotty 2 Hotty',
                    status: 'authorized',
                    provider: 'Friendly LDAP Provider',
                    type: 'user',
                    activities: [{
                        id: '25fd6vv87-3249-0001-05g6-4d4767890765',
                        description: 'Saved something...',
                        created: new Date().setDate(new Date().getDate() - 1),
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
                }]);
            });
        // return this.http.get('/nifi-registry-api/users/?bucket=' + bucketId)
        //     .map(function (response) {
        //         return response.json() || [];
        //     })
        //     .catch(function (error) {
        //     self.dialogService.openConfirm({
        //         title: 'Error',
        //         message: error._body,
        //         acceptButton: 'Ok',
        //         acceptButtonColor: 'fds-warn'
        //     });
        //     return rxjs.Observable.throw(error._body);
        // });
    },

    //TODO: REST call to API to delete user by id.
    deleteUser: function (id) {
        var self = this;
    },

    //TODO: REST call to API to suspend user by id.
    suspendUser: function (id) {
        var self = this;
    }
};

NfRegistryApi.parameters = [ngHttp.Http, fdsDialogsModule.FdsDialogService];

module.exports = NfRegistryApi;