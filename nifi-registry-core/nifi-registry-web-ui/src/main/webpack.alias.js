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

const path = require('path');

module.exports = {
    // Flow Design System
    '@flow-design-system/core': path.resolve(__dirname, 'node_modules/@nifi-fds/core/flow-design-system.module.js'),
    '@flow-design-system/dialogs': path.resolve(__dirname, 'node_modules/@nifi-fds/core/dialogs/fds-dialogs.module.js'),
    '@flow-design-system/dialog-component': path.resolve(__dirname, 'node_modules/@nifi-fds/core/dialogs/fds-dialog.component.js'),
    '@flow-design-system/dialog-service': path.resolve(__dirname, 'node_modules/@nifi-fds/core/dialogs/services/dialog.service.js'),
    '@flow-design-system/confirm-dialog-component': path.resolve(__dirname, 'node_modules/@nifi-fds/core/dialogs/confirm-dialog/confirm-dialog.component.js'),
    '@flow-design-system/snackbars': path.resolve(__dirname, 'node_modules/@nifi-fds/core/snackbars/fds-snackbars.module.js'),
    '@flow-design-system/snackbar-component': path.resolve(__dirname, 'node_modules/@nifi-fds/core/snackbars/fds-snackbar.component.js'),
    '@flow-design-system/snackbar-service': path.resolve(__dirname, 'node_modules/@nifi-fds/core/snackbars/services/snackbar.service.js'),
    '@flow-design-system/coaster-component': path.resolve(__dirname, 'node_modules/@nifi-fds/core/snackbars/coaster/coaster.component.js'),
    '@flow-design-system/common/storage-service': path.resolve(__dirname, 'node_modules/@nifi-fds/core/common/services/fds-storage.service.js'),

    'switchMap': path.resolve(__dirname, 'node_modules/rxjs/add/operator/switchMap.js'),

    // Nifi Registry
    'nifi-registry/nf-registry.module.js': path.resolve(__dirname, 'webapp/nf-registry.module.js'),
    'nifi-registry/nf-registry.animations.js': path.resolve(__dirname, 'webapp/nf-registry.animations.js'),
    'nifi-registry/nf-registry.routes.js': path.resolve(__dirname, 'webapp/nf-registry.routes.js'),
    'nifi-registry/nf-registry.js': path.resolve(__dirname, 'webapp/nf-registry.js'),
    'nifi-registry/services/nf-registry.api.js': path.resolve(__dirname, 'webapp/services/nf-registry.api.js'),
    'nifi-registry/services/nf-registry.service.js': path.resolve(__dirname, 'webapp/services/nf-registry.service.js'),
    'nifi-registry/services/nf-storage.service.js': path.resolve(__dirname, 'webapp/services/nf-storage.service.js'),
    'nifi-registry/services/nf-registry.auth-guard.service.js': path.resolve(__dirname, 'webapp/services/nf-registry.auth-guard.service.js'),
    'nifi-registry/services/nf-registry.token.interceptor.js': path.resolve(__dirname, 'webapp/services/nf-registry.token.interceptor.js'),
    'nifi-registry/components/page-not-found/nf-registry-page-not-found.js': path.resolve(__dirname, 'webapp/components/page-not-found/nf-registry-page-not-found.js'),
    'nifi-registry/components/login/nf-registry-login.js': path.resolve(__dirname, 'webapp/components/login/nf-registry-login.js'),
    'nifi-registry/components/login/dialogs/nf-registry-user-login.js': path.resolve(__dirname, 'webapp/components/login/dialogs/nf-registry-user-login.js'),
    'nifi-registry/components/explorer/nf-registry-explorer.js': path.resolve(__dirname, 'webapp/components/explorer/nf-registry-explorer.js'),
    'nifi-registry/components/administration/nf-registry-administration.js': path.resolve(__dirname, 'webapp/components/administration/nf-registry-administration.js'),
    'nifi-registry/components/administration/users/nf-registry-users-administration.js': path.resolve(__dirname, 'webapp/components/administration/users/nf-registry-users-administration.js'),
    'nifi-registry/components/administration/users/dialogs/add-user/nf-registry-add-user.js': path.resolve(__dirname, 'webapp/components/administration/users/dialogs/add-user/nf-registry-add-user.js'),
    'nifi-registry/components/administration/users/dialogs/create-new-group/nf-registry-create-new-group.js': path.resolve(__dirname, 'webapp/components/administration/users/dialogs/create-new-group/nf-registry-create-new-group.js'),
    'nifi-registry/components/administration/users/dialogs/add-users-to-group/nf-registry-add-users-to-group.js': path.resolve(__dirname, 'webapp/components/administration/users/dialogs/add-users-to-group/nf-registry-add-users-to-group.js'),
    'nifi-registry/components/administration/users/dialogs/add-user-to-groups/nf-registry-add-user-to-groups.js': path.resolve(__dirname, 'webapp/components/administration/users/dialogs/add-user-to-groups/nf-registry-add-user-to-groups.js'),
    'nifi-registry/components/administration/users/sidenav/manage-user/nf-registry-manage-user.js': path.resolve(__dirname, 'webapp/components/administration/users/sidenav/manage-user/nf-registry-manage-user.js'),
    'nifi-registry/components/administration/users/sidenav/manage-group/nf-registry-manage-group.js': path.resolve(__dirname, 'webapp/components/administration/users/sidenav/manage-group/nf-registry-manage-group.js'),
    'nifi-registry/components/administration/workflow/dialogs/create-bucket/nf-registry-create-bucket.js': path.resolve(__dirname, 'webapp/components/administration/workflow/dialogs/create-bucket/nf-registry-create-bucket.js'),
    'nifi-registry/components/administration/workflow/dialogs/edit-bucket-policy/nf-registry-edit-bucket-policy.js': path.resolve(__dirname, 'webapp/components/administration/workflow/dialogs/edit-bucket-policy/nf-registry-edit-bucket-policy.js'),
    'nifi-registry/components/administration/workflow/dialogs/add-policy-to-bucket/nf-registry-add-policy-to-bucket.js': path.resolve(__dirname, 'webapp/components/administration/workflow/dialogs/add-policy-to-bucket/nf-registry-add-policy-to-bucket.js'),
    'nifi-registry/components/administration/workflow/sidenav/manage-bucket/nf-registry-manage-bucket.js': path.resolve(__dirname, 'webapp/components/administration/workflow/sidenav/manage-bucket/nf-registry-manage-bucket.js'),
    'nifi-registry/components/administration/workflow/nf-registry-workflow-administration.js': path.resolve(__dirname, 'webapp/components/administration/workflow/nf-registry-workflow-administration.js'),
    'nifi-registry/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js': path.resolve(__dirname, 'webapp/components/explorer/grid-list/registry/nf-registry-grid-list-viewer.js'),
    'nifi-registry/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js': path.resolve(__dirname, 'webapp/components/explorer/grid-list/registry/nf-registry-bucket-grid-list-viewer.js'),
    'nifi-registry/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js': path.resolve(__dirname, 'webapp/components/explorer/grid-list/registry/nf-registry-droplet-grid-list-viewer.js'),

    'nf-registry.testbed-factory': path.resolve(__dirname, 'webapp/nf-registry.testbed-factory.js'),

    'components': path.resolve(__dirname, 'webapp/components'),
    'services': path.resolve(__dirname, 'webapp/services'),
};
