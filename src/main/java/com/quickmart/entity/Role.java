package com.quickmart.entity;

// Stored as text in users.role (CHECK constraint lists the same three values).
// Spring Security sees them as authorities ROLE_CUSTOMER, ROLE_VENDOR, ROLE_ADMIN.
public enum Role {
    CUSTOMER, VENDOR, ADMIN
}
