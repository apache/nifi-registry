/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *       FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */

module.exports = {
    "extends": "eslint-config-airbnb",
    "env": {
        "browser": true,
        "es6": true,
        "jasmine": true,
        "jquery": true
    },
    "parserOptions": {
        "ecmaVersion": 2017,
        "sourceType": "module"
    },
    "parser": "@typescript-eslint/parser",
    "plugins": ["@typescript-eslint"],
    overrides: [
        {
            // Legacy Javascript files
            files: ['*.js'],
            rules: {
                "dot-notation": 0,
                "prefer-arrow-callback": 0,
                "no-var": 0,
                "no-redeclare": 0,
                "no-shadow": 0,
                "quote-props": 0,
                "object-shorthand": 0,
                "vars-on-top": 0,
                "no-param-reassign": 0,
                "block-scoped-var": 0,
                "prefer-destructuring": 0,
                "prefer-template": 0,
                "consistent-return": 0,
                "no-restricted-properties": 0,
                "no-use-before-define": 0,
                "object-curly-spacing": 0,
                "newline-per-chained-call": 0,
                "no-bitwise": 0,
                "no-nested-ternary": 0,
                "no-useless-escape": 0,
                "no-prototype-builtins": 0,
            }
        },
        {
            // Typescript files
            files: ['*.ts'],
            rules: {
                '@typescript-eslint/no-unused-vars': [2, { args: "none" }]
            }
        }
    ],
    "rules": {
        // Customize for all files
        "no-unused-vars": ["error", { "args": "none" }],
        "lines-between-class-members": ["error", "always", { "exceptAfterSingleLine": true }],
        "indent": ["error", 4],

        // Disable for all files
        "max-len": 0,
        "func-names": 0,
        "spaced-comment": 0,
        "comma-dangle": 0,
        "import/extensions": 0,
        "import/no-unresolved": 0,
        "import/no-extraneous-dependencies": 0,
        "no-plusplus": 0,
        "react/no-this-in-sfc": 0,
        "prefer-promise-reject-errors": 0,
        "object-curly-newline": 0,
        "no-restricted-globals": 0,
        "import/prefer-default-export": 0
    }
};
