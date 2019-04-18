#!/bin/sh

set -e

rm -rf ./tmp/langohr
mkdir -p ./tmp/langohr/keystore/ ./tmp/langohr/empty

KEYSTORE="./tmp/langohr/keystore/keystore"
EMPTY_KEYSTORE="./tmp/langohr/empty/empty_keystore"
PASSWORD="bunnies"

CERTIFICATE_DIR=${LANGOHR_CERTIFICATE_DIR:-"./test/resources/tls"}

echo "Will use certificates from ${CERTIFICATE_DIR}..."

rm -rf $KEYSTORE $EMPTY_KEYSTORE
# touch $KEYSTORE $EMPTY_KEYSTORE

keytool -import -alias "server1" \
    -file "$CERTIFICATE_DIR/ca_certificate.pem" \
    -keystore $KEYSTORE \
    -noprompt \
    -storepass $PASSWORD

keytool -list -keystore $KEYSTORE -storepass $PASSWORD

# we can't create an empty keystore so we add and
# delete a certificate
keytool -import -alias "server1" \
    -file "$CERTIFICATE_DIR/ca_certificate.pem" \
    -keystore $EMPTY_KEYSTORE \
    -noprompt \
    -storepass $PASSWORD

keytool -delete -alias "server1" -keystore $EMPTY_KEYSTORE -storepass $PASSWORD

keytool -list -keystore $EMPTY_KEYSTORE -storepass $PASSWORD
