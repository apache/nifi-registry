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
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

module.exports = {
    ts: {
        test: /\.tsx?$/,
        include: [
            path.resolve(__dirname, 'webapp')
        ],
        use: [
            {
                loader: 'cache-loader'
            },
            {
                loader: 'ts-loader'
            }
        ]
    },

    nifiFds: {
        /*
        * Send all js files from @nifi-fds through a custom loader that replaces its usage of inline systemjs text loading
        * of html files like:
        *     require('./confirm-dialog.component.html!text')
        *
        * with normal require calls that are subsequently loaded via webpack's html-loader like:
        *     require('./confirm-dialog.component.html')
        */
        test: /\.js$/,
        include: [
            path.resolve(__dirname, 'node_modules/@nifi-fds/core')
        ],
        use: [
            {
                loader: 'cache-loader'
            },
            {
                loader: path.resolve(__dirname, 'systemjs-text-to-html-loader')
            }
        ]
    },

    js: {
        test: /\.js$/,
        include: [
            path.resolve(__dirname, 'webapp')
        ],
        use: [
            {
                loader: 'cache-loader'
            },
            {
                loader: 'babel-loader',
                options: {
                    presets: ['@babel/preset-env']
                }
            }
        ]
    },

    html: {
        test: /\.html$/,
        include: [
            path.resolve(__dirname, 'node_modules/@nifi-fds/core'),
            path.resolve(__dirname, 'webapp')
        ],
        use: [
            {loader: 'cache-loader'},
            {
                loader: 'html-loader',
                options: {
                    attrs: false
                }
            }
        ]
    },

    scss: {
        test: /\.scss$/,
        use: [
            {
                // Create CSS files separately
                loader: MiniCssExtractPlugin.loader
            },
            {
                // Translate CSS into CommonJS
                loader: 'css-loader',
                options: {
                    url: false
                }
            },
            {
                // Compile Sass to CSS
                loader: 'sass-loader'
            }
        ]
    },

    images: {
        test: /\.(svg|png)$/i,
        include: [
            path.resolve(__dirname, 'webapp')
        ],
        use: [{
            loader: 'file-loader',
            options: {
                name: '[name].[ext]',
                outputPath: 'assets/images/'
            }
        }]
    },

    fonts: {
        test: /\.(woff(2)?|ttf|eot)(\?v=\d+\.\d+\.\d+)?$/,
        use: [{
            loader: 'file-loader',
            options: {
                name: '[name].[ext]',
                outputPath: 'assets/fonts/'
            }
        }]
    },

    xlf: {
        test: /\.(xlf)$/i,
        include: [
            path.resolve(__dirname, 'locale')
        ],
        use: [{
            loader: 'file-loader',
            options: {
                name: '[name].[ext]',
                outputPath: 'assets/locale/'
            }
        }]
    },

    ignoreScss: {
        test: /\.scss$/,
        use: 'null-loader'
    }
}
