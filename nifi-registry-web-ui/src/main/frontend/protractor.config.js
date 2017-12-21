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

var fs = require('fs');
var path = require('canonical-path');
var _ = require('lodash');

exports.config = {
    directConnect: true,

    // Capabilities to be passed to the webdriver instance.
    capabilities: {
        'browserName': 'chrome'
    },

    // Framework to use. Jasmine is recommended.
    framework: 'jasmine',

    // Spec patterns are relative to this config file
    specs: ['**/*e2e-spec.js'],

    // For angular tests
    useAllAngular2AppRoots: true,

    // Base URL for application server
    baseUrl: 'http://localhost:18080/nifi-registry',

    // See https://github.com/angular/protractor/blob/master/docs/server-setup.md for the various protractor
    // browser driver setup. Here we directly use Chrome or Firefox drivers. If Chrome or Firefox are not
    // available an error will be thrown and a Selenium Server needs to be installed and started.
    directConnect: true,

    //OR

    // The address of a running selenium server.
    // seleniumAddress: 'http://localhost:4444/wd/hub',

    // OR

    // The location of the selenium standalone server .jar file, relative
    // to the location of this config. If no other method of starting selenium
    // is found, this will default to
    // node_modules/protractor/selenium/selenium-server...
    // seleniumServerJar: './node_modules/protractor/selenium/selenium-server-standalone-2.41.0.jar',

    // The port to start the selenium server on, or null if the server should
    // find its own unused port.
    // seleniumPort: 4444,

    onPrepare: function () {
        // debugging
        console.log('browser.params:' + JSON.stringify(browser.params));
        jasmine.getEnv().addReporter(new Reporter(browser.params));

        // Allow changing bootstrap mode to NG1 for upgrade tests
        global.setProtractorToNg1Mode = function () {
            browser.useAllAngular2AppRoots = false;
            browser.rootEl = 'body';
        };
    },

    jasmineNodeOpts: {
        defaultTimeoutInterval: 10000,
        showTiming: true,
        print: function () {
        }
    }
};

// Custom reporter
function Reporter(options) {
    var _defaultOutputFile = path.resolve(process.cwd(), './_test-output', 'protractor-results.txt');
    options.outputFile = options.outputFile || _defaultOutputFile;

    initOutputFile(options.outputFile);
    options.appDir = options.appDir || './';
    var _root = {appDir: options.appDir, suites: []};
    log('AppDir: ' + options.appDir, +1);
    var _currentSuite;

    this.suiteStarted = function (suite) {
        _currentSuite = {description: suite.description, status: null, specs: []};
        _root.suites.push(_currentSuite);
        log('Suite: ' + suite.description, +1);
    };

    this.suiteDone = function (suite) {
        var statuses = _currentSuite.specs.map(function (spec) {
            return spec.status;
        });
        statuses = _.uniq(statuses);
        var status = statuses.indexOf('failed') >= 0 ? 'failed' : statuses.join(', ');
        _currentSuite.status = status;
        log('Suite ' + _currentSuite.status + ': ' + suite.description, -1);
    };

    this.specStarted = function (spec) {

    };

    this.specDone = function (spec) {
        var currentSpec = {
            description: spec.description,
            status: spec.status
        };
        if (spec.failedExpectations.length > 0) {
            currentSpec.failedExpectations = spec.failedExpectations;
        }

        _currentSuite.specs.push(currentSpec);
        log(spec.status + ' - ' + spec.description);
    };

    this.jasmineDone = function () {
        outputFile = options.outputFile;
        //// Alternate approach - just stringify the _root - not as pretty
        //// but might be more useful for automation.
        // var output = JSON.stringify(_root, null, 2);
        var output = formatOutput(_root);
        fs.appendFileSync(outputFile, output);
    };

    function ensureDirectoryExistence(filePath) {
        var dirname = path.dirname(filePath);
        if (directoryExists(dirname)) {
            return true;
        }
        ensureDirectoryExistence(dirname);
        fs.mkdirSync(dirname);
    }

    function directoryExists(path) {
        try {
            return fs.statSync(path).isDirectory();
        }
        catch (err) {
            return false;
        }
    }

    function initOutputFile(outputFile) {
        ensureDirectoryExistence(outputFile);
        var header = "Protractor results for: " + (new Date()).toLocaleString() + "\n\n";
        fs.writeFileSync(outputFile, header);
    }

    // for output file output
    function formatOutput(output) {
        var indent = '  ';
        var pad = '  ';
        var results = [];
        results.push('AppDir:' + output.appDir);
        output.suites.forEach(function (suite) {
            results.push(pad + 'Suite: ' + suite.description + ' -- ' + suite.status);
            pad += indent;
            suite.specs.forEach(function (spec) {
                results.push(pad + spec.status + ' - ' + spec.description);
                if (spec.failedExpectations) {
                    pad += indent;
                    spec.failedExpectations.forEach(function (fe) {
                        results.push(pad + 'message: ' + fe.message);
                    });
                    pad = pad.substr(2);
                }
            });
            pad = pad.substr(2);
            results.push('');
        });
        results.push('');
        return results.join('\n');
    }

    // for console output
    var _pad;

    function log(str, indent) {
        _pad = _pad || '';
        if (indent == -1) {
            _pad = _pad.substr(2);
        }
        console.log(_pad + str);
        if (indent == 1) {
            _pad = _pad + '  ';
        }
    }

}
