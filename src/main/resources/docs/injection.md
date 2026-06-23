# Injection

Injection flaws occur when untrusted input is interpreted as part of a command
or query. The classic example is SQL injection, but the same root cause produces
operating-system command injection, LDAP injection, and others.

## SQL injection

SQL injection arises when user input is concatenated directly into a SQL
statement. If a login form builds a query by gluing the username into the SQL
string, an attacker can supply input that changes the meaning of the query,
bypassing authentication or dumping the entire database.

The primary defense is parameterized queries, also called prepared statements.
With parameters, the query structure is fixed in advance and user input is bound
as data, so it can never be parsed as SQL. Object-relational mappers generally
parameterize by default, but raw query escape hatches reintroduce the risk.

## Other guidance

Validate and constrain input against an allowlist of expected formats; reject
anything that does not match rather than trying to strip dangerous characters.
Apply least privilege to the database account the application uses, so a
successful injection has limited blast radius. For operating-system commands,
avoid invoking a shell with concatenated input; pass arguments as an explicit
list to the process API instead. Escaping should be a last resort and must be
context-aware, because the correct escaping for HTML differs from SQL, which
differs again from a shell.
