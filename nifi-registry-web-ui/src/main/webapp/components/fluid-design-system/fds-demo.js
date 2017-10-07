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
var NfRegistryExplorer = require('nifi-registry/components/explorer/nf-registry-explorer.js');
var covalentCore = require('@covalent/core');
var ngRouter = require('@angular/router');
var ngMaterial = require('@angular/material');
var nfRegistryAnimations = require('nifi-registry/nf-registry.animations.js');
var fdsDialogsModule = require('@fluid-design-system/dialogs');

var NUMBER_FORMAT = function (v) {
    return v;
};
var DECIMAL_FORMAT = function (v) {
    return v.toFixed(2);
};
var date = new Date();

/**
 * FdsDemo constructor.
 *
 * @param snackBarService       The angular material snack bar service module.
 * @param dialog                The angular material dialog module.
 * @param TdDialogService       The covalent dialog service module.
 * @param TdDataTableService    The covalent data table service module.
 * @constructor
 */
function FdsDemo(snackBarService, dialog, TdDataTableService, FdsDialogService) {

    //<editor-fold desc="Snack Bars">

    this.snackBarService = snackBarService;

    //</editor-fold>

    //<editor-fold desc="Dialog">

    this.dialog = dialog;

    //</editor-fold>

    //<editor-fold desc="Simple Dialogs">

    this.dialogService = FdsDialogService;

    //</editor-fold>

    //<editor-fold desc="Expansion Panel">

    this.expandCollapseExpansion1Msg = 'No expanded/collapsed detected yet';
    this.expansion1 = false;
    this.disabled = false;

    //</editor-fold>

    //<editor-fold desc="Autocomplete">

    this.currentState = '';
    this.reactiveStates = '';
    this.tdStates = [];
    this.tdDisabled = false;
    this.states = [
        {code: 'AL', name: 'Alabama'},
        {code: 'AK', name: 'Alaska'},
        {code: 'AZ', name: 'Arizona'},
        {code: 'AR', name: 'Arkansas'},
        {code: 'CA', name: 'California'},
        {code: 'CO', name: 'Colorado'},
        {code: 'CT', name: 'Connecticut'},
        {code: 'DE', name: 'Delaware'},
        {code: 'FL', name: 'Florida'},
        {code: 'GA', name: 'Georgia'},
        {code: 'HI', name: 'Hawaii'},
        {code: 'ID', name: 'Idaho'},
        {code: 'IL', name: 'Illinois'},
        {code: 'IN', name: 'Indiana'},
        {code: 'IA', name: 'Iowa'},
        {code: 'KS', name: 'Kansas'},
        {code: 'KY', name: 'Kentucky'},
        {code: 'LA', name: 'Louisiana'},
        {code: 'ME', name: 'Maine'},
        {code: 'MD', name: 'Maryland'},
        {code: 'MA', name: 'Massachusetts'},
        {code: 'MI', name: 'Michigan'},
        {code: 'MN', name: 'Minnesota'},
        {code: 'MS', name: 'Mississippi'},
        {code: 'MO', name: 'Missouri'},
        {code: 'MT', name: 'Montana'},
        {code: 'NE', name: 'Nebraska'},
        {code: 'NV', name: 'Nevada'},
        {code: 'NH', name: 'New Hampshire'},
        {code: 'NJ', name: 'New Jersey'},
        {code: 'NM', name: 'New Mexico'},
        {code: 'NY', name: 'New York'},
        {code: 'NC', name: 'North Carolina'},
        {code: 'ND', name: 'North Dakota'},
        {code: 'OH', name: 'Ohio'},
        {code: 'OK', name: 'Oklahoma'},
        {code: 'OR', name: 'Oregon'},
        {code: 'PA', name: 'Pennsylvania'},
        {code: 'RI', name: 'Rhode Island'},
        {code: 'SC', name: 'South Carolina'},
        {code: 'SD', name: 'South Dakota'},
        {code: 'TN', name: 'Tennessee'},
        {code: 'TX', name: 'Texas'},
        {code: 'UT', name: 'Utah'},
        {code: 'VT', name: 'Vermont'},
        {code: 'VA', name: 'Virginia'},
        {code: 'WA', name: 'Washington'},
        {code: 'WV', name: 'West Virginia'},
        {code: 'WI', name: 'Wisconsin'},
        {code: 'WY', name: 'Wyoming'},
    ];

    //</editor-fold>

    //<editor-fold desc="Searchable Expansion Panels">

    this.dataTableService = TdDataTableService;

    this.droplets = [{
        id: '23f6cc59-0156-1000-09b4-2b0610089090',
        name: "Decompression_Circular_Flow",
        displayName: 'Decompressed Circular flow',
        type: 'flow',
        sublabel: 'A sublabel',
        compliant: {
            id: '25fd6vv87-3549-0001-05g6-4d4567890765',
            label: 'Compliant',
            type: 'certification'
        },
        fleet: {
            id: '23f6cc59-3549-0001-05g6-4d4567890765',
            label: 'Fleet',
            type: 'certification'
        },
        prod: {
            id: '52fd6vv87-3549-0001-05g6-4d4567890765',
            label: 'Production Ready',
            type: 'certification'
        },
        secure: {
            id: '32f6cc59-3549-0001-05g6-4d4567890765',
            label: 'Secure',
            type: 'certification'
        },
        versions: [{
            id: '23f6cc59-0156-1000-06b4-2b0810089090',
            revision: '1',
            dependentFlows: [{
                id: '25fd6vv87-3549-0001-05g6-4d4567890765'
            }],
            created: date.setDate(date.getDate() - 1),
            updated: new Date()
        }, {
            id: '25fd6vv87-3549-0001-05g6-4d4567890765',
            revision: '2',
            dependentFlows: [{
                id: '23f6cc59-0156-1000-06b4-2b0810089090'
            }],
            created: new Date(),
            updated: new Date()
        }],
        flows: [],
        extensions: [],
        assets: [],
        actions: [{
            'name': 'Delete',
            'icon': 'fa fa-close',
            'tooltip': 'Delete User'
        }, {
            'name': 'Manage',
            'icon': 'fa fa-user',
            'tooltip': 'Manage User'
        }, {
            'name': 'Action 3',
            'icon': 'fa fa-question',
            'tooltip': 'Whatever else we want to do...'
        }]
    }, {
        id: '25fd6vv87-3249-0001-05g6-4d4767890765',
        name: "DateConversion",
        displayName: 'Date conversion',
        type: 'asset',
        sublabel: 'A sublabel',
        compliant: {
            id: '25fd6vv34-3549-0001-05g6-4d4567890765',
            label: 'Compliant',
            type: 'certification'
        },
        prod: {
            id: '52vn6vv87-3549-0001-05g6-4d4567890765',
            label: 'Production Ready',
            type: 'certification'
        },
        versions: [{
            id: '23f6ic59-0156-1000-06b4-2b0810089090',
            revision: '1',
            dependentFlows: [{
                id: '23f6cc19-0156-1000-06b4-2b0810089090'
            }],
            created: new Date(),
            updated: new Date()
        }],
        flows: [],
        extensions: [],
        assets: [],
        actions: [{
            'name': 'Delete',
            'icon': 'fa fa-close',
            'tooltip': 'Delete User'
        }]
    }, {
        id: '52fd6vv87-3294-0001-05g6-4d4767890765',
        name: "nifi-email-bundle",
        displayName: 'nifi-email-bundle',
        type: 'extension',
        sublabel: 'A sublabel',
        compliant: {
            id: '33fd6vv87-3549-0001-05g6-4d4567890765',
            label: 'Compliant',
            test: {
                label: 'test'
            },
            type: 'certification'
        },
        versions: [{
            id: '23d3cc59-0156-1000-06b4-2b0810089090',
            revision: '1',
            dependentFlows: [{
                id: '23f6cc89-0156-1000-06b4-2b0810089090'
            }],
            created: new Date(),
            updated: new Date()
        }],
        flows: [],
        extensions: [],
        assets: [],
        actions: [{
            'name': 'Delete',
            'icon': 'fa fa-close',
            'tooltip': 'Delete User'
        }, {
            'name': 'Manage',
            'icon': 'fa fa-user',
            'tooltip': 'Manage User'
        },]
    }];

    this.filteredDroplets = [];

    this.dropletColumns = [
        {name: 'id', label: 'ID', sortable: true},
        {name: 'name', label: 'Name', sortable: true,},
        {name: 'displayName', label: 'Display Name', sortable: true},
        {name: 'sublabel', label: 'Label', sortable: true},
        {name: 'type', label: 'Type', sortable: true}
    ];

    this.autoCompleteDroplets = [];
    this.dropletsSearchTerms = [];

    //</editor-fold>

    //<editor-fold desc="Data Tables">

    this.data = [{
        'id': 1,
        'name': 'Frozen yogurt',
        'type': 'Ice cream',
        'calories': 159.0,
        'fat': 6.0,
        'carbs': 24.0,
        'protein': 4.0,
        'sodium': 87.0,
        'calcium': 14.0,
        'iron': 1.0,
        'comments': 'I love froyo!',
        'actions': [{
            'name': 'Action 1',
            'icon': 'fa fa-user',
            'tooltip': 'Manage Users'
        }, {
            'name': 'Action 2',
            'icon': 'fa fa-key',
            'tooltip': 'Manage Permissions'
        }]
    }, {
        'id': 2,
        'name': 'Ice cream sandwich',
        'type': 'Ice cream',
        'calories': 237.0,
        'fat': 9.0,
        'carbs': 37.0,
        'protein': 4.3,
        'sodium': 129.0,
        'calcium': 8.0,
        'iron': 1.0,
        'actions': [{
            'name': 'Action 1',
            'icon': 'fa fa-user',
            'tooltip': 'Manage Users'
        }, {
            'name': 'Action 2',
            'icon': 'fa fa-key',
            'tooltip': 'Manage Permissions'
        }, {
            'name': 'Action 3',
            'tooltip': 'Action 3'
        }, {
            'name': 'Action 4',
            'disabled': true,
            'tooltip': 'Action 4'
        }, {
            'name': 'Action 5',
            'tooltip': 'Action 5'
        }]
    }, {
        'id': 3,
        'name': 'Eclair',
        'type': 'Pastry',
        'calories': 262.0,
        'fat': 16.0,
        'carbs': 24.0,
        'protein': 6.0,
        'sodium': 337.0,
        'calcium': 6.0,
        'iron': 7.0,
        'actions': [{
            'name': 'Action 1',
            'icon': 'fa fa-user',
            'tooltip': 'Manage Users'
        }, {
            'name': 'Action 2',
            'icon': 'fa fa-key',
            'tooltip': 'Manage Permissions'
        }, {
            'name': 'Action 3',
            'tooltip': 'Action 3'
        }, {
            'name': 'Action 4',
            'disabled': true,
            'tooltip': 'Action 4'
        }, {
            'name': 'Action 5',
            'tooltip': 'Action 5'
        }],
    }, {
        'id': 4,
        'name': 'Cupcake',
        'type': 'Pastry',
        'calories': 305.0,
        'fat': 3.7,
        'carbs': 67.0,
        'protein': 4.3,
        'sodium': 413.0,
        'calcium': 3.0,
        'iron': 8.0,
        'actions': [{
            'name': 'Action 1',
            'icon': 'fa fa-user',
            'tooltip': 'Manage Users'
        }, {
            'name': 'Action 2',
            'icon': 'fa fa-key',
            'tooltip': 'Manage Permissions'
        }, {
            'name': 'Action 3',
            'tooltip': 'Action 3'
        }, {
            'name': 'Action 4',
            'disabled': true,
            'tooltip': 'Action 4'
        }, {
            'name': 'Action 5',
            'tooltip': 'Action 5'
        }],
    }, {
        'id': 5,
        'name': 'Jelly bean',
        'type': 'Candy',
        'calories': 375.0,
        'fat': 0.0,
        'carbs': 94.0,
        'protein': 0.0,
        'sodium': 50.0,
        'calcium': 0.0,
        'iron': 0.0,
    }, {
        'id': 6,
        'name': 'Lollipop',
        'type': 'Candy',
        'calories': 392.0,
        'fat': 0.2,
        'carbs': 98.0,
        'protein': 0.0,
        'sodium': 38.0,
        'calcium': 0.0,
        'iron': 2.0,
    }, {
        'id': 7,
        'name': 'Honeycomb',
        'type': 'Other',
        'calories': 408.0,
        'fat': 3.2,
        'carbs': 87.0,
        'protein': 6.5,
        'sodium': 562.0,
        'calcium': 0.0,
        'iron': 45.0,
    }, {
        'id': 8,
        'name': 'Donut',
        'type': 'Pastry',
        'calories': 452.0,
        'fat': 25.0,
        'carbs': 51.0,
        'protein': 4.9,
        'sodium': 326.0,
        'calcium': 2.0,
        'iron': 22.0,
    }, {
        'id': 9,
        'name': 'KitKat',
        'type': 'Candy',
        'calories': 518.0,
        'fat': 26.0,
        'carbs': 65.0,
        'protein': 7.0,
        'sodium': 54.0,
        'calcium': 12.0,
        'iron': 6.0,
    }, {
        'id': 10,
        'name': 'Chocolate',
        'type': 'Candy',
        'calories': 518.0,
        'fat': 26.0,
        'carbs': 65.0,
        'protein': 7.0,
        'sodium': 54.0,
        'calcium': 12.0,
        'iron': 6.0,
    }, {
        'id': 11,
        'name': 'Chamoy',
        'type': 'Candy',
        'calories': 518.0,
        'fat': 26.0,
        'carbs': 65.0,
        'protein': 7.0,
        'sodium': 54.0,
        'calcium': 12.0,
        'iron': 6.0,
    },];

    this.filteredData = this.data;
    this.filteredTotal = this.data.length;

    this.columns = [
        {name: 'comments', label: 'Comments', width: 10},
        {name: 'name', label: 'Dessert (100g serving)', sortable: true, width: 10},
        {name: 'type', label: 'Type', sortable: true, width: 10},
        {name: 'calories', label: 'Calories', numeric: true, format: NUMBER_FORMAT, sortable: true, width: 10},
        {name: 'fat', label: 'Fat (g)', numeric: true, format: DECIMAL_FORMAT, sortable: true, width: 10},
        {name: 'carbs', label: 'Carbs (g)', numeric: true, format: NUMBER_FORMAT, sortable: true, width: 10},
        {name: 'protein', label: 'Protein (g)', numeric: true, format: DECIMAL_FORMAT, sortable: true, width: 10},
        {name: 'sodium', label: 'Sodium (mg)', numeric: true, format: NUMBER_FORMAT, sortable: true, width: 10},
        {name: 'calcium', label: 'Calcium (%)', numeric: true, format: NUMBER_FORMAT, sortable: true, width: 10},
        {name: 'iron', label: 'Iron (%)', numeric: true, format: NUMBER_FORMAT, width: 10},
    ];

    this.allRowsSelected = false;
    this.autoCompleteData = [];
    this.selectedRows = [];

    this.searchTerm = [];
    this.fromRow = 1;
    this.currentPage = 1;
    this.pageSize = 5;
    this.pageCount = 0;

    //</editor-fold>

    //<editor-fold desc="Chips $ Autocomplete">

    this.readOnly = false;

    this.items = [
        'stepper',
        'expansion-panel',
        'markdown',
        'highlight',
        'loading',
        'media',
        'chips',
        'http',
        'json-formatter',
        'pipes',
        'need more?',
    ];

    this.itemsRequireMatch = this.items.slice(0, 6);

    //</editor-fold>

    //<editor-fold desc="Radios">

    this.favoriteSeason = 'Autumn';

    this.seasonOptions = [
        'Winter',
        'Spring',
        'Summer',
        'Autumn',
    ];

    //</editor-fold>

    //<editor-fold desc="Select">

    this.selectedValue = '';

    this.foods = [
        {value: 'steak-0', viewValue: 'Steak'},
        {value: 'pizza-1', viewValue: 'Pizza'},
        {value: 'tacos-2', viewValue: 'Tacos'},
    ];

    //</editor-fold>

    //<editor-fold desc="Chips">

    this.chips = [
        {name: 'Default', color: '', selected: false},
        {name: 'Default (selected)', color: '', selected: true},
        {name: 'Primary (selected)', color: 'primary', selected: true},
        {name: 'Accent (selected)', color: 'accent', selected: true},
        {name: 'Warn (selected)', color: 'warn', selected: true},
    ];

    //</editor-fold>

    //<editor-fold desc="Checkbox">

    this.user = {
        agreesToTOS: false
    };

    this.groceries = [{
        bought: true,
        name: 'Seitan',
    }, {
        bought: false,
        name: 'Almond Meal Flour',
    }, {
        bought: false,
        name: 'Organic Eggs',
    },];

    //</editor-fold>

    //<editor-fold desc="Slide Toggle">

    this.systems = [{
        name: 'Lights',
        on: false,
        color: 'primary',
    }, {
        name: 'Surround Sound',
        on: true,
        color: 'accent',
    }, {
        name: 'T.V.',
        on: true,
        color: 'warn',
    },];

    this.house = {
        lockHouse: false,
    };

    //</editor-fold>
};

FdsDemo.prototype = {
    constructor: FdsDemo,

    //<editor-fold desc="Autocomplete">

    displayFn: function (value) {
        return value && typeof value === 'object' ? value.name : value;
    },

    filterStates: function (val) {
        return val ? this.states.filter(function (s) {
            return s.name.match(new RegExp(val, 'gi'));
        }) : this.states;
    },

    //</editor-fold>

    //<editor-fold desc="Snack Bars">

    showSnackBar: function () {
        var snackBarRef = this.snackBarService.open('Message', 'Action', {duration: 3000});
    },

    //</editor-fold>

    //<editor-fold desc="Dialog">

    openDialog: function () {
        this.dialog.open(NfRegistryExplorer, {
            height: '50%', // can be px or %
            width: '60%', // can be px or %
        });
    },

    //</editor-fold>

    //<editor-fold desc="Expansion Panel">

    toggleExpansion1: function () {
        if (!this.disabled) {
            this.expansion1 = !this.expansion1;
        }
    },

    toggleDisabled: function () {
        this.disabled = !this.disabled;
    },

    expandExpansion1Event: function () {
        this.expandCollapseExpansion1Msg = 'Expand event emitted.';
    },

    collapseExpansion1Event: function () {
        this.expandCollapseExpansion1Msg = 'Collapse event emitted.';
    },

    //</editor-fold>

    //<editor-fold desc="Simple Dialogs">

    openAlert: function () {
        this.dialogService.openAlert({
            title: 'Alert',
            disableClose: true,
            message: 'This is how simple it is to create an alert with this wrapper service.',
        });
    },

    openConfirm: function () {
        this.dialogService.openConfirm({
            title: 'Confirm',
            message: 'This is how simple it is to create a confirm with this wrapper service. Do you agree?',
            cancelButton: 'Disagree',
            acceptButton: 'Agree',
        });
    },

    openPrompt: function () {
        this.dialogService.openPrompt({
            title: 'Prompt',
            message: 'This is how simple it is to create a prompt with this wrapper service. Prompt something.',
            value: 'Populated value',
            cancelButton: 'Cancel',
            acceptButton: 'Ok',
        });
    },

    //</editor-fold>

    //<editor-fold desc="Searchable Expansion Panels">

    isDropletFilterChecked: function (term) {
        return (this.dropletsSearchTerms.indexOf(term) > -1);
    },

    getDropletTypeCount: function (type) {
        return this.filteredDroplets.filter(function (droplet) {
            return droplet.type === type;
        }).length;
    },

    getDropletCertificationCount: function (certification) {
        return this.filteredDroplets.filter(function (droplet) {
            return Object.keys(droplet).find(function (key) {
                if (key === certification && droplet[certification].type === 'certification') {
                    return droplet;
                }
            });
        }).length;
    },

    getSortBy: function () {
        var sortByColumnLabel;
        var arrayLength = this.dropletColumns.length;
        for (var i = 0; i < arrayLength; i++) {
            if (this.dropletColumns[i].active === true) {
                sortByColumnLabel = this.dropletColumns[i].label;
                break;
            }
        }
        return sortByColumnLabel;
    },

    sortDroplets: function (column) {
        if (column.sortable === true) {
            // toggle column sort order
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filterDroplets(column.name, sortOrder);
            this.activeColumn = column;
            //only one column can be actively sorted so we reset all to inactive
            this.dropletColumns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    toggleDropletsFilter: function (searchTerm) {
        var applySearchTerm = true;
        // check if the search term is already applied and remove it if true
        if (this.dropletsSearchTerms.length > 0) {
            var arrayLength = this.dropletsSearchTerms.length;
            for (var i = 0; i < arrayLength; i++) {
                var index = this.dropletsSearchTerms.indexOf(searchTerm);
                if (index > -1) {
                    this.dropletsSearchTerms.splice(index, 1);
                    applySearchTerm = false;
                }
            }
        }

        // if we just removed the search term do NOT apply it again
        if (applySearchTerm) {
            this.dropletsSearchTerms.push(searchTerm);
        }

        this.filterDroplets(this.activeColumn.name, this.activeColumn.sortOrder);
    },

    filterDroplets: function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in this.dropletColumns
        if (sortBy === undefined) {
            var arrayLength = this.dropletColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.dropletColumns[i].sortable === true) {
                    sortBy = this.dropletColumns[i].name;
                    this.activeColumn = this.dropletColumns[i];
                    //only one column can be actively sorted so we reset all to inactive
                    this.dropletColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.dropletColumns[i].active = true;
                    this.dropletColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newData = this.droplets;

        for (var i = 0; i < this.dropletsSearchTerms.length; i++) {
            newData = this.filterData(newData, this.dropletsSearchTerms[i], true, this.activeColumn.name);
        }

        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.filteredDroplets = newData;
        this.getAutoCompleteDroplets();
    },

    getAutoCompleteDroplets: function () {
        var self = this;
        this.autoCompleteDroplets = [];
        this.dropletColumns.forEach(function (c) {
            self.filteredDroplets.forEach(function (r) {
                (r[c.name.toLowerCase()]) ? self.autoCompleteDroplets.push(r[c.name.toLowerCase()].toString()) : '';
            });
        });
    },

    //</editor-fold>

    filterData: function (data, searchTerm, ignoreCase) {
        var field = '';
        if (searchTerm.indexOf(":") > -1) {
            field = searchTerm.split(':')[0].trim();
            searchTerm = searchTerm.split(':')[1].trim();
        }
        var filter = searchTerm ? (ignoreCase ? searchTerm.toLowerCase() : searchTerm) : '';

        if (filter) {
            data = data.filter(function (item) {
                var res = Object.keys(item).find(function (key) {
                    if (field.indexOf(".") > -1) {
                        var objArray = field.split(".");
                        var obj = item;
                        var arrayLength = objArray.length;
                        for (var i = 0; i < arrayLength; i++) {
                            try {
                                obj = obj[objArray[i]];
                            } catch (e) {
                                return false;
                            }
                        }
                        var preItemValue = ('' + obj);
                        var itemValue = ignoreCase ? preItemValue.toLowerCase() : preItemValue;
                        return itemValue.indexOf(filter) > -1;
                    } else {
                        if (key !== field && field !== '') {
                            return false;
                        }
                        var preItemValue = ('' + item[key]);
                        var itemValue = ignoreCase ? preItemValue.toLowerCase() : preItemValue;
                        return itemValue.indexOf(filter) > -1;
                    }
                });
                return !(typeof res === 'undefined');
            });
        }
        return data;
    },

    //<editor-fold desc="Data Tables">

    sort: function (sortEvent, column) {
        if (column.sortable) {
            var sortBy = column.name;
            var sortOrder = column.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';
            this.filter(sortBy, sortOrder);

            //only one column can be actively sorted so we reset all to inactive
            this.columns.forEach(function (c) {
                c.active = false;
            });
            //and set this column as the actively sorted column
            column.active = true;
        }
    },

    searchRemove: function (searchTerm) {
        //only remove the first occurrence of the search term
        var index = this.searchTerm.indexOf(searchTerm);
        if (index !== -1) {
            this.searchTerm.splice(index, 1);
        }
        this.fromRow = 1;
        this.currentPage = 1;
        this.pageSize = 1;
        this.filter();
    },

    searchAdd: function (searchTerm) {
        this.searchTerm.push(searchTerm);
        this.fromRow = 1;
        this.currentPage = 1;
        this.pageSize = 1;
        this.filter();
    },

    page: function (pagingEvent) {
        this.fromRow = pagingEvent.fromRow;
        this.currentPage = pagingEvent.page;
        this.pageSize = pagingEvent.pageSize;
        this.filter();
    },

    filter: function (sortBy, sortOrder) {
        if (this.allRowsSelected) {
            this.toggleSelectAll();
        }
        this.deselectAll();
        var newData = this.data;

        for (var i = 0; i < this.searchTerm.length; i++) {
            newData = this.filterData(newData, this.searchTerm[i], true);
        }
        this.filteredTotal = newData.length;
        newData = this.dataTableService.sortData(newData, sortBy, sortOrder);
        this.pageCount = newData.length;
        newData = this.dataTableService.pageData(newData, this.fromRow, this.currentPage * this.pageSize);
        this.filteredData = newData;
        this.getAutoCompleteData();
    },

    toggleSelect: function (row) {
        if (this.allFilteredRowsSelected()) {
            this.allRowsSelected = true;
        } else {
            this.allRowsSelected = false;
        }
    },

    toggleSelectAll: function () {
        if (this.allRowsSelected) {
            this.selectAll();
        } else {
            this.deselectAll();
        }
    },

    selectAll: function () {
        this.filteredData.forEach(function (c) {
            c.checked = true;
        });
    },

    deselectAll: function () {
        this.filteredData.forEach(function (c) {
            c.checked = false;
        });
    },

    allFilteredRowsSelected: function () {
        var allFilteredRowsSelected = true;
        this.filteredData.forEach(function (c) {
            if (c.checked === undefined || c.checked === false) {
                allFilteredRowsSelected = false;
            }
        });

        return allFilteredRowsSelected;
    },

    areTooltipsOn: function () {
        return this.columns[0].hasOwnProperty('tooltip');
    },

    toggleTooltips: function () {
        if (this.columns[0].tooltip) {
            this.columns.forEach(function (c) {
                delete c.tooltip;
            });
        } else {
            this.columns.forEach(function (c) {
                c.tooltip = 'This is ' + c.label + '!';
            });
        }
    },

    openDataTablePrompt: function (row, name) {
        this.dialogService.openPrompt({
            message: 'Enter comment?',
            value: row[name],
        }).afterClosed().subscribe(function (value) {
            if (value !== undefined) {
                row[name] = value;
            }
        })
    },

    getAutoCompleteData: function () {
        var self = this;
        this.autoCompleteData = [];
        this.columns.forEach(function (c) {
            self.filteredData.forEach(function (r) {
                (r[c.name.toLowerCase()]) ? self.autoCompleteData.push(r[c.name.toLowerCase()].toString()) : '';
            });
        });
    },

    //</editor-fold>

    //<editor-fold desc="Chips $ Autocomplete">

    toggleReadOnly: function () {
        this.readOnly = !this.readOnly;
    },

    //</editor-fold>

    //<editor-fold desc="Life Cycle Listeners">

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        this.filter();
        this.filterDroplets();
    }

    //</editor-fold>
};

FdsDemo.annotations = [
    new ngCore.Component({
        template: require('./fds-demo.html!text'),
        animations: [nfRegistryAnimations.slideInLeftAnimation],
        host: {
            '[@routeAnimation]': 'routeAnimation'
        }
    })
];

FdsDemo.parameters = [ngMaterial.MdSnackBar, ngMaterial.MdDialog, covalentCore.TdDataTableService, fdsDialogsModule.FdsDialogService];

module.exports = FdsDemo;
