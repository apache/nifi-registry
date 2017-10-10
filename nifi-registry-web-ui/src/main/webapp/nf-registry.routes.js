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

var ngRouter = require('@angular/router');
var FdsDemo = require('nifi-registry/components/fluid-design-system/fds-demo.js');
var NfPageNotFoundComponent = require('nifi-registry/components/page-not-found/nf-registry-page-not-found.js');
var NfRegistryExplorer = require('nifi-registry/components/explorer/nf-registry-explorer.js');
var NfRegistryAdministration = require('nifi-registry/components/administration/nf-registry-administration.js');
var NfRegistryUsersAdministration = require('nifi-registry/components/administration/users/nf-registry-users-administration.js');
var NfRegistryAddUser = require('nifi-registry/components/administration/users/add/nf-registry-add-user.js');
var NfRegistryUserDetails = require('nifi-registry/components/administration/users/details/nf-registry-user-details.js');
var NfRegistryUserPermissions = require('nifi-registry/components/administration/users/permissions/nf-registry-user-permissions.js');
var NfRegistryBucketPermissions = require('nifi-registry/components/administration/workflow/buckets/permissions/nf-registry-bucket-permissions.js');
var NfRegistryWorkflowAdministration = require('nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js');
var NfRegistryGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js');
var NfRegistryBucketGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js');
var NfRegistryDropletGridListViewer = require('nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js');

var NfRegistryRoutes = new ngRouter.RouterModule.forRoot([{
    path: 'nifi-registry/explorer',
    component: NfRegistryExplorer,
    children: [
        {
            path: 'grid-list',
            component: NfRegistryGridListViewer
        }, {
            path: 'grid-list/buckets/:bucketId',
            component: NfRegistryBucketGridListViewer
        },
        {
            path: 'grid-list/buckets/:bucketId/:dropletType/:dropletId',
            component: NfRegistryDropletGridListViewer
        }
        ]
    // canActivate: [AuthGuard] //TODO: https://angular.io/api/router/CanActivate https://scotch.io/tutorials/routing-angular-2-single-page-apps-with-the-component-router
}, {
    path: 'nifi-registry/fluid-design-system',
    component: FdsDemo
}, {
    path: 'nifi-registry/administration',
    component: NfRegistryAdministration,
    children: [{
        path: '',
        redirectTo: 'users',
        pathMatch: 'full'
    }, {
        path: 'users',
        component: NfRegistryUsersAdministration
    }, {
        path: 'workflow',
        component: NfRegistryWorkflowAdministration
    }]
}, {
    path: 'nifi-registry/explorer/grid-list/buckets',
    redirectTo: '/nifi-registry/explorer/grid-list',
    pathMatch: 'full'
}, {
    path: 'nifi-registry',
    redirectTo: '/nifi-registry/explorer/grid-list',
    pathMatch: 'full'
}, {
    path: '',
    redirectTo: '/nifi-registry/explorer/grid-list',
    pathMatch: 'full'
}, {
    path: '**',
    component: NfPageNotFoundComponent
}, {
    path: 'user/details/:userId',
    component: NfRegistryUserDetails,
    outlet: 'sidenav'
}, {
    path: 'user/permissions/:userId',
    component: NfRegistryUserPermissions,
    outlet: 'sidenav'
}, {
    path: 'user/add',
    component: NfRegistryAddUser,
    outlet: 'sidenav'
}, {
    path: 'bucket/permissions/:bucketId',
    component: NfRegistryBucketPermissions,
    outlet: 'sidenav'
}]);

module.exports = NfRegistryRoutes;