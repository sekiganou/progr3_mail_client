#!/usr/bin/env bash

nix-shell --command 'mvn clean compile exec:java -Dexec.mainClass="progr3.mail.client.app.Launcher"'
