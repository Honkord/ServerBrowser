/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permission checks, role management, and request authorization.
 */
public final class access_control {
	public enum role {
		GUEST,
		USER,
		ADMIN
	}

	public enum permission {
		PROXY_REQUEST,
		MANAGE_USERS,
		VIEW_AUDIT_LOG
	}

	private static final Map<role, Set<permission>> ROLE_PERMISSIONS = Map.of(
			role.GUEST, EnumSet.noneOf(permission.class),
			role.USER, EnumSet.of(permission.PROXY_REQUEST),
			role.ADMIN, EnumSet.of(
					permission.PROXY_REQUEST,
					permission.MANAGE_USERS,
					permission.VIEW_AUDIT_LOG));

	private final Map<String, role> roles_by_user_id = new ConcurrentHashMap<>();

	public void assign_role(String user_id, role user_role) {
		if (user_id == null || user_id.isBlank()) {
			throw new IllegalArgumentException("user_id is required");
		}
		if (user_role == null) {
			throw new IllegalArgumentException("role is required");
		}
		roles_by_user_id.put(user_id, user_role);
	}

	public role get_role(String user_id) {
		return roles_by_user_id.getOrDefault(user_id, role.GUEST);
	}

	public boolean check_permission(String user_id, permission required) {
		if (required == null) {
			return false;
		}
		Set<permission> granted = ROLE_PERMISSIONS.getOrDefault(get_role(user_id), Collections.emptySet());
		return granted.contains(required);
	}

	public boolean authorize_request(String user_id, permission required) {
		return check_permission(user_id, required);
	}
}
