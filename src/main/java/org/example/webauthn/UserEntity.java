package org.example.webauthn;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dummy mock implementation of users that would normally be stored in a DB.
 *
 * Here we just simply use an in memory ArrayList
 */
@Slf4j
@Data
public class UserEntity {

	/**
	 * Users by their email
	 */
	public static Map<String, UserEntity> users = new HashMap<>();

	@NonNull
	String email;

	@NonNull
	String fullName;

	/**
	 * The
	 */
	//@OneToOne     // This would be a 1:1 relationship in quarkus-panache / hibernate
	//@Ignore       // SECURITY IMPORTANT: ignore in GraphQL and JSON
	//@JsonIgnore
	public WebAuthnCredential webAuthnCredential;

	/**
	 * Find a user by his email address
	 *
	 * @param email user's email
	 * @return the UserEntity or Optional.empty() if there is no user with that email
	 */
	public static Optional<UserEntity> findByEmail(String email) {
		return Optional.of(users.get(email));
	}

	/**
	 * mock implementation of persist.
	 */
	public void persist() {
		log.info("Persisting " + this.toString());
		users.put(this.email, this);
	}

	public String toString() {
		return "UserEntity[email=" + email + ", fullName=" + fullName + "]";
	}
}