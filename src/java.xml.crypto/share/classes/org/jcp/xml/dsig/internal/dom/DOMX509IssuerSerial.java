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
 * $Id: DOMX509IssuerSerial.java 1788465 2017-03-24 15:10:51Z coheigea $
 */
package org.jcp.xml.dsig.internal.dom;

import org.jspecify.annotations.Nullable;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;

import java.math.BigInteger;

import javax.security.auth.x500.X500Principal;

import org.w3c.dom.Element;

/**
 * DOM-based implementation of X509IssuerSerial.
 *
 */
public final class DOMX509IssuerSerial extends BaseStructure
    implements X509IssuerSerial {

    private final String issuerName;
    private final BigInteger serialNumber;

    /**
     * Creates a {@code DOMX509IssuerSerial} containing the specified
     * issuer distinguished name/serial number pair.
     *
     * @param issuerName the X.509 issuer distinguished name in RFC 2253
     *    String format
     * @param serialNumber the serial number
     * @throws IllegalArgumentException if the format of {@code issuerName}
     *    is not RFC 2253 compliant
     * @throws NullPointerException if {@code issuerName} or
     *    {@code serialNumber} is {@code null}
     */
    public DOMX509IssuerSerial(String issuerName, BigInteger serialNumber) {
        if (issuerName == null) {
            throw new NullPointerException("issuerName cannot be null");
        }
        if (serialNumber == null) {
            throw new NullPointerException("serialNumber cannot be null");
        }
        // check that issuer distinguished name conforms to RFC 2253
        new X500Principal(issuerName);
        this.issuerName = issuerName;
        this.serialNumber = serialNumber;
    }

    /**
     * Creates a {@code DOMX509IssuerSerial} from an element.
     *
     * @param isElem an X509IssuerSerial element
     */
    public DOMX509IssuerSerial(Element isElem) throws MarshalException {
        Element iNElem = DOMUtils.getFirstChildElement(isElem,
                                                       "X509IssuerName",
                                                       XMLSignature.XMLNS);
        Element sNElem = DOMUtils.getNextSiblingElement(iNElem,
                                                        "X509SerialNumber",
                                                        XMLSignature.XMLNS);
        issuerName = iNElem.getFirstChild().getNodeValue();
        serialNumber = new BigInteger(sNElem.getFirstChild().getNodeValue());
    }

    @Override
    public String getIssuerName() {
        return issuerName;
    }

    @Override
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    @Override
    
    
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof X509IssuerSerial)) {
            return false;
        }
        X509IssuerSerial ois = (X509IssuerSerial)obj;
        return issuerName.equals(ois.getIssuerName()) &&
                serialNumber.equals(ois.getSerialNumber());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + issuerName.hashCode();
        result = 31 * result + serialNumber.hashCode();

        return result;
    }
}
