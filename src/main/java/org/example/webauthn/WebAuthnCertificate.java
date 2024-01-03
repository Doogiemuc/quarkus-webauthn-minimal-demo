package org.example.webauthn;

//@Entity
public class WebAuthnCertificate /* extends PanacheEntity */ {

	//@ManyToOne
	public WebAuthnCredential webAuthnCredential;

	/**
	 * A X509 certificate encoded as base64url.
	 */
	public String x5c;
}