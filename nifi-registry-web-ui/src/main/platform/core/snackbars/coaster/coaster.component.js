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
var $ = require('jquery');

/**
 * FdsCoasterComponent constructor.
 *
 * @constructor
 */
function FdsCoasterComponent() {
    this.title = '';
    this.message = '';
    this.icon = '';
    this.color = '';
    this.snackBarRef = undefined;
    this.viewContainerRef = undefined;
};

FdsCoasterComponent.prototype = {
    constructor: FdsCoasterComponent,

    /**
     * Initialize the component.
     */
    ngAfterViewChecked: function () {
        $('.fds-snackbar-wrapper').css('border-color', this.color);
        $('.fds-snackbar-title').css('color', this.color);
        $('.fds-coaster-icon').css('color', this.color);

        if (this.icon) {
            $('.fds-snackbar-wrapper').css('padding', '15px 15px 15px 45px');
        } else {
            $('.fds-snackbar-wrapper').css('padding', '15px 15px 15px 15px');
        }
    },

    /**
     * Close the snackbar and send a cancel response to any subscribers.
     */
    cancel: function () {
        this.snackBarRef.dismiss(false);
    }
};

FdsCoasterComponent.annotations = [
    new ngCore.Component({
        selector: 'fds-coaster',
        template: require('./coaster.component.html!text')
    })
];

FdsCoasterComponent.parameters = [];

module.exports = FdsCoasterComponent;
