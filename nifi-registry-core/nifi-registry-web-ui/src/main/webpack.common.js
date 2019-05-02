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

const webpack = require('webpack');
const path = require('path');
const FixStyleOnlyEntriesPlugin = require('webpack-fix-style-only-entries');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

const webpackAlias = require('./webpack.alias');

module.exports = {
    // Deployment target
    target: 'web',

    // Starting point of building the bundles
    entry: {
        // JS files
        'nf-registry.bundle.min': path.resolve(__dirname, 'webapp/nf-registry-bootstrap.js'),

        // SCSS files
        'nf-registry.style.min': [
            path.resolve(__dirname, 'webapp/theming/nf-registry.scss'),
        ]
    },

    // Output bundles
    output: {
        // add the content hash for auto cache-busting
        filename: '[name].[contenthash].js',
        path: path.resolve(__dirname, './'),
        publicPath: 'nifi-registry/'
    },

    optimization: {
        splitChunks: {
            cacheGroups: {
                vendor: {
                    chunks: 'initial',
                    test: path.resolve(__dirname, 'node_modules'),
                    name: 'vendor.min',
                    enforce: true
                }
            }
        }
    },

    // Change how modules are resolved
    resolve: {
        extensions: ['.ts', '.tsx', '.js'],
        alias: webpackAlias
    },

    // Polyfill or mock certain Node.js globals and modules
    node: {
        console: true
    },

    module: {
        rules: [
            {
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
            {
                test: /\.js$/,
                include: [
                    path.resolve(__dirname, 'webapp')
                ],
                use: [
                    {
                        loader: 'cache-loader'
                    },
                    {
                        loader: path.resolve(__dirname, 'angular-url-loader')
                    },
                    {
                        loader: path.resolve(__dirname, 'systemjs-text-to-html-loader')
                    },
                    {
                        loader: 'babel-loader',
                        options: {
                            presets: ['@babel/preset-env'],
                            plugins: [
                                '@babel/plugin-transform-runtime',
                                '@babel/plugin-transform-modules-commonjs',
                                ['@babel/plugin-proposal-decorators', { 'legacy': true }],
                                '@babel/plugin-proposal-class-properties'
                            ]
                        }
                    }
                ]
            },
            {
                test: /\.(svg|png)$/i,
                include: [
                    path.resolve(__dirname, 'webapp')
                ],
                use: [
                    {
                        loader: 'url-loader',
                    }
                ]
            },
            {
                test: /\.html$/,
                include: [
                    path.resolve(__dirname, 'node_modules/@nifi-fds/core'),
                    path.resolve(__dirname, 'webapp')
                ],
                use: [
                    { loader: 'cache-loader' },
                    {
                        loader: 'html-loader',
                        options: {
                            attrs: false
                        }
                    }
                ]
            },
            {
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
            }
        ]
    },

    plugins: [
        // Automatically load modules instead of having to import or require them everywhere
        // TODO: https://github.com/apache/nifi-fds/pull/12
        new webpack.ProvidePlugin({
            '$': 'jquery',
            jQuery: 'jquery'
        }),

        // Fix style only entry generating an extra js file
        new FixStyleOnlyEntriesPlugin(),

        // Create HTML files to serve your webpack bundles
        new HtmlWebpackPlugin({
            template: 'webapp/template.html',
            filename: 'index.html'
        })
    ]
};
