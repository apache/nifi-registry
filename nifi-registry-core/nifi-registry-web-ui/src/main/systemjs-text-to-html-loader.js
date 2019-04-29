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

/**
 * Removes inline systemjs-style loader syntax for html!text imports
 *
 *   require('./some-module.html!systemjs-plugin')  ->  require('./some-module.html')
 */
module.exports = function (source) {
    if (source.indexOf('.html')) {
        const exp = new RegExp('require\\(([\'"])([^!\\s]*)!text\\1\\)', 'g');
        source = source.replace(exp, `require($1$2$1)`);
    }

    return source;
};
