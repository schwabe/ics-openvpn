// ExternalCertificateProvider.aidl
package de.blinkt.openvpn.api;


/*
 * This is very simple interface that is specialised to have only the minimal set of crypto
 * operation that are needed for OpenVPN to authenticate with an external certificate
 */
interface ExternalCertificateProvider {
    /**
     * Requests signing the data with RSA/ECB/PKCS1PADDING
     * for RSA certficate and with NONEwithECDSA for EC certificates
     * @parm alias the parameter that
     */
    byte[] getSignedData(String alias, in byte[] data);

    /**
     * Requests a
     */
    String[] getCertificateChain(in String alias);

    /**
     * request an Intent that should be started when user uses the select certificate box
     * the already selected alias will be provided in the extra android.security.extra.KEY_ALIAS
     * if applicable
     */

}
