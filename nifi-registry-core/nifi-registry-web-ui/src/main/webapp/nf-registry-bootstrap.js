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

require('core-js');
require('zone.js');
require('hammerjs');
require('switchMap');
// patch Observable with appropriate methods
require('rxjs/add/operator/map');
require('rxjs/add/operator/catch');
require('rxjs/add/observable/of');
require('rxjs/add/observable/forkJoin');
var $ = require('jquery');
var NfRegistryModule = require('nifi-registry/nf-registry.module.js');
var ngPlatformBrowserDynamic = require('@angular/platform-browser-dynamic');
var ngCore = require('@angular/core');

// Comment out this line when developing to assert for unidirectional data flow
ngCore.enableProdMode();

// Get the locale id from the global
var locale = navigator.language;

var providers = [];

// No locale or U.S. English: no translation providers so go ahead and bootstrap the app
if (!locale || locale === 'en-US') {
    ngPlatformBrowserDynamic.platformBrowserDynamic().bootstrapModule(NfRegistryModule, {providers: providers});
} else { //load the translation providers and bootstrap the module
    var translationFile = './nifi-registry/messages.' + locale + '.xlf';

    $.ajax({
        url: translationFile
    }).done(function (translations) {
        // add providers if translation file for locale is loaded
        if (translations) {
            providers.push({provide: ngCore.TRANSLATIONS, useValue: translations});
            providers.push({provide: ngCore.TRANSLATIONS_FORMAT, useValue: 'xlf'});
            providers.push({provide: ngCore.LOCALE_ID, useValue: locale});
        }
        ngPlatformBrowserDynamic.platformBrowserDynamic().bootstrapModule(NfRegistryModule, {providers: providers});
    }).fail(function () {
        ngPlatformBrowserDynamic.platformBrowserDynamic().bootstrapModule(NfRegistryModule, {providers: providers});
    });
}