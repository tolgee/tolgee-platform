with import <nixpkgs> {};
ruby.withPackages (ps: with ps; [ rails faraday json sqlite3 scrypt ])

