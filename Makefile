# This file is part of Serendipity, an UCI chess engine written in Java.
#
# Copyright (C) 2024-2025  Shawn Xu <shawn@shawnxu.org>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

# Modified for SerenSkibidi, 2026-11-02.
EXE ?= SerenSkibidi
MAVEN_EXE ?= mvn
MAVEN_ARGS ?=
PYTHON ?= python3
JAR := Serendipity/target/serenskibidi-1.0.jar

.PHONY: all net test
all: net
	$(MAVEN_EXE) $(MAVEN_ARGS) -f Serendipity/pom.xml package
	cat header.sh $(JAR) > $(EXE)
	chmod +x $(EXE)
	cp $(JAR) SerenSkibidi.jar

net:
ifdef EVALFILE
	cp "$(EVALFILE)" Serendipity/src/main/resources/embedded.nnue
else
	$(PYTHON) scripts/fetch-network.py
endif

test: all
	$(PYTHON) scripts/smoke-test.py
