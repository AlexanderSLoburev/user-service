package com.example.userservice.dao;

import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import java.util.List;
import java.util.Optional;

/**
 * DAO contract for the `users` table (Task 2.1).
 *
 * General contract:
 * <ul>
 *   <li>Each method is atomic: an operation is either applied in its entirety
 * or leaves no trace (transactionality is handled by the implementation; see
 * Task 2.3);</li>
 *   <li>storage technology is not exposed externally: no Session
 * or Hibernate exceptions - only application exceptions (implementation -
 * Task 2.4);</li>
 *   <li>Data format validation (email, age) is the responsibility
 * of the console UI (Task 3.2). The DAO guarantees only database-level
 * invariants (email uniqueness).</li>
 * </ul>
 */

public interface UserDao {

  /**
   * Saves a NEW user. The id field must be null - it is assigned by the
   * database. createdAt is set automatically.
   *
   * @param user the user to save
   * @return the saved user with an assigned id
   * @throws IllegalArgumentException if user == null or id is already assigned
   * @throws DuplicateEmailException  if the email is already taken
   * @throws DataAccessException      in case of infrastructure failures
   */

  User save(User user);

  /**
   * Searches for a user by id.
   *
   * @throws IllegalArgumentException if id == null
   */
  Optional<User> findById(Long id);

  /**
   * Searches for a user by email address. For UI prompts and to check if the
   * email address is in use.
   */
  Optional<User> findByEmail(String email);

  /**
   * All users, sorted by id in ascending order.
   * The result is never null (if the table is empty, the list is empty).
   */
  List<User> findAll();

  /**
   * Updates an existing user by its id. The name, email, and age fields are
   * updated; createdAt remains unchanged. Standard workflow: findById -> edit
   * -> update.
   *
   * @param user a user with a valid id
   * @return the updated instance
   * @throws IllegalArgumentException if user == null or id == null
   * @throws UserNotFoundException    if no user with that id exists
   * @throws DuplicateEmailException  if the new email is already taken by
   *     another user
   */
  User update(User user);

  /**
   * Deletes a user by id. Deletion is idempotent: deleting a user who
   * no longer exists does not result in an error.
   *
   * @return true — the user existed and was deleted; false — the user did not
   *     exist
   * @throws IllegalArgumentException if id == null
   */
  boolean deleteById(Long id);
}