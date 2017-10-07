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
            '@angular/core': 'npm:@angular/core/bundles/core.umd.min.js',
            '@angular/common': 'npm:@angular/common/bundles/common.umd.min.js',
            '@angular/platform-browser': 'npm:@angular/platform-browser/bundles/platform-browser.umd.min.js',
            '@angular/platform-browser-dynamic': 'npm:@angular/platform-browser-dynamic/bundles/platform-browser-dynamic.umd.min.js',
            '@angular/http': 'npm:@angular/http/bundles/http.umd.min.js',
            '@angular/router': 'npm:@angular/router/bundles/router.umd.min.js',
            '@angular/forms': 'npm:@angular/forms/bundles/forms.umd.min.js',
            '@angular/flex-layout': 'npm:@angular/flex-layout/bundles/flex-layout.umd.js',
            '@angular/material': 'npm:@angular/material/bundles/material.umd.min.js',
            '@angular/platform-browser/animations': 'npm:@angular/platform-browser/bundles/platform-browser-animations.umd.min.js',
            '@angular/cdk': 'npm:@angular/cdk/bundles/cdk.umd.min.js',
            '@angular/animations': 'npm:@angular/animations/bundles/animations.umd.min.js',
            '@angular/animations/browser': 'npm:@angular/animations/bundles/animations-browser.umd.min.js',
            '@angular/compiler': 'npm:@angular/compiler/bundles/compiler.umd.min.js',

            // Covalent
            '@covalent/core': 'npm:@covalent/core/core.umd.js',

            // other libraries
            'rxjs': 'npm:rxjs',
            'moment': 'npm:moment',
            'angular2-moment': 'npm:angular2-moment',
            'switchMap': 'npm:rxjs/add/operator/switchMap',
            'zone.js': 'npm:zone.js/dist/zone.js',
            'core-js': 'npm:core-js/client/shim.min.js',
            'superagent': 'npm:superagent/superagent.js',
            'querystring': 'npm:querystring',

            // Fluid Design System
            '@fluid-design-system/core': 'npm:@fluid-design-system/dist/platform/core/fluid-design-system.module.js',
            '@fluid-design-system/dialogs': 'npm:@fluid-design-system/dist/platform/core/dialogs/fds-dialogs.module.js',
            '@fluid-design-system/dialog-component': 'npm:@fluid-design-system/dist/platform/core/dialogs/fds-dialog.component.js',
            '@fluid-design-system/dialog-service': 'npm:@fluid-design-system/dist/platform/core/dialogs/services/dialog.service.js',
            '@fluid-design-system/confirm-dialog-component': 'npm:@fluid-design-system/dist/platform/core/dialogs/confirm-dialog/confirm-dialog.component.js',
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
