package org.example.webauthn;

import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.AttestationCertificates;
import io.vertx.ext.auth.webauthn.Authenticator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * In this class we expose our {@link UserEntity} to the quarkus-webAuthn Library
 */
@Slf4j
@ApplicationScoped
@Blocking  // Defer calls to worker pool and not the IO thread
public class WebAuthnSetup implements WebAuthnUserProvider {

	// Adapted from https://github.com/FroMage/quarkus-renarde-todo/blob/main/src/main/java/util/MyWebAuthnSetup.java

	@Transactional
	@Override
	public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userName) {
		log.info("findWebAuthnCredentialsByUserName: "+userName);
		return Uni.createFrom().item(toAuthenticators(WebAuthnCredential.findByUserName(userName)));
	}

	@Transactional
	@Override
	public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credID) {
		log.info("findWebAuthnCredentialsByCredID: "+credID);
		return Uni.createFrom().item(toAuthenticators(WebAuthnCredential.findByCredID(credID)));

	}

	@Transactional
	@Override
	public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
		log.info("updateOrStoreWebAuthnCredentials" + authenticator);

		// We assume that a user must already exist before WebAuthnCredentials can be added and stored
		UserEntity user = UserEntity.findByEmail(authenticator.getUserName())
				.orElseThrow(() -> new RuntimeException("Cannot updateOrStoreWebAuthnCredentials. No user with email: " + authenticator.getUserName()));

		// IF this user has no credential yet, THEN Create one
		if (user.getWebAuthnCredential() == null) {
			log.info("Create new webAuthnCredentials for "+user.email);
			WebAuthnCredential credential = new WebAuthnCredential(authenticator, user);
			credential.persist();
			user.webAuthnCredential.counter = authenticator.getCounter();
			user.persist();
		} else {
			// ELSE update the counter of the webAuthnCredential
			log.info("Updating webAuthnCredentials for " + user + "from counter=" + user.webAuthnCredential.counter + " to counter=" + authenticator.getCounter());
			user.webAuthnCredential.counter = authenticator.getCounter();
		}

		log.info("========== updateOrStoreWebAuthnCredentials: " + user.toString());

		return Uni.createFrom().nullItem();
	}

	/** map a list of our WebauthnCredentials to a list of Vertx authenticators */
	private static List<Authenticator> toAuthenticators(List<WebAuthnCredential> creds) {
		return creds.stream().map(WebAuthnSetup::toAuthenticator).toList();
	}

	private static Authenticator toAuthenticator(WebAuthnCredential credential) {
		Authenticator ret = new Authenticator();
		ret.setAaguid(credential.aaguid);
		AttestationCertificates attestationCertificates = new AttestationCertificates();
		attestationCertificates.setAlg(credential.alg);
		List<String> x5cs = new ArrayList<>(credential.x5cList.size());
		for (WebAuthnCertificate webAuthnCertificate : credential.x5cList) {
			x5cs.add(webAuthnCertificate.x5c);
		}
		attestationCertificates.setX5c(x5cs);
		ret.setAttestationCertificates(attestationCertificates);
		ret.setCounter(credential.counter);
		ret.setCredID(credential.credID);
		ret.setFmt(credential.fmt);
		ret.setPublicKey(credential.publicKey);
		ret.setType(credential.type);
		ret.setUserName(credential.userName);
		return ret;
	}

	//TODO: If your implementation of UserEntities contains roles, then you can add them here.
	@Override
	public Set<String> getRoles(String userName) {
		return WebAuthnUserProvider.super.getRoles(userName);
	}
}