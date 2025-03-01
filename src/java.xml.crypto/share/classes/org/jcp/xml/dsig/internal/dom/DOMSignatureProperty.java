/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * $Id: DOMSignatureProperty.java 1788465 2017-03-24 15:10:51Z coheigea $
 */
package org.jcp.xml.dsig.internal.dom;

import org.jspecify.annotations.Nullable;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DOM-based implementation of SignatureProperty.
 *
 */
public final class DOMSignatureProperty extends BaseStructure
    implements SignatureProperty {

    private final String id;
    private final String target;
    private final List<XMLStructure> content;

    /**
     * Creates a {@code SignatureProperty} from the specified parameters.
     *
     * @param content a list of one or more {@link XMLStructure}s. The list
     *    is defensively copied to protect against subsequent modification.
     * @param target the target URI
     * @param id the Id (may be {@code null})
     * @throws ClassCastException if {@code content} contains any
     *    entries that are not of type {@link XMLStructure}
     * @throws IllegalArgumentException if {@code content} is empty
     * @throws NullPointerException if {@code content} or
     *    {@code target} is {@code null}
     */
    public DOMSignatureProperty(List<? extends XMLStructure> content,
                                String target, String id)
    {
        if (target == null) {
            throw new NullPointerException("target cannot be null");
        } else if (content == null) {
            throw new NullPointerException("content cannot be null");
        } else if (content.isEmpty()) {
            throw new IllegalArgumentException("content cannot be empty");
        } else {
            this.content = Collections.unmodifiableList(
                new ArrayList<>(content));
            for (int i = 0, size = this.content.size(); i < size; i++) {
                if (!(this.content.get(i) instanceof XMLStructure)) {
                    throw new ClassCastException
                        ("content["+i+"] is not a valid type");
                }
            }
        }
        this.target = target;
        this.id = id;
    }

    /**
     * Creates a {@code DOMSignatureProperty} from an element.
     *
     * @param propElem a SignatureProperty element
     */
    public DOMSignatureProperty(Element propElem)
        throws MarshalException
    {
        // unmarshal attributes
        target = DOMUtils.getAttributeValue(propElem, "Target");
        if (target == null) {
            throw new MarshalException("target cannot be null");
        }
        id = DOMUtils.getIdAttributeValue(propElem, "Id");

        List<XMLStructure> newContent = new ArrayList<>();
        Node firstChild = propElem.getFirstChild();
        while (firstChild != null) {
            newContent.add(new javax.xml.crypto.dom.DOMStructure(firstChild));
            firstChild = firstChild.getNextSibling();
        }
        if (newContent.isEmpty()) {
            throw new MarshalException("content cannot be empty");
        } else {
            this.content = Collections.unmodifiableList(newContent);
        }
    }

    @Override
    public List<XMLStructure> getContent() {
        return content;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTarget() {
        return target;
    }

    public static void marshal(XmlWriter xwriter, SignatureProperty sigProp, String dsPrefix, XMLCryptoContext context)
        throws MarshalException
    {
        xwriter.writeStartElement(dsPrefix, "SignatureProperty", XMLSignature.XMLNS);

        // set attributes
        xwriter.writeIdAttribute("", "", "Id", sigProp.getId());
        xwriter.writeAttribute("", "", "Target", sigProp.getTarget());

        // create and append any elements and mixed content
        List<XMLStructure> content = getContent(sigProp);
        for (XMLStructure property : content) {
            xwriter.marshalStructure(property, dsPrefix, context);
        }

        xwriter.writeEndElement(); // "SignatureProperty"
    }

    @Override
    
    
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SignatureProperty)) {
            return false;
        }
        SignatureProperty osp = (SignatureProperty)o;

        boolean idsEqual = id == null ? osp.getId() == null
                                       : id.equals(osp.getId());

        @SuppressWarnings("unchecked")
        List<XMLStructure> ospContent = osp.getContent();
        return equalsContent(ospContent) &&
                target.equals(osp.getTarget()) && idsEqual;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (id != null) {
            result = 31 * result + id.hashCode();
        }
        result = 31 * result + target.hashCode();
        result = 31 * result + content.hashCode();

        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<XMLStructure> getContent(SignatureProperty prop) {
        return prop.getContent();
    }
    private boolean equalsContent(List<XMLStructure> otherContent) {
        int osize = otherContent.size();
        if (content.size() != osize) {
            return false;
        }
        for (int i = 0; i < osize; i++) {
            XMLStructure oxs = otherContent.get(i);
            XMLStructure xs = content.get(i);
            if (oxs instanceof javax.xml.crypto.dom.DOMStructure) {
                if (!(xs instanceof javax.xml.crypto.dom.DOMStructure)) {
                    return false;
                }
                Node onode = ((javax.xml.crypto.dom.DOMStructure)oxs).getNode();
                Node node = ((javax.xml.crypto.dom.DOMStructure)xs).getNode();
                if (!DOMUtils.nodesEqual(node, onode)) {
                    return false;
                }
            } else {
                if (!(xs.equals(oxs))) {
                    return false;
                }
            }
        }

        return true;
    }
}
