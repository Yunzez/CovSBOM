/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.ext.jython;

import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * {@link JythonVersionAdapter} for Jython 2.2.
 */
public class _Jython22VersionAdapter extends JythonVersionAdapter {

    public boolean isPyInstance(Object obj) {
        return obj instanceof PyJavaInstance;
    }

    public Object pyInstanceToJava(Object pyInstance) {
        return ((PyJavaInstance) pyInstance).__tojava__(java.lang.Object.class);
    }

    public String getPythonClassName(PyObject pyObject) {
        return pyObject.getType().getFullName();
    }

}
