package org.example.webauthn;

import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.auth.webauthn.PublicKeyCredential;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * This credential contains the public-key linked to the UserEntity.
 * Keep in mind that one user (a person) might use several authenticator devices.
 */
@Slf4j
//@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userName", "credID"}))
//@Entity
public class WebAuthnCredential /* extends PanacheEntity */ {

	public static List<WebAuthnCredential> webAuthnCredentialMap = new ArrayList<>();

	/**
	 * Mock implementation of the quarkus-panache list() method to lookup Entities
	 *
	 * @param searchColumnName only search for attribute "username" or "credId" is supported in this mock!
	 * @param searchValue the username that we are searchfing for
	 */
	public static List<WebAuthnCredential> list(String searchColumnName, String searchValue) {
		if (searchColumnName == null) throw new RuntimeException("Must provide a searchColumnName!");
		List<WebAuthnCredential> result;
		if (searchColumnName.equalsIgnoreCase("username")) {
			result = webAuthnCredentialMap.stream().filter(cred -> cred.userName.equals(searchValue)).toList();
		} else if (searchColumnName.equalsIgnoreCase("credID")) {
			result = webAuthnCredentialMap.stream().filter(cred -> cred.userName.equals(searchValue)).toList();
		} else {
			throw new RuntimeException("Can only search for 'username' or 'credID'!");
		}
		return result;
	}

	/**
	 * mock implementation of the persist call
	 */
	public void persist() {
		log.info("Persisting "+this.toString());
		webAuthnCredentialMap.add(this);
	}

	/**
	 * The username linked to this authenticator.
	 *
	 * Whether you want to user usernames or email addresses as usernames is an important decision
	 * you should think about! <a href="https://passwordless.id/thoughts/emails-vs-usernames">email-vs-usernames</a>
	 *
	 * In this example we use emails as usernames!
	 */
	@NonNull
	public String userName;

	/**
	 * The type of key (must be "public-key")
	 */
	public String type = "public-key";

	/**
	 * The non user identifiable id for the authenticator.
	 *
	 * A single user can have more than one authenticator device,
	 * which means a single username can map to multiple credential IDs,
	 * all of which identify the same user.
	 *
	 * An authenticator device may be shared by multiple users,
	 * because a single person may want multiple user accounts with different usernames,
	 * all of which having the same authenticator device.
	 * So a single credential ID may be used by multiple different users.
	 */
	@NonNull
	public String credID;

	/**
	 * The public key associated with this authenticator
	 */
	@NonNull
	public String publicKey;

	/**
	 * The signature counter of the authenticator to prevent replay attacks
	 */
	@NonNull
	public long counter;

  /*
  https://developers.yubico.com/java-webauthn-server/JavaDoc/webauthn-server-attestation/2.0.0/com/yubico/fido/metadata/AAGUID.html
  Some authenticators have an AAGUID, which is a 128-bit identifier that indicates the type (e.g. make and model) of the authenticator.

  NonAttestation seems to require this to be "00000000-0000-0000-0000-000000000000"
   */
	public String aaguid = "00000000-0000-0000-0000-000000000000";

	/*
	 * The Authenticator attestation certificates object, a JSON like:
	 * <pre>{@code
	 *   {
	 *     "alg": "string",
	 *     "x5c": [
	 *       "base64"
	 *     ]
	 *   }
	 * }</pre>
	 */

	/**
	 * The algorithm used for the public credential
	 */
	//@Enumerated(EnumType.STRING)    // See https://vladmihalcea.com/the-best-way-to-map-an-enum-type-with-jpa-and-hibernate/
	@NonNull
	public PublicKeyCredential alg;

	/**
	 * The list of X509 certificates encoded as base64url.
	 */
	//@OneToMany(mappedBy = "webAuthnCredential")
	@NonNull
	public List<WebAuthnCertificate> x5cList = new ArrayList<>();

	@NonNull
	public String fmt;

	//@OneToOne  // owning side
	@NonNull
	public UserEntity user;


	public WebAuthnCredential(Authenticator authenticator, UserEntity user) {
		aaguid = authenticator.getAaguid();
		if(authenticator.getAttestationCertificates() != null)
			alg = authenticator.getAttestationCertificates().getAlg();
		counter = authenticator.getCounter();
		credID = authenticator.getCredID();
		fmt = authenticator.getFmt();
		publicKey = authenticator.getPublicKey();
		type = authenticator.getType();
		userName = authenticator.getUserName();
		if(authenticator.getAttestationCertificates() != null
				&& authenticator.getAttestationCertificates().getX5c() != null) {
			for (String x5c : authenticator.getAttestationCertificates().getX5c()) {
				WebAuthnCertificate cert = new WebAuthnCertificate();
				cert.x5c = x5c;
				cert.webAuthnCredential = this;
				this.x5cList.add(cert);
			}
		}
		this.user = user;
		user.webAuthnCredential = this;
	}

	public static List<WebAuthnCredential> findByUserName(String userName) {
		List<WebAuthnCredential> creds = WebAuthnCredential.list("username", userName);
		for (WebAuthnCredential cred : creds) {
			log.info("   " + cred.userName+ ", " + cred.credID);
		}
		log.info("findByUserName(userName="+userName+") => found " + creds.size() + " WebAuthnCredential(s)");
		return creds;
	}

	public static List<WebAuthnCredential> findByCredID(String credID) {
		return WebAuthnCredential.list("credID", credID);
	}

	public String toString() {
		return "WebAuthnCredential[credId= " + this.credID + ", user=" + this.user + "]";
	}

}