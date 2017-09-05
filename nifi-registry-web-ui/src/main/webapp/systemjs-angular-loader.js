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

var templateUrlRegex = /templateUrl\s*:(\s*['"`](.*?)['"`]\s*)/gm;
var stylesRegex = /styleUrls *:(\s*\[[^\]]*?\])/g;
var stringRegex = /(['`"])((?:[^\\]\\\1|.)*?)\1/g;

module.exports.translate = function (load) {
    if (load.source.indexOf('moduleId') != -1) return load;

    var url = document.createElement('a');
    url.href = load.address;

    var basePathParts = url.pathname.split('/');

    basePathParts.pop();
    var basePath = basePathParts.join('/');

    var baseHref = document.createElement('a');
    baseHref.href = this.baseURL;
    baseHref = baseHref.pathname;

    if (!baseHref.startsWith('/base/')) { // it is not karma
        basePath = basePath.replace(baseHref, '');
    }

    load.source = load.source
        .replace(templateUrlRegex, function (match, quote, url) {
            var resolvedUrl = url;

            if (url.startsWith('.')) {
                resolvedUrl = basePath + url.substr(1);
            }

            return 'templateUrl: "' + resolvedUrl + '"';
        })
        .replace(stylesRegex, function (match, relativeUrls) {
            var urls = [];

            while ((match = stringRegex.exec(relativeUrls)) !== null) {
                if (match[2].startsWith('.')) {
                    urls.push('"' + basePath + match[2].substr(1) + '"');
                } else {
                    urls.push('"' + match[2] + '"');
                }
            }

            return "styleUrls: [" + urls.join(', ') + "]";
        });

    return load;
};
