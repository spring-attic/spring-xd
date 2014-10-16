/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.container.store;

import java.util.List;

import org.springframework.xd.dirt.cluster.Admin;

/**
 * Repository for persisting {@link Admin} entities for admins.
 *
 * @author Janne Valkealahti
 */
public interface AdminRepository {

	/**
	 * Update the admin runtime attributes.
	 *
	 * @param admin the admin runtime entity
	 */
	void update(Admin admin);

	/**
	 * Save the admin runtime attributes.
	 *
	 * @param admin the container runtime entity
	 * @return the admin runtime
	 */
	Admin save(Admin admin);

	/**
	 * Returns all admin runtime entities.
	 *
	 * @return all admin runtime entities
	 */
	List<Admin> findAll();

	/**
	 * Returns whether a admin runtime with the given id exists.
	 *
	 * @param id admin runtime id
	 * @return true if a admin runtime with the given id exists, {@literal false} otherwise
	 */
	boolean exists(String id);

	/**
	 * Retrieves a admin runtime by its id.
	 *
	 * @param id admin runtime id
	 * @return a admin runtime
	 */
	Admin findOne(String id);

}
