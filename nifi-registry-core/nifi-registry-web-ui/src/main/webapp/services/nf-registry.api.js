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

var NfStorage = require('nifi-registry/services/nf-storage.service.js');
var ngCommonHttp = require('@angular/common/http');
var fdsDialogsModule = require('@flow-design-system/dialogs');
var rxjs = require('rxjs/Observable');
var MILLIS_PER_SECOND = 1000;
var headers = new Headers({'Content-Type': 'application/json'});

var config = {
    urls: {
        currentUser: '/nifi-registry-api/access',
        kerberos: '/nifi-registry-api/access/token/kerberos'
    }
};

/**
 * NfRegistryApi constructor.
 *
 * @param nfStorage             A wrapper for the browser's local storage.
 * @param http                  The angular http module.
 * @param fdsDialogService      The FDS dialog service.
 * @constructor
 */
function NfRegistryApi(nfStorage, http, fdsDialogService) {
    this.nfStorage = nfStorage;
    this.http = http;
    this.dialogService = fdsDialogService;
};

NfRegistryApi.prototype = {
    constructor: NfRegistryApi,

    /**
     * Retrieves the snapshot metadata for an existing droplet the registry has stored.
     *
     * @param {string}  dropletUri     The uri of the droplet to request.
     * @returns {*}
     */
    getDropletSnapshotMetadata: function (dropletUri) {
        var self = this;
        var url = '/nifi-registry-api/' + dropletUri;
        url += '/versions';
        return this.http.get(url)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Retrieves the given droplet with or without snapshot metadata.
     *
     * @param {string}  bucketId        The id of the bucket to request.
     * @param {string}  dropletType     The type of the droplet to request.
     * @param {string}  dropletId       The id of the droplet to request.
     * @returns {*}
     */
    getDroplet: function (bucketId, dropletType, dropletId) {
        var self = this;
        var url = '/nifi-registry-api/buckets/' + bucketId + '/' + dropletType + '/' + dropletId;
        return this.http.get(url)
            .map(function (response) {
                return response || {};
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Flow Not Found',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
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
                return response || [];
            })
            .catch(function (error) {
                return rxjs.Observable.of(error);
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
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
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
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
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
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Get metadata for an existing bucket in the registry.
     *
     * @param {string} bucketId     The identifier of the bucket to retrieve.
     * @returns {*}
     */
    getBucket: function (bucketId) {
        var self = this;
        var url = '/nifi-registry-api/buckets/' + bucketId;
        return this.http.get(url)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Bucket Not Found',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
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
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Buckets Not Found',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Updates a bucket.
     *
     * @param {string} identifier           The identifier of the bucket.
     * @param {string} name                 The name of the bucket.
     * @param {string} allowBundleRedeploy  Whether or not the bucket allows redeploying released bundles
     * @returns {*}
     */
    updateBucket: function (updatedBucket) {
        var self = this;
        return this.http.put('/nifi-registry-api/buckets/' + updatedBucket.identifier, updatedBucket, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Get user by id.
     *
     * @param userId
     * @returns {*}
     */
    getUser: function (userId) {
        var self = this;
        return this.http.get('/nifi-registry-api/tenants/users/' + userId)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'User Not Found',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Creates a user.
     *
     * @param {string} identity     The identity of the user.
     * @returns {*}
     */
    addUser: function (identity) {
        var self = this;
        return this.http.post('/nifi-registry-api/tenants/users', {
            identity: identity,
            resourcePermissions: {
                anyTopLevelResource: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                buckets: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                tenants: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                policies: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                },
                proxy: {
                    canRead: false,
                    canWrite: false,
                    canDelete: false
                }
            }
        }, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Updates a user.
     *
     * @param {string} identifier   The identifier of the user.
     * @param {string} identity     The identity of the user.
     * @returns {*}
     */
    updateUser: function (identifier, identity) {
        var self = this;
        return this.http.put('/nifi-registry-api/tenants/users/' + identifier, {
            'identifier': identifier,
            'identity': identity
        }, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Gets all users.
     *
     * @returns {*}
     */
    getUsers: function () {
        var self = this;
        return this.http.get('/nifi-registry-api/tenants/users')
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Users Not Found',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Delete an existing user from the registry.
     *
     * @param {string} userId     The identifier of the user to be deleted.
     * @returns {*}
     */
    deleteUser: function (userId) {
        var self = this;
        return this.http.delete('/nifi-registry-api/tenants/users/' + userId, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Gets all user groups.
     *
     * @returns {*}
     */
    getUserGroups: function () {
        var self = this;
        return this.http.get('/nifi-registry-api/tenants/user-groups')
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Groups Not Found',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Get user group.
     *
     * @param {string} groupId  The id of the group to retrieve.
     * @returns {*}
     */
    getUserGroup: function (groupId) {
        var self = this;
        return this.http.get('/nifi-registry-api/tenants/user-groups/' + groupId)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Group Not Found',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Delete an existing user group from the registry.
     *
     * @param {string} userGroupId     The identifier of the user group to be deleted.
     * @returns {*}
     */
    deleteUserGroup: function (userGroupId) {
        var self = this;
        return this.http.delete('/nifi-registry-api/tenants/user-groups/' + userGroupId, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Creates a new group.
     *
     * @param {string}  identifier      The identifier of the user.
     * @param {string}  identity        The identity of the user.
     * @param {array}   users           The array of users to be added to the new group.
     * @returns {*}
     */
    createNewGroup: function (identifier, identity, users) {
        var self = this;
        return this.http.post('/nifi-registry-api/tenants/user-groups', {
            'identifier': identifier,
            'identity': identity,
            'users': users
        }, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Updates a group.
     *
     * @param {string}  identifier   The identifier of the group.
     * @param {string}  identity     The identity of the group.
     * @param {array}   users         The array of users in the new group.
     * @returns {*}
     */
    updateUserGroup: function (identifier, identity, users) {
        var self = this;
        return this.http.put('/nifi-registry-api/tenants/user-groups/' + identifier, {
            'identifier': identifier,
            'identity': identity,
            'users': users
        }, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Get metadata for all policies in the registry for which the client is authorized.
     *
     * @returns {*}
     */
    getPolicies: function () {
        var self = this;
        var url = '/nifi-registry-api/policies';
        return this.http.get(url)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Get resource policies by resource identifier.
     *
     * @param {string} action       The name of the resource action (e.g. READ/WRITE/DELETE).
     * @param {string} resource     The name of the resource (e.g. /buckets, /tenants, /policies, /proxy).
     * @param {string} resourceId   The resource identifier.
     * @returns {*}
     */
    getResourcePoliciesById: function (action, resource, resourceId) {
        var self = this;
        return this.http.get('/nifi-registry-api/policies/' + action + resource + '/' + resourceId)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Get policy action resource.
     *
     * @param {string} action   The name of the resource action (e.g. READ/WRITE/DELETE).
     * @param {string} resource     The name of the resource (e.g. /buckets, /tenants, /policies, /proxy).
     * @returns {*}
     */
    getPolicyActionResource: function (action, resource) {
        var self = this;
        return this.http.get('/nifi-registry-api/policies/' + action + resource)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Update policy action resource.
     *
     * @param {string} identifier   The identifier of the group.
     * @param {string} action       The name of the resource action (e.g. READ/WRITE/DELETE).
     * @param {string} resource     The name of the resource (e.g. /buckets, /tenants, /policies, /proxy).
     * @param {string} users        The users with resource privileges.
     * @param {string} userGroups   The user groups with resource privileges.
     * @returns {*}
     */
    putPolicyActionResource: function (identifier, action, resource, users, userGroups) {
        var self = this;
        return this.http.put('/nifi-registry-api/policies/' + identifier, {
            'identifier': identifier,
            'resource': resource,
            'action': action,
            'users': users,
            'userGroups': userGroups
        }, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Creates a policy action resource.
     *
     * @param {string} action       The name of the resource action (e.g. READ/WRITE/DELETE).
     * @param {string} resource     The name of the resource (e.g. /buckets, /tenants, /policies, /proxy).
     * @param {string} users        The users with resource privileges.
     * @param {string} userGroups   The user groups with resource privileges.
     * @returns {*}
     */
    postPolicyActionResource: function (action, resource, users, userGroups) {
        var self = this;
        return this.http.post('/nifi-registry-api/policies', {
            'resource': resource,
            'action': action,
            'users': users,
            'userGroups': userGroups
        }, headers)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: error.error,
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of(error);
            });
    },

    /**
     * Authenticate a user.
     *
     * @param {string} username     The name of the user to authenticate.
     * @param {string} password     The user password.
     * @returns {*}
     */
    postToLogin: function (username, password) {
        var self = this;

        var encodedCredentials = btoa(username + ":" + password);
        var headers = new ngCommonHttp.HttpHeaders({
            'Authorization': 'Basic ' + encodedCredentials
        });

        var options = {
            headers: headers,
            withCredentials: true,
            responseType: 'text'
        };
        return this.http.post('/nifi-registry-api/access/token/login', null, options)
            .map(function (jwt) {
                // get the payload and store the token with the appropriate expiration
                var token = self.nfStorage.getJwtPayload(jwt);
                if (token) {
                    var expiration = parseInt(token['exp'], 10) * MILLIS_PER_SECOND;
                    self.nfStorage.setItem('jwt', jwt, expiration);
                }
                return jwt;
            })
            .catch(function (error) {
                self.dialogService.openConfirm({
                    title: 'Error',
                    message: 'Please contact your System Administrator.',
                    acceptButton: 'Ok',
                    acceptButtonColor: 'fds-warn'
                });
                return rxjs.Observable.of('');
            });
    },

    /**
     * Kerberos ticket exchange.
     *
     * @returns {*}
     */
    ticketExchange: function () {
        var self = this;
        if (this.nfStorage.hasItem('jwt')) {
            return rxjs.Observable.of(self.nfStorage.getItem('jwt'));
        } else {
            return this.http.post(config.urls.kerberos, null, {responseType: 'text'})
                .map(function (jwt) {
                    // get the payload and store the token with the appropriate expiration
                    var token = self.nfStorage.getJwtPayload(jwt);
                    if (token) {
                        var expiration = parseInt(token['exp'], 10) * MILLIS_PER_SECOND;
                        self.nfStorage.setItem('jwt', jwt, expiration);
                    }
                    return jwt;
                })
                .catch(function (error) {
                    return rxjs.Observable.of('');
                });
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
        return this.http.get(config.urls.currentUser)
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                return rxjs.Observable.of({
                    error: error,
                    resourcePermissions: {
                        anyTopLevelResource: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        buckets: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        tenants: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        policies: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        },
                        proxy: {
                            canRead: false,
                            canWrite: false,
                            canDelete: false
                        }
                    }
                });
            });
    },

    /**
     * Get NiFi Registry configuration resource.
     *
     * @returns {*}
     */
    getRegistryConfig: function (action, resource) {
        var self = this;
        return this.http.get('/nifi-registry-api/config')
            .map(function (response) {
                return response;
            })
            .catch(function (error) {
                // If failed, return an empty object.
                return rxjs.Observable.of({});
            });
    }

};

NfRegistryApi.parameters = [
    NfStorage,
    ngCommonHttp.HttpClient,
    fdsDialogsModule.FdsDialogService
];

module.exports = NfRegistryApi;
