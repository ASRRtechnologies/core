/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.exceptions

/** A [DuplicateException] that names what collided: `Project with value P-001 for property number already exists`. */
class TypedDuplicateException(type: String?, property: String, value: String) : DuplicateException("$type with value $value for property $property already exists")
