.PHONY: build up down logs test e2e clean

build:
\tmvn -q -DskipTests=false clean verify

up:
\tdocker compose up --build -d

down:
\tdocker compose down -v

logs:
\tdocker compose logs --tail=200

test:
\tmvn -q -DskipTests=false test

e2e:
\tmvn -q -pl e2e-tests -am -DskipTests=false test

clean:
\trm -rf **/target
