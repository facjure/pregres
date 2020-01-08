# Pregres

An Clojure interface to Postgres database development.

## Rationale 

Customers and developers have substantial investments in, and are comfortable
with the performance, security and stability of, widely used opensource
relational databases like Postgres. While Clojure developers may envy the
relative ease, flexibility and productivity of immutable databases like Datomic,
they have concerns about running them on customer-approved rdbms, accessing
existing data (including bigdata), and decades of research on fine-tuning
SQL. In addition, they face ongoing problems integrating with analytics and data
science teams.

Pregres is an effort to build a pragmatic library suitable in those areas where
a comprehensive Clojure api is suitable. It reflects the reality that, for
security, ACID compliance, and sharding scalability, sql workflows must
simply shine with idiomatic Clojure.

## Features

- Built on Clojure [JDBC](https://github.com/clojure/java.jdbc)
- Built-in data type conversions, including json
- Industry's best [Connection Pool](https://github.com/brettwooldridge/HikariCP)
- First-class [Migrations](https://github.com/weavejester/ragtime)
- Plain old SQL using [HugSQL](https://www.hugsql.org/) 
- Integration with popular Postgres extensions (postgis)

## Credits

Some functions are copied from the following libraries. I remain thankful to them.
 
- [audit triggers](https://github.com/2ndQuadrant/audit-trigger)
- [clojure.jdbc](https://github.com/funcool/clojure.jdbc)
- [clj-postgresql](https://github.com/remodoy/clj-postgresql)
- [pg-migrator](https://github.com/aphel-bilisim-hizmetleri/pg-migrator)
- [sparkx-kits](https://github.com/staples-sparx/kits)

## Status

Beta, unstable

## License

Copyright © 2015-2020, Facjure, LLC. Distributed under the Eclipse Public
License either version 1.0 or (at your option) any later version.
