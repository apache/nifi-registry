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

    // Nifi Registry app folders
    'components': path.resolve(__dirname, 'webapp/components'),
    'services': path.resolve(__dirname, 'webapp/services'),
    'images': path.resolve(__dirname, 'webapp/images'),

    // Nifi Registry app files
    'nf-registry.testbed-factory': path.resolve(__dirname, 'webapp/nf-registry.testbed-factory.js'),
    'nf-registry.animations': path.resolve(__dirname, 'webapp/nf-registry.animations.js'),
    'nf-registry.routes' : path.resolve(__dirname, 'webapp/nf-registry.routes.js'),
    'nf-registry': path.resolve(__dirname, 'webapp/nf-registry.js'),
    'nf-registry.module': path.resolve(__dirname, 'webapp/nf-registry.module.js')
};
