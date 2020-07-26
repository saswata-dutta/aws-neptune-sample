#!/usr/bin/env bash
set -x

wget https://www.amazontrust.com/repository/SFSRootCAG2.pem

openssl x509 -outform der -in SFSRootCAG2.pem -out SFSRootCAG2.der

# Create an empty Java KeyStore
keytool -genkey -keyalg RSA -alias SFSRootCAG2 -keystore SFSRootCAG2.ks
keytool -delete -alias SFSRootCAG2 -keystore SFSRootCAG2.ks

keytool -import -alias SFSRootCAG2 -keystore SFSRootCAG2.ks -file SFSRootCAG2.der
keytool -importkeystore -srckeystore SFSRootCAG2.ks -destkeystore SFSRootCAG2.ks -deststoretype pkcs12
