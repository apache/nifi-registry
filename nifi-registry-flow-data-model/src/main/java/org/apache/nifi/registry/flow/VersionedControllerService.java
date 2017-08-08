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

package org.apache.nifi.registry.flow;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

public class VersionedControllerService extends VersionedComponent {

    private String type;
    private Bundle bundle;
    private List<ControllerServiceAPI> controllerServiceApis;

    private Map<String, String> properties;
    private String annotationData;


    @ApiModelProperty(value = "The type of the controller service.")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(value = "The details of the artifact that bundled this processor type.")
    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    @ApiModelProperty(value = "Lists the APIs this Controller Service implements.")
    public List<ControllerServiceAPI> getControllerServiceApis() {
        return controllerServiceApis;
    }

    public void setControllerServiceApis(List<ControllerServiceAPI> controllerServiceApis) {
        this.controllerServiceApis = controllerServiceApis;
    }

    @ApiModelProperty(value = "The properties of the controller service.")
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @ApiModelProperty(value = "The annotation for the controller service. This is how the custom UI relays configuration to the controller service.")
    public String getAnnotationData() {
        return annotationData;
    }

    public void setAnnotationData(String annotationData) {
        this.annotationData = annotationData;
    }

    @Override
    public int hashCode() {
        final String id = getIdentifier();
        return 37 + 3 * ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        if (obj.getClass() != VersionedControllerService.class) {
            return false;
        }

        final VersionedControllerService other = (VersionedControllerService) obj;
        return Objects.equals(getIdentifier(), other.getIdentifier());
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.CONTROLLER_SERVICE;
    }
}
