// ExternalCertificateProvider.aidl
package de.blinkt.openvpn.api;

/*
 * This is very simple interface that is specialised to have only the minimal set of crypto
 * operation that are needed for OpenVPN to authenticate with an external certificate
 */
interface ExternalCertificateProvider {
    /**
     * deprecated use {@link #getSignedDataWithExtra} instead
     *
     * Requests signing the data with RSA/ECB/PKCS1PADDING
     * for RSA certficate and with NONEwithECDSA for EC certificates
     * @param alias user certificate identifier
     */
    byte[] getSignedData(in String alias, in byte[] data);

    /**
     * Requests the certificate chain for the selected alias
     * The first certifcate returned is assumed to be
     * the user certificate
     */
    byte[] getCertificateChain(in String alias);

    /**
     * This function is called for the app to get additional meta information from the
     * external provider and will be called with the stored alias in the app
     *
     * For external app provider that do not provide an activity to configure them, this
     * is used to get the alias that should be used.
     * The format is the same as the activity should return, i.e.
     *
     * EXTRA_ALIAS = "de.blinkt.openvpn.api.KEY_ALIAS"
     * EXTRA_DESCRIPTION = "de.blinkt.openvpn.api.KEY_DESCRIPTION"
     *
     * as the keys for the bundle.
     *
     */
    Bundle getCertificateMetaData(in String alias);

    /**
     * Requests signing the data with RSA/ECB/nopadding, RSA/ECB/PKCS1PADDING or PKCS1PSSPADDING
     * for RSA certficate and with NONEwithECDSA for EC certificates
     * @param alias user certificate identifier
     * @param data the data to be signed
     * @param extra additional information.
     * Should contain the following keys:
     * <ul>
       * <li>int key "de.blinkt.openvpn.api.RSA_PADDING_TYPE", may be set as:
       * <ul>
         * <li>0 - for RSA/ECB/nopadding
         * <li>1 - for RSA/ECB/PKCS1PADDING
         * <li>2 - for PKCS1PSSPADDING
       * </ul>
       * <li>string key "de.blinkt.openvpn.api.SALTLEN", may be set as:
       * <ul>
         * <li>"digest" - use the same salt size as the hash to sign
         * <li>"max" - use maximum possible saltlen which is '(nbits-1)/8 - hlen - 2'. Here
           * 'nbits' is the number of bits in the key modulus and 'hlen' is the size in octets of
           * the hash. See: RFC 8017 sec 8.1.1 and 9.1.1.
       * </ul>
       * <li>boolean key "de.blinkt.openvpn.api.NEEDS_DIGEST", indicating that the data should be
         * hashed before signing or not
       * <li>string key "de.blinkt.openvpn.api.DIGEST", the short common digest algorithm name to
         * use (such as SHA256, SHA224, etc.)
     * </ul>
     */
    byte[] getSignedDataWithExtra(in String alias, in byte[] data, in Bundle extra);
}
