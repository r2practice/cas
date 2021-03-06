/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
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
package org.jasig.cas.adaptors.x509.authentication.principal;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import edu.vt.middleware.crypt.x509.DNUtils;
import edu.vt.middleware.crypt.x509.types.AttributeType;


/**
 * Credential to principal resolver that extracts one or more attribute values
 * from the certificate subject DN and combines them with intervening delimiters.
 *
 * <p>
 * This class is a replacement for the following deprecated resolvers:
 * <ol>
 * <li>X509CertificateCredentialsToIdentifierPrincipalResolver</li>
 * </ol>
 *
 * @author Marvin S. Addison
 * @since 3.4.4
 *
 */
public class X509CertificateCredentialsToSubjectPrinciplalResolver
extends AbstractX509CertificateCredentialsToPrincipalResolver {

    /** Pattern used to extract attribute names from descriptor. */
    private static final Pattern ATTR_PATTERN = Pattern.compile("\\$(\\w+)");

    /** Descriptor representing an abstract format of the principal to be resolved.*/
    @NotNull
    private String descriptor;

    /**
     * Sets the descriptor that describes for format of the principal ID to
     * create from X.509 subject DN attributes.  The descriptor is made up of
     * common X.509 attribute names prefixed by "$", which are replaced by
     * attribute values extracted from DN attribute values.
     * <p>
     * EXAMPLE:
     * <p>
     * <pre>
     * <bean class="org.jasig.cas.adaptors.x509.authentication.principal.X509CertificateCredentialsToSubjectPrinciplalResolver"
     *   p:descriptor="$UID@$DC.$DC" />
     * </pre>
     * <p>
     * The above bean when applied to a certificate with the DN<br>
     * <strong>DC=edu, DC=vt/UID=jacky, CN=Jascarnella Ellagwonto</strong><br>
     * produces the principal <strong>jacky@vt.edu</strong>.
     *
     * @param s Descriptor string where attribute names are prefixed with "$"
     * to identify replacement by real attribute values from the subject DN.
     * Valid attributes include common X.509 DN attributes such as the following:
     * <ul>
     * <li>C</li>
     * <li>CN</li>
     * <li>DC</li>
     * <li>EMAILADDRESS</li>
     * <li>L</li>
     * <li>O</li>
     * <li>OU</li>
     * <li>SERIALNUMBER</li>
     * <li>ST</li>
     * <li>UID</li>
     * <li>UNIQUEIDENTIFIER</li>
     * </ul>
     * For a complete list of supported attributes, see
     * {@link edu.vt.middleware.crypt.x509.types.AttributeType}.
     *
     */
    public void setDescriptor(final String s) {
        this.descriptor = s;
    }

    /**
     * Replaces placeholders in the descriptor with values extracted from attribute
     * values in relative distinguished name components of the DN.
     *
     * @param certificate X.509 certificate credential.
     * @return Resolved principal ID.
     * @see org.jasig.cas.adaptors.x509.authentication.principal.AbstractX509CertificateCredentialsToPrincipalResolver#resolvePrincipalInternal(java.security.cert.X509Certificate)
     */
    @Override
    protected String resolvePrincipalInternal(final X509Certificate certificate) {
        log.debug("Resolving principal for {}", certificate);
        final StringBuffer sb = new StringBuffer();
        final Matcher m = ATTR_PATTERN.matcher(this.descriptor);
        final Map<String, AttributeContext> attrMap = new HashMap<String, AttributeContext>();
        String name;
        String[] values;
        AttributeContext context;
        while (m.find()) {
            name = m.group(1);
            if (!attrMap.containsKey(name)) {
                values = DNUtils.getAttributeValues(
                        certificate.getSubjectX500Principal(),
                        AttributeType.fromName(name));
                attrMap.put(name, new AttributeContext(name, values));
            }
            context = attrMap.get(name);
            m.appendReplacement(sb, context.nextValue());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final class AttributeContext {
        private int currentIndex;
        private String name;
        private final String[] values;

        public AttributeContext(final String name, final String[] values) {
            this.values = values;
        }

        public String nextValue() {
            if (this.currentIndex == this.values.length) {
                throw new IllegalStateException("No values remaining for attribute " + this.name);
            }
            return this.values[this.currentIndex++];
        }
    }
}
