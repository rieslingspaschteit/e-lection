package pse.election.backendserver.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * This class corresponds to the relation "Authority" in the database. Authorities are defined by
 * the host of the system in the  file, whereas authorities have the power to create and direct
 * elections.
 *
 * @version 1.0
 */
@Entity
public class Authority {

  /**
   * This is the primary key.
   */
  @Id
  private String email;

  public Authority() {
  }

  /**
   * Constructs an Authority instance.
   *
   * @param email of authority
   */
  public Authority(String email) {
    this.email = email;
  }

  /** Getter for the email of the authority.
   *
   * @return email
   */
  public String getEmail() {
    return this.email;
  }
}
