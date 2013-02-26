#!/bin/sh

set -e

rm -rf ./tmp/langohr
mkdir -p ./tmp/langohr/keystore/ ./tmp/langohr/empty

export KEYSTORE=`mktemp ./tmp/langohr/keystore/keystore.XXXXX`
export EMPTY_KEYSTORE=`mktemp ./tmp/langohr/empty/empty_keystore.XXXXX`
export PASSWORD="bunnies"

rm -rf $KEYSTORE $EMPTY_KEYSTORE
# touch $KEYSTORE $EMPTY_KEYSTORE

keytool -import -alias "server1" \
    -file ./test/resources/tls/testca/cacert.pem \
    -keystore $KEYSTORE \
    -noprompt \
    -storepass $PASSWORD

keytool -list -keystore $KEYSTORE -storepass $PASSWORD

# we can't create an empty keystore so we add and
# delete a certificate
keytool -import -alias "server1" \
    -file ./test/resources/tls/testca/cacert.pem \
    -keystore $EMPTY_KEYSTORE \
    -noprompt \
    -storepass $PASSWORD

keytool -delete -alias "server1" -keystore $EMPTY_KEYSTORE -storepass $PASSWORD

keytool -list -keystore $EMPTY_KEYSTORE -storepass $PASSWORD
