/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

public class Signer {

    private static final Logger logger = LoggerFactory.getLogger(Signer.class);

    private static final BaseEncoding BASE_64_ENCODING = BaseEncoding.base64();

    public String signFormParameters(final Long amount,
                                     final String currency,
                                     final String shipBeforeDate,
                                     final String reference,
                                     final String skin,
                                     final String merchantAccount,
                                     final String email,
                                     final String customerId,
                                     final String contract,
                                     final String allowedMethods,
                                     final String sessionValidity,
                                     final String hmacSecret,
                                     final String hmacAlgorithm) {
        try {
            final StringBuilder signingString = new StringBuilder();
            signingString.append(amount);
            signingString.append(currency);
            signingString.append(shipBeforeDate);
            signingString.append(reference);
            signingString.append(skin);
            signingString.append(merchantAccount);
            signingString.append(sessionValidity);
            if (email != null) {
                signingString.append(email);
            }
            if (customerId != null) {
                signingString.append(customerId);
            }
            // shopperLocale is ignored
            if (contract != null) {
                signingString.append(contract);
            }
            if (allowedMethods != null) {
                signingString.append(allowedMethods);
            }
            return signData(hmacSecret, hmacAlgorithm, signingString.toString());
        } catch (final SignatureGenerationException e) {
            logger.warn("Could not build hpp signature", e);
            return "";
        }
    }

    public String signData(final String secret, final String algorithm, final String signingData) throws SignatureGenerationException {
        try {
            final SecretKey key = createSecretKey(secret, algorithm);
            final Mac mac = Mac.getInstance(key.getAlgorithm());
            mac.init(key);
            return BASE_64_ENCODING.encode(mac.doFinal(signingData.getBytes("UTF8")));
        } catch (final NoSuchAlgorithmException nsae) {
            throw new SignatureGenerationException("Error while signature generation.", nsae);
        } catch (final IllegalStateException ise) {
            throw new SignatureGenerationException("Error while signature generation.", ise);
        } catch (final UnsupportedEncodingException uee) {
            throw new SignatureGenerationException("Error while signature generation.", uee);
        } catch (final InvalidKeyException ike) {
            throw new SignatureGenerationException("Error while signature generation.", ike);
        }
    }

    public boolean verifySignature(final String secret, final String algorithm, final String data, final String signature)
            throws SignatureVerificationException {
        try {
            if (data != null && signature != null) {
                final String expectedSignature = signData(secret, algorithm, data);
                return signature.equals(expectedSignature);
            }

            return false;
        } catch (SignatureGenerationException e) {
            throw new SignatureVerificationException("Error while signature verification.", e.getCause());
        }
    }

    private SecretKey createSecretKey(final String secret, final String algorithm) throws UnsupportedEncodingException {
        return new SecretKeySpec(secret.getBytes("UTF-8"), algorithm);
    }
}
