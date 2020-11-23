/*
 * Copyright 2020 kec.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hl7.tinkar.binary;

import org.eclipse.collections.api.list.ImmutableList;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.UUID;


/**
 *
 * Template for marshalable class implementations classes
 *

 private static final int marshalVersion = 1;

 // Using a static method rather than a constructor eliminates the need for
 // a readResolve method, but allows the implementation to decide how
 // to handle special cases.

    @Unmarshaler
    public static StampDTO make(TinkarInput in) {
        try {
            int objectMarshalVersion = in.readInt();
            switch (objectMarshalVersion) {
                case marshalVersion:
                    throw new UnsupportedOperationException();
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported version: " + objectMarshalVersion);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @Marshaler
    public void marshal(TinkarOutput out) {
        try {
            out.writeInt(marshalVersion);
            throw new UnsupportedOperationException();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }



 *
 *
 */
public interface Marshalable {
    
    @Marshaler
    void marshal(TinkarOutput out);
    
    default TinkarByteArrayOutput marshal() {
        TinkarByteArrayOutput byteArrayOutput = TinkarByteArrayOutput.make();
        marshal(byteArrayOutput);
        return byteArrayOutput;
    }

    static <T> T makeVersion(Class<T> objectClass, TinkarByteArrayOutput output, ImmutableList<UUID> componentUuids) {
        return makeVersion(objectClass, output.getBytes(), componentUuids);
    }

    static <T> T makeVersion(Class<T> objectClass, byte[] input, ImmutableList<UUID> componentUuids) {
        return makeVersion(objectClass, TinkarInput.make(input), componentUuids);
    }

    static <T> T makeSemanticVersion(Class<T> objectClass, TinkarInput input, ImmutableList<UUID> componentUuids,
                                     ImmutableList<UUID> definitionForSemanticUuids, ImmutableList<UUID> referencedComponentUuids) {
        try {
            ArrayList<Method> unmarshalMethodList = new ArrayList<>();
            for (Method method: objectClass.getDeclaredMethods()) {
                for (Annotation annotation: method.getAnnotations()) {
                    if (annotation instanceof SemanticVersionUnmarshaler) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            unmarshalMethodList.add(method);
                        } else {
                            throw new RuntimeException("VersionUnmarshaler method for class: " + objectClass
                                    + " is not static: " + method);
                        }
                    }
                }
            }
            if (unmarshalMethodList.isEmpty()) {
                throw new IllegalStateException("No VersionUnmarshaler method for class: " + objectClass);
            } else if (unmarshalMethodList.size() == 1) {
                Method unmarshalMethod = unmarshalMethodList.get(0);
                return (T) unmarshalMethod.invoke(null, input, componentUuids, definitionForSemanticUuids, referencedComponentUuids);
            }
            throw new RuntimeException("More than one unmarshal method for class: " + objectClass
                    + " methods: " + unmarshalMethodList);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    static <T> T makeVersion(Class<T> objectClass, TinkarInput input, ImmutableList<UUID> componentUuids) {
        try {
            ArrayList<Method> unmarshalMethodList = new ArrayList<>();
            for (Method method: objectClass.getDeclaredMethods()) {
                for (Annotation annotation: method.getAnnotations()) {
                    if (annotation instanceof VersionUnmarshaler) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            unmarshalMethodList.add(method);
                        } else {
                            throw new RuntimeException("VersionUnmarshaler method for class: " + objectClass
                                    + " is not static: " + method);
                        }
                    }
                }
            }
            if (unmarshalMethodList.isEmpty()) {
                throw new IllegalStateException("No VersionUnmarshaler method for class: " + objectClass);
            } else if (unmarshalMethodList.size() == 1) {
                Method unmarshalMethod = unmarshalMethodList.get(0);
                return (T) unmarshalMethod.invoke(null, input, componentUuids);
            }
            throw new RuntimeException("More than one unmarshal method for class: " + objectClass
                    + " methods: " + unmarshalMethodList);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    static <T> T make(Class<T> objectClass, byte[] input) {
        return make(objectClass, TinkarInput.make(input));
    }

    static <T> T make(Class<T> objectClass, TinkarByteArrayOutput output) {
        return make(objectClass, TinkarInput.make(output));
    }

    static <T> T make(Class<T> objectClass, TinkarInput input) {
        try {
            ArrayList<Method> unmarshalMethodList = new ArrayList<>();
            for (Method method: objectClass.getDeclaredMethods()) {
                for (Annotation annotation: method.getAnnotations()) {
                    if (annotation instanceof Unmarshaler) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            unmarshalMethodList.add(method);
                        } else {
                            throw new RuntimeException("Marshaler method for class: " + objectClass
                                    + " is not static: " + method);
                        }
                    }
                }
            }
            if (unmarshalMethodList.isEmpty()) {
                throw new IllegalStateException("No unmarshal method for class: " + objectClass);
            } else if (unmarshalMethodList.size() == 1) {
                Method unmarshalMethod = unmarshalMethodList.get(0);
                     return (T) unmarshalMethod.invoke(null, input);
             }
            throw new RuntimeException("More than one unmarshal method for class: " + objectClass
                    + " methods: " + unmarshalMethodList);
            
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

}