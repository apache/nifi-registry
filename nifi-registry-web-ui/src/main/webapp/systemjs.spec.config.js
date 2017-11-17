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

(function (global) {
    System.config({
        paths: {
            // paths serve as alias
            'npm:': 'nifi-registry/node_modules/'
        },
        // map tells the System loader where to look for things
        map: {
            'text': 'npm:systemjs-plugin-text/text.js',
            'app': './webapp',

            // jquery
            'jquery': 'npm:jquery/dist/jquery.min.js',

            // Angular
            '@angular/core': 'npm:@angular/core/bundles/core.umd.js',
            '@angular/common': 'npm:@angular/common/bundles/common.umd.js',
            '@angular/common/http': 'npm:@angular/common/bundles/common-http.umd.js',
            '@angular/platform-browser': 'npm:@angular/platform-browser/bundles/platform-browser.umd.js',
            '@angular/platform-browser-dynamic': 'npm:@angular/platform-browser-dynamic/bundles/platform-browser-dynamic.umd.js',
            '@angular/http': 'npm:@angular/http/bundles/http.umd.js',
            '@angular/router': 'npm:@angular/router/bundles/router.umd.js',
            '@angular/forms': 'npm:@angular/forms/bundles/forms.umd.js',
            '@angular/flex-layout': 'npm:@angular/flex-layout/bundles/flex-layout.umd.js',
            '@angular/material': 'npm:@angular/material/bundles/material.umd.js',
            '@angular/platform-browser/animations': 'npm:@angular/platform-browser/bundles/platform-browser-animations.umd.js',
            '@angular/cdk': 'npm:@angular/cdk/bundles/cdk.umd.js',
            '@angular/cdk/a11y': 'npm:@angular/cdk/bundles/cdk-a11y.umd.js',
            '@angular/cdk/collections': 'npm:@angular/cdk/bundles/cdk-collections.umd.js',
            '@angular/cdk/observers': 'npm:@angular/cdk/bundles/cdk-observers.umd.js',
            '@angular/cdk/overlay': 'npm:@angular/cdk/bundles/cdk-overlay.umd.js',
            '@angular/cdk/platform': 'npm:@angular/cdk/bundles/cdk-platform.umd.js',
            '@angular/cdk/portal': 'npm:@angular/cdk/bundles/cdk-portal.umd.js',
            '@angular/cdk/keycodes': 'npm:@angular/cdk/bundles/cdk-keycodes.umd.js',
            '@angular/cdk/bidi': 'npm:@angular/cdk/bundles/cdk-bidi.umd.js',
            '@angular/cdk/coercion': 'npm:@angular/cdk/bundles/cdk-coercion.umd.js',
            '@angular/cdk/table': 'npm:@angular/cdk/bundles/cdk-table.umd.js',
            '@angular/cdk/rxjs': 'npm:@angular/cdk/bundles/cdk-rxjs.umd.js',
            '@angular/cdk/scrolling': 'npm:@angular/cdk/bundles/cdk-scrolling.umd.js',
            '@angular/cdk/stepper': 'npm:@angular/cdk/bundles/cdk-stepper.umd.js',
            '@angular/animations': 'npm:@angular/animations/bundles/animations.umd.js',
            '@angular/animations/browser': 'npm:@angular/animations/bundles/animations-browser.umd.js',
            '@angular/compiler': 'npm:@angular/compiler/bundles/compiler.umd.js',

            // needed to support gestures for angular material
            'hammerjs': 'npm:hammerjs/hammer.min.js',

            // Covalent
            '@covalent/core': 'npm:@covalent/core/bundles/core.umd.min.js',

            // other libraries
            'rxjs': 'npm:rxjs',
            'moment': 'npm:moment',
            'angular2-moment': 'npm:angular2-moment',
            'switchMap': 'npm:rxjs/add/operator/switchMap',
            'zone.js': 'npm:zone.js/dist/zone.js',
            'core-js': 'npm:core-js/client/shim.min.js',
            'superagent': 'npm:superagent/superagent.js',
            'querystring': 'npm:querystring',
            'tslib': 'npm:tslib/tslib.js',

            // Fluid Design System
            '@fluid-design-system/core': 'npm:@fluid-design-system/dist/platform/core/fluid-design-system.module.js',
            '@fluid-design-system/dialogs': 'npm:@fluid-design-system/dist/platform/core/dialogs/fds-dialogs.module.js',
            '@fluid-design-system/dialog-component': 'npm:@fluid-design-system/dist/platform/core/dialogs/fds-dialog.component.js',
            '@fluid-design-system/dialog-service': 'npm:@fluid-design-system/dist/platform/core/dialogs/services/dialog.service.js',
            '@fluid-design-system/confirm-dialog-component': 'npm:@fluid-design-system/dist/platform/core/dialogs/confirm-dialog/confirm-dialog.component.js',
            '@fluid-design-system/snackbars': 'npm:@fluid-design-system/dist/platform/core/snackbars/fds-snackbars.module.js',
            '@fluid-design-system/snackbar-component': 'npm:@fluid-design-system/dist/platform/core/snackbars/fds-snackbar.component.js',
            '@fluid-design-system/snackbar-service': 'npm:@fluid-design-system/dist/platform/core/snackbars/services/snackbar.service.js',
            '@fluid-design-system/coaster-component': 'npm:@fluid-design-system/dist/platform/core/snackbars/coaster/coaster.component.js'
        },
        // packages tells the System loader how to load when no filename and/or no extension
        packages: {
            app: {
                defaultExtension: 'js',
                meta: {
                    './*.js': {
                        loader: 'nifi-registry/systemjs-angular-loader.js'
                    }
                }
            },
            'nifi-registry/systemjs-angular-loader.js': {
                loader: false
            },
            'rxjs': {
                defaultExtension: 'js'
            },
            'querystring': {
                main: './index.js',
                defaultExtension: 'js'
            },
            'moment': {
                main: './moment.js',
                defaultExtension: 'js'
            },
            'angular2-moment': {
                main: './index.js',
                defaultExtension: 'js'
            }
        }
    });
})(this);
