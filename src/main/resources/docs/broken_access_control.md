# Broken Access Control

Broken access control happens when an application fails to enforce what an
authenticated user is allowed to do. It is consistently one of the most common
and most damaging web application weaknesses.

## Common forms

Insecure direct object references occur when an endpoint trusts an identifier
supplied by the client, such as /api/orders/1043, without checking that the
current user actually owns object 1043. An attacker simply increments the
identifier to read or modify other users' data.

Missing function-level authorization happens when the user interface hides an
administrative action but the underlying endpoint does not re-check the caller's
role. Hiding a button is not access control; the server must verify permission
on every request.

Privilege escalation occurs when a normal user can perform actions reserved for
higher-privileged roles, for example by tampering with a role claim in a token
or a hidden form field.

## Defenses

Enforce authorization on the server for every request, never on the client.
Deny by default: a request should be rejected unless an explicit rule permits it.
Check ownership of the specific record being accessed, not just that the user is
logged in. Prefer opaque or unguessable identifiers over sequential integers to
reduce enumeration. Centralize authorization logic so the same checks are applied
consistently rather than re-implemented per endpoint. Log access-control failures
and alert on repeated denials, which often indicate an attacker probing for gaps.
