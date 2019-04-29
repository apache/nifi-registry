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
const webpackConfig = require('./webpack.prod');

module.exports = function (grunt) {
    // load all grunt tasks matching the ['grunt-*', '@*/grunt-*'] patterns
    require('load-grunt-tasks')(grunt);

    grunt.initConfig({
        watch: {
            theme: {
                files: [
                    'webapp/theming/**/*.scss'
                ],
                tasks: ['compile-web-ui-styles']
            },
            webapp: {
                files: [
                    'webapp/**/*.js',
                    'webapp/**/*.ts',
                    'webapp/**/*.html'
                ],
                tasks: ['dev-bundle-web-ui']
            }
        },
        webpack: {
            prod: Object.assign({
                mode: 'production',
                devtool: 'source-map'
            }, webpackConfig),
            dev: Object.assign({}, webpackConfig)
        },
        browserSync: {
            bsFiles: {
                src: [
                    'nf-registry.bundle.min.js'
                ]
            },
            options: {
                port: 8080,
                watchTask: true,
                server: {
                    baseDir: './'
                }
            }
        }
    });

    grunt.registerTask('dev-bundle-web-ui', ['webpack:dev']);
    grunt.registerTask('prod-bundle-web-ui', ['webpack:prod']);
    grunt.registerTask('default', ['dev-bundle-web-ui', 'browserSync', 'watch']);
};
